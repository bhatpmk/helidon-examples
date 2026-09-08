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

import java.util.List;
import java.util.Optional;

import io.helidon.data.Data;

/**
 * Explicit SQL repository comparing the mapping and reduction paths provided by Helidon Data JDBC.
 * <p>
 * Scalar and record results need no annotation because their mapping shape is known at compile time.
 * {@link Data.RowMapper} selects application code for one physical row. Repeated identity-bearing
 * {@link Data.BeanMapping} declarations select a generated reducer for a mutable object graph. {@link Data.RowReducer}
 * selects an application-owned result-set reducer when identity or construction rules are outside the generated
 * graph contract. Every generated method calls the same public {@code JdbcClient} API available to imperative code.
 */
@Data.Repository
@Data.Provider("jdbc")
@Data.PersistenceUnit("contacts")
public interface ContactRepository {

    /**
     * Lists contact records using labels that match the record component names.
     *
     * @return contacts ordered by identifier
     */
    @Data.Query("""
            SELECT ID AS id, NAME AS name
            FROM CONTACT
            ORDER BY ID
            """)
    List<Contact> listContacts();

    /**
     * Finds one contact using generated optional-record mapping.
     *
     * @param id contact identifier
     * @return matching contact, or empty when no row exists
     */
    @Data.Query("""
            SELECT ID AS id, NAME AS name
            FROM CONTACT
            WHERE ID = :id
            """)
    Optional<Contact> findContact(long id);

    /**
     * Finds one contact using an explicitly selected row mapper.
     *
     * @param id contact identifier
     * @return matching contact, or {@code null} when no row exists
     */
    @Data.Query("SELECT ID AS id, NAME AS name FROM CONTACT WHERE ID = :id")
    @Data.RowMapper(ContactNameMapper.class)
    Contact mappedContact(long id);

    /**
     * Lists one selected scalar column.
     *
     * @return contact names ordered by identifier
     */
    @Data.Query("SELECT NAME FROM CONTACT ORDER BY ID")
    List<String> listNames();

    /**
     * Lists detached flat rows from a three-table join.
     * <p>
     * Phone and tag identifiers are boxed because a left join can produce {@code NULL} child columns.
     *
     * @return joined contact detail rows
     */
    @Data.Query("""
            SELECT c.ID    AS contactId,
                   c.NAME  AS contactName,
                   p.ID    AS phoneId,
                   p.TYPE  AS phoneType,
                   p.PHONE AS phoneNumber,
                   t.ID    AS tagId,
                   t.NAME  AS tagName
            FROM CONTACT c
            LEFT JOIN PHONE p ON p.CONTACT_ID = c.ID
            LEFT JOIN TAG t ON t.PHONE_ID = p.ID
            ORDER BY c.ID, p.ID, t.ID
            """)
    List<ContactDetail> listDetails();

    /**
     * Lists aggregate records whose SQL labels match the record component names.
     *
     *
     * @return contact summary cards
     */
    @Data.Query("""
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
            """)
    List<ContactCard> listCards();

    /**
     * Reduces a contact, phone, and tag join into identity-defined object graphs.
     *
     *
     * @return contacts with deduplicated phones and tags
     */
    @Data.Query("""
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
            """)
    @Data.BeanMapping(value = ContactGraph.class, identityProperty = "id")
    @Data.BeanMapping(value = PhoneGraph.class, propertyPath = "phones", identityProperty = "id")
    @Data.BeanMapping(value = TagGraph.class, propertyPath = "phones.tags", identityProperty = "id")
    List<ContactGraph> listGraphs();

    /**
     * Reduces the contact join into an immutable graph using application-defined composite phone identity.
     * <p>
     * The application reducer identifies a phone by its type and number, builds mutable state only while consuming the
     * result set, and returns immutable records from {@code finish()}.
     *
     * @return immutable contacts with ordered, deduplicated phones and tags
     */
    @Data.Query("""
            SELECT c.ID    AS contactId,
                   c.NAME  AS contactName,
                   p.ID    AS phoneId,
                   p.TYPE  AS phoneType,
                   p.PHONE AS phoneNumber,
                   t.ID    AS tagId,
                   t.NAME  AS tagName
            FROM CONTACT c
            LEFT JOIN PHONE p ON p.CONTACT_ID = c.ID
            LEFT JOIN TAG t ON t.PHONE_ID = p.ID
            ORDER BY c.ID, p.ID, t.ID
            """)
    @Data.RowReducer(ImmutableContactGraphReducer.class)
    List<ImmutableContactGraph> listImmutableGraphs();

    /**
     * Applies a small application reducer to a query that intentionally repeats each contact row.
     * <p>
     * Unlike {@link #listImmutableGraphs()}, this method demonstrates only custom root deduplication. It keeps the
     * example of the smallest useful {@link Data.RowReducer} beside the complete immutable graph reducer.
     *
     * @return deduplicated contacts in first-seen order
     */
    @Data.Query("""
            SELECT ID AS id, NAME AS name FROM CONTACT
            UNION ALL
            SELECT ID AS id, NAME AS name FROM CONTACT
            ORDER BY id
            """)
    @Data.RowReducer(ContactRowReducer.class)
    List<Contact> listWithCustomReducer();
}
