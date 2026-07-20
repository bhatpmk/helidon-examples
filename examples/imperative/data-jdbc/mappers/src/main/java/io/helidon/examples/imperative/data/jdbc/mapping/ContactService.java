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
package io.helidon.examples.imperative.data.jdbc.mapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import io.helidon.data.DataException;
import io.helidon.data.jdbc.JdbcClient;
import io.helidon.examples.imperative.data.jdbc.mapping.model.Contact;
import io.helidon.examples.imperative.data.jdbc.mapping.model.ContactCard;
import io.helidon.examples.imperative.data.jdbc.mapping.model.ContactDetail;
import io.helidon.examples.imperative.data.jdbc.mapping.model.ContactGraph;
import io.helidon.examples.imperative.data.jdbc.mapping.model.ContactNameMapper;
import io.helidon.examples.imperative.data.jdbc.mapping.model.Phone;
import io.helidon.examples.imperative.data.jdbc.mapping.model.Tag;

/**
 * Contact data access implemented with the current imperative {@link JdbcClient} API.
 * <p>
 * Each method mirrors one declarative mapper example. Record and scalar methods use direct row-mapper lambdas, the
 * explicit-contact method uses {@link ContactNameMapper}, and the graph methods pass stateful reducers to
 * {@link JdbcClient.Statement#reduce(JdbcClient.RowReducer)}. The first graph reducer uses one scalar identity per
 * scope, matching generated declarative graph semantics. The second uses immutable records and a composite phone
 * identity, which is intentionally an application-owned reduction rule.
 */
final class ContactService {

    private static final String CONTACT_SELECT = "SELECT ID AS id, NAME AS name FROM CONTACT";

    private static final String DETAILS_SQL = """
            SELECT c.ID AS contactId,
                   c.NAME AS contactName,
                   p.ID AS phoneId,
                   p.TYPE AS phoneType,
                   p.PHONE AS phoneNumber,
                   t.ID AS tagId,
                   t.NAME AS tagName
            FROM CONTACT c
            LEFT JOIN PHONE p ON p.CONTACT_ID = c.ID
            LEFT JOIN TAG t ON t.PHONE_ID = p.ID
            ORDER BY c.ID, p.ID, t.ID
            """;

    private static final String CARDS_SQL = """
            SELECT c.ID AS id,
                   c.NAME AS displayName,
                   MIN(p.PHONE) AS firstPhone,
                   COUNT(DISTINCT p.ID) AS phoneCount,
                   COUNT(DISTINCT t.ID) AS tagCount
            FROM CONTACT c
            LEFT JOIN PHONE p ON p.CONTACT_ID = c.ID
            LEFT JOIN TAG t ON t.PHONE_ID = p.ID
            GROUP BY c.ID, c.NAME
            ORDER BY c.ID
            """;

    private static final String GRAPHS_SQL = """
            SELECT c.ID AS "id",
                   c.NAME AS "name",
                   p.ID AS "phones.id",
                   p.TYPE AS "phones.type",
                   p.PHONE AS "phones.phone",
                   t.ID AS "phones.tags.id",
                   t.NAME AS "phones.tags.name"
            FROM CONTACT c
            LEFT JOIN PHONE p ON p.CONTACT_ID = c.ID
            LEFT JOIN TAG t ON t.PHONE_ID = p.ID
            ORDER BY c.ID, p.ID, t.ID
            """;

    private static final String IMMUTABLE_GRAPHS_SQL = """
            SELECT c.ID AS contactId,
                   c.NAME AS contactName,
                   p.ID AS phoneId,
                   p.TYPE AS phoneType,
                   p.PHONE AS phoneNumber,
                   t.ID AS tagId,
                   t.NAME AS tagName
            FROM CONTACT c
            LEFT JOIN PHONE p ON p.CONTACT_ID = c.ID
            LEFT JOIN TAG t ON t.PHONE_ID = p.ID
            ORDER BY c.ID, p.ID, t.ID
            """;

    private static final JdbcClient.RowMapper<Contact> CONTACT_MAPPER = row -> new Contact(
            row.required("id", Long.class),
            row.required("name", String.class));

    private static final JdbcClient.RowMapper<ContactDetail> DETAIL_MAPPER = row -> new ContactDetail(
            row.required("contactId", Long.class),
            row.required("contactName", String.class),
            row.optional("phoneId", Long.class).orElse(null),
            row.optional("phoneType", String.class).orElse(null),
            row.optional("phoneNumber", String.class).orElse(null),
            row.optional("tagId", Long.class).orElse(null),
            row.optional("tagName", String.class).orElse(null));

    private static final JdbcClient.RowMapper<ContactCard> CARD_MAPPER = row -> new ContactCard(
            row.required("id", Long.class),
            row.required("displayName", String.class),
            row.optional("firstPhone", String.class).orElse(null),
            row.required("phoneCount", Long.class),
            row.required("tagCount", Long.class));

    private final JdbcClient jdbcClient;

    ContactService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    List<Contact> listContacts() {
        return jdbcClient.create(CONTACT_SELECT + " ORDER BY ID")
                .map(CONTACT_MAPPER)
                .list();
    }

    Optional<Contact> findContact(long id) {
        return jdbcClient.create(CONTACT_SELECT + " WHERE ID = ?")
                .bind(1, id)
                .map(CONTACT_MAPPER)
                .optional();
    }

    Contact mappedContact(long id) {
        return jdbcClient.create(CONTACT_SELECT + " WHERE ID = ?")
                .bind(1, id)
                .map(new ContactNameMapper())
                .one();
    }

    List<String> listNames() {
        return jdbcClient.create("SELECT NAME FROM CONTACT ORDER BY ID")
                .map(String.class)
                .list();
    }

    List<ContactDetail> listDetails() {
        return jdbcClient.create(DETAILS_SQL)
                .map(DETAIL_MAPPER)
                .list();
    }

    List<ContactCard> listCards() {
        return jdbcClient.create(CARDS_SQL)
                .map(CARD_MAPPER)
                .list();
    }

    List<ContactGraph> listGraphs() {
        return jdbcClient.create(GRAPHS_SQL)
                .reduce(new ContactGraphReducer(false));
    }

    List<ContactGraph> listImmutableGraphs() {
        return jdbcClient.create(IMMUTABLE_GRAPHS_SQL)
                .reduce(new ContactGraphReducer(true));
    }

    List<Contact> listWithCustomReducer() {
        return jdbcClient.create("""
                               SELECT ID AS id, NAME AS name FROM CONTACT
                               UNION ALL
                               SELECT ID AS id, NAME AS name FROM CONTACT
                               ORDER BY id
                               """)
                .reduce(new DuplicateContactReducer());
    }

    /**
     * Reduces a join with one scalar identity for each object scope.
     * <p>
     * The boolean constructor flag selects whether the phone key is the database identifier or the application-defined
     * composite of type and number. Both modes use the same provider-owned row lifecycle and public reducer contract.
     */
    private static final class ContactGraphReducer implements JdbcClient.RowReducer<List<ContactGraph>> {
        private final boolean compositePhoneIdentity;
        private final Map<Long, ContactState> contacts = new LinkedHashMap<>();

        private ContactGraphReducer(boolean compositePhoneIdentity) {
            this.compositePhoneIdentity = compositePhoneIdentity;
        }

        @Override
        public void accept(JdbcClient.Row row) {
            String contactIdLabel = compositePhoneIdentity ? "contactId" : "id";
            String contactNameLabel = compositePhoneIdentity ? "contactName" : "name";
            String phoneIdLabel = compositePhoneIdentity ? "phoneId" : "phones.id";
            String phoneTypeLabel = compositePhoneIdentity ? "phoneType" : "phones.type";
            String phoneNumberLabel = compositePhoneIdentity ? "phoneNumber" : "phones.phone";
            String tagIdLabel = compositePhoneIdentity ? "tagId" : "phones.tags.id";
            String tagNameLabel = compositePhoneIdentity ? "tagName" : "phones.tags.name";

            Long contactId = row.required(contactIdLabel, Long.class);
            String contactName = row.required(contactNameLabel, String.class);
            ContactState contact = contacts.get(contactId);
            if (contact == null) {
                contact = new ContactState(contactId, contactName);
                contacts.put(contactId, contact);
            } else if (!Objects.equals(contact.name, contactName)) {
                throw new DataException("Conflicting projected contact name for one identity");
            }

            Long phoneId = row.optional(phoneIdLabel, Long.class).orElse(null);
            String phoneType = row.optional(phoneTypeLabel, String.class).orElse(null);
            String phoneNumber = row.optional(phoneNumberLabel, String.class).orElse(null);
            Long tagId = row.optional(tagIdLabel, Long.class).orElse(null);
            String tagName = row.optional(tagNameLabel, String.class).orElse(null);
            if (phoneId == null && phoneType == null && phoneNumber == null) {
                if (tagId != null || tagName != null) {
                    throw new DataException("Projected tag exists beneath an absent phone");
                }
                return;
            }
            if (phoneId == null || phoneType == null || phoneNumber == null) {
                throw new DataException("Projected phone has an incomplete identity");
            }

            PhoneKey key = compositePhoneIdentity
                    ? new PhoneKey(null, phoneType, phoneNumber)
                    : new PhoneKey(phoneId, null, null);
            PhoneState phone = contact.phones.get(key);
            if (phone == null) {
                phone = new PhoneState(phoneId, phoneType, phoneNumber);
                contact.phones.put(key, phone);
            } else if (!Objects.equals(phone.id, phoneId)
                    || !Objects.equals(phone.type, phoneType)
                    || !Objects.equals(phone.number, phoneNumber)) {
                throw new DataException("Conflicting projected phone values for one identity");
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
            Tag existing = phone.tags.get(tagId);
            if (existing == null) {
                phone.tags.put(tagId, new Tag(tagId, tagName));
            } else if (!Objects.equals(existing.name(), tagName)) {
                throw new DataException("Conflicting projected tag name for one identity");
            }
        }

        @Override
        public List<ContactGraph> finish() {
            List<ContactGraph> result = new ArrayList<>(contacts.size());
            for (ContactState contact : contacts.values()) {
                List<Phone> phones = new ArrayList<>(contact.phones.size());
                for (PhoneState phone : contact.phones.values()) {
                    phones.add(new Phone(phone.id, phone.type, phone.number, List.copyOf(phone.tags.values())));
                }
                result.add(new ContactGraph(contact.id, contact.name, List.copyOf(phones)));
            }
            return List.copyOf(result);
        }
    }

    private static final class DuplicateContactReducer implements JdbcClient.RowReducer<List<Contact>> {
        private final Map<Long, Contact> contacts = new LinkedHashMap<>();

        @Override
        public void accept(JdbcClient.Row row) {
            Long id = row.required("id", Long.class);
            contacts.putIfAbsent(id, new Contact(id, row.required("name", String.class)));
        }

        @Override
        public List<Contact> finish() {
            return List.copyOf(contacts.values());
        }
    }

    private record PhoneKey(Long id, String type, String number) {
    }

    private static final class ContactState {
        private final Long id;
        private final String name;
        private final Map<PhoneKey, PhoneState> phones = new LinkedHashMap<>();

        private ContactState(Long id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static final class PhoneState {
        private final Long id;
        private final String type;
        private final String number;
        private final Map<Long, Tag> tags = new LinkedHashMap<>();

        private PhoneState(Long id, String type, String number) {
            this.id = id;
            this.type = type;
            this.number = number;
        }
    }
}
