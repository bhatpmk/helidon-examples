/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.examples.declarative.data.jdbc.mapping.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.helidon.data.DataException;
import io.helidon.data.jdbc.JdbcClient;

/**
 * Reduces contact, phone, and tag join rows into an custom record graph.
 * <p>
 * The reducer demonstrates the application-controlled alternative to generated graph reduction. It defines a
 * composite phone identity from the phone type and number, retains first-seen SQL order, suppresses duplicate rows,
 * checks repeated scalar values, and interprets a fully-null child projection as an outer-join absence. The provider
 * creates a fresh reducer for each repository invocation and owns the JDBC result set, statement, and connection.
 *
 * <p>The reducer copies values from each callback-scoped {@link JdbcClient.Row} into its own state. It never retains
 * the row or receives a JDBC resource.</p>
 */
public final class CustomContactGraphReducer
        implements JdbcClient.RowReducer<List<CustomContactGraph>> {

    private final Map<Long, ContactState> contacts = new LinkedHashMap<>();

    /**
     * Creates reducer state for one repository invocation.
     */
    public CustomContactGraphReducer() {
    }

    /**
     * Incorporates one physical join row into the logical graph.
     *
     * @param row callback-scoped value view for the current physical row
     * @throws DataException if an outer-join hierarchy or repeated scalar value is inconsistent
     */
    @Override
    public void accept(JdbcClient.Row row) {
        Long contactId = row.required("contactId", Long.class);
        String contactName = row.required("contactName", String.class);
        ContactState contact = contacts.get(contactId);
        if (contact == null) {
            contact = new ContactState(contactId, contactName);
            contacts.put(contactId, contact);
        } else if (!Objects.equals(contact.name, contactName)) {
            throw new DataException("Conflicting projected contact name for one contact identity");
        }

        Long phoneId = row.optional("phoneId", Long.class).orElse(null);
        String phoneType = row.optional("phoneType", String.class).orElse(null);
        String phoneNumber = row.optional("phoneNumber", String.class).orElse(null);
        Long tagId = row.optional("tagId", Long.class).orElse(null);
        String tagName = row.optional("tagName", String.class).orElse(null);

        boolean phoneAbsent = phoneId == null && phoneType == null && phoneNumber == null;
        if (phoneAbsent) {
            if (tagId != null || tagName != null) {
                throw new DataException("Projected tag exists beneath an absent phone");
            }
            return;
        }
        if (phoneId == null || phoneType == null || phoneNumber == null) {
            throw new DataException("Projected phone has an incomplete composite identity");
        }

        PhoneIdentity phoneIdentity = new PhoneIdentity(phoneType, phoneNumber);
        PhoneState phone = contact.phones.get(phoneIdentity);
        if (phone == null) {
            phone = new PhoneState(phoneId, phoneType, phoneNumber);
            contact.phones.put(phoneIdentity, phone);
        } else if (!Objects.equals(phone.databaseId, phoneId)) {
            throw new DataException("Conflicting projected phone identifier for one composite identity");
        }

        if (tagId == null) {
            if (tagName != null) {
                throw new DataException("Projected tag name exists without a tag identity");
            }
            return;
        }
        if (tagName == null) {
            throw new DataException("Projected tag identity exists without a tag name");
        }

        CustomTagGraph existing = phone.tags.putIfAbsent(tagId, new CustomTagGraph(tagId, tagName));
        if (existing != null && !Objects.equals(existing.name(), tagName)) {
            throw new DataException("Conflicting projected tag name for one tag identity");
        }
    }

    /**
     * Creates the custom record result after the provider exhausts the result set.
     *
     * @return custom record roots, phones, and tags in first-seen SQL order
     */
    @Override
    public List<CustomContactGraph> finish() {
        List<CustomContactGraph> result = new ArrayList<>(contacts.size());
        for (ContactState contact : contacts.values()) {
            List<CustomPhoneGraph> phones = new ArrayList<>(contact.phones.size());
            for (PhoneState phone : contact.phones.values()) {
                phones.add(new CustomPhoneGraph(phone.databaseId,
                                                   phone.type,
                                                   phone.number,
                                                   List.copyOf(phone.tags.values())));
            }
            result.add(new CustomContactGraph(contact.id, contact.name, List.copyOf(phones)));
        }
        return List.copyOf(result);
    }

    private record PhoneIdentity(String type, String number) {
    }

    private static final class ContactState {
        private final Long id;
        private final String name;
        private final Map<PhoneIdentity, PhoneState> phones = new LinkedHashMap<>();

        private ContactState(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static final class PhoneState {
        private final Long databaseId;
        private final String type;
        private final String number;
        private final Map<Long, CustomTagGraph> tags = new LinkedHashMap<>();

        private PhoneState(Long databaseId, String type, String number) {
            this.databaseId = databaseId;
            this.type = type;
            this.number = number;
        }
    }
}

