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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.helidon.data.jdbc.JdbcClient;
import io.helidon.examples.imperative.data.jdbc.mapping.model.Contact;
import io.helidon.examples.imperative.data.jdbc.mapping.model.ContactCard;
import io.helidon.examples.imperative.data.jdbc.mapping.model.Phone;
import io.helidon.examples.imperative.data.jdbc.mapping.model.Tag;

/**
 * Contact data access implemented with the imperative JDBC API.
 */
final class ContactService {

    private static final String DOTTED_LABEL_QUERY = """
            SELECT c.ID    AS "id",
                   c.NAME  AS "name",
                   p.ID    AS "phones.id",
                   p.TYPE  AS "phones.type",
                   p.PHONE AS "phones.phone",
                   t.ID    AS "phones.tags.id",
                   t.NAME  AS "phones.tags.name"
            FROM CONTACT c
            LEFT JOIN PHONE p ON p.CONTACT_ID = c.ID
            LEFT JOIN TAG t   ON t.PHONE_ID = p.ID
            ORDER BY c.ID, p.ID, t.ID
            """;

    private static final String EXPLICIT_LABEL_QUERY = """
            SELECT c.ID    AS contact_key,
                   c.NAME  AS contact_name,
                   p.ID    AS phone_key,
                   p.TYPE  AS phone_kind,
                   p.PHONE AS phone_number,
                   t.ID    AS tag_key,
                   t.NAME  AS tag_name
            FROM CONTACT c
            LEFT JOIN PHONE p ON p.CONTACT_ID = c.ID
            LEFT JOIN TAG t   ON t.PHONE_ID = p.ID
            ORDER BY c.ID, p.ID, t.ID
            """;

    private static final String CARD_QUERY = """
            SELECT c.ID AS contact_id,
                   c.NAME AS contact_display_name,
                   MIN(p.PHONE) AS primary_phone,
                   COUNT(DISTINCT p.ID) AS phone_count,
                   COUNT(DISTINCT t.ID) AS tag_count
            FROM CONTACT c
            LEFT JOIN PHONE p ON p.CONTACT_ID = c.ID
            LEFT JOIN TAG t ON t.PHONE_ID = p.ID
            GROUP BY c.ID, c.NAME
            ORDER BY c.ID
            """;

    private final JdbcClient jdbcClient;

    ContactService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    List<Contact> listWithDottedLabels() {
        return reduce(jdbcClient.query(DOTTED_LABEL_QUERY)
                              .fetchSize(32)
                              .list(ContactService::dottedRow));
    }

    List<Contact> listWithExplicitMapping() {
        return reduce(jdbcClient.query(EXPLICIT_LABEL_QUERY)
                              .fetchSize(32)
                              .list(ContactService::explicitRow));
    }

    List<ContactCard> listCards() {
        return jdbcClient.query(CARD_QUERY)
                .list(row -> new ContactCard(requiredLong(row, "contact_id"),
                                             row.string("contact_display_name"),
                                             row.string("primary_phone"),
                                             requiredLong(row, "phone_count"),
                                             requiredLong(row, "tag_count")));
    }

    private static ContactRow dottedRow(JdbcClient.Row row) {
        return new ContactRow(requiredLong(row, "id"),
                              row.string("name"),
                              nullableLong(row, "phones.id"),
                              row.string("phones.type"),
                              row.string("phones.phone"),
                              nullableLong(row, "phones.tags.id"),
                              row.string("phones.tags.name"));
    }

    private static ContactRow explicitRow(JdbcClient.Row row) {
        return new ContactRow(requiredLong(row, "contact_key"),
                              row.string("contact_name"),
                              nullableLong(row, "phone_key"),
                              row.string("phone_kind"),
                              row.string("phone_number"),
                              nullableLong(row, "tag_key"),
                              row.string("tag_name"));
    }

    private static List<Contact> reduce(List<ContactRow> rows) {
        Map<Long, ContactAccumulator> contacts = new LinkedHashMap<>();
        for (ContactRow row : rows) {
            ContactAccumulator contact = contacts.computeIfAbsent(row.contactId(),
                                                                  id -> new ContactAccumulator(id, row.contactName()));
            if (row.phoneId() != null) {
                PhoneAccumulator phone = contact.phone(row.phoneId(), row.phoneType(), row.phoneNumber());
                if (row.tagId() != null) {
                    phone.addTag(row.tagId(), row.tagName());
                }
            }
        }
        return contacts.values()
                .stream()
                .map(ContactAccumulator::toContact)
                .toList();
    }

    private static Long nullableLong(JdbcClient.Row row, String columnLabel) {
        Object value = row.get(columnLabel);
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }

    private static long requiredLong(JdbcClient.Row row, String columnLabel) {
        return ((Number) row.get(columnLabel)).longValue();
    }

    private record ContactRow(Long contactId,
                              String contactName,
                              Long phoneId,
                              String phoneType,
                              String phoneNumber,
                              Long tagId,
                              String tagName) {
    }

    private static final class ContactAccumulator {
        private final Long id;
        private final String name;
        private final Map<Long, PhoneAccumulator> phones = new LinkedHashMap<>();

        private ContactAccumulator(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        private PhoneAccumulator phone(Long id, String type, String phone) {
            return phones.computeIfAbsent(id, ignored -> new PhoneAccumulator(id, type, phone));
        }

        private Contact toContact() {
            return new Contact(id,
                               name,
                               phones.values()
                                       .stream()
                                       .map(PhoneAccumulator::toPhone)
                                       .toList());
        }
    }

    private static final class PhoneAccumulator {
        private final Long id;
        private final String type;
        private final String phone;
        private final Map<Long, Tag> tags = new LinkedHashMap<>();

        private PhoneAccumulator(Long id, String type, String phone) {
            this.id = id;
            this.type = type;
            this.phone = phone;
        }

        private void addTag(Long id, String name) {
            tags.putIfAbsent(id, new Tag(id, name));
        }

        private Phone toPhone() {
            return new Phone(id, type, phone, List.copyOf(tags.values()));
        }
    }
}
