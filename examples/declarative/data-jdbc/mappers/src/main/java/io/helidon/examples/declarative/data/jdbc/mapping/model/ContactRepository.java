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
import io.helidon.data.jdbc.Jdbc;

/**
 * Explicit SQL repository comparing the mapping and reduction paths provided by Helidon Data JDBC.
 * <p>
 * Scalar and record results need no annotation because their mapping shape is known at compile time.
 * {@link Jdbc.RowMapper} can select either an exact mapper service or a mapper service by generic result type. Without
 * the annotation, supported records retain generated mapping as a fallback. {@link Jdbc.IdentityReducer} selects a
 * generated reducer for a record graph. {@link Jdbc.RowReducer} selects an application-owned result-set
 * reducer when identity or construction rules are outside the generated graph contract. Every generated method calls
 * the same public {@code JdbcClient} API available to imperative code.
 */
@Data.Repository
@Data.Provider("jdbc")
@Data.PersistenceUnit("contacts")
public interface ContactRepository {

    /**
     * Lists contact records using labels that match the record component names.
     *
     * - implicit record mapping with terminal method list()
     * - SQL labels id and name match the Contact record
     * - generated code will call the canonical constructor
     * - each row becomes one Contact
     *
     * @return contacts ordered by identifier
     */
    @Jdbc.Statement("""
            SELECT ID AS id, NAME AS name
            FROM CONTACT
            ORDER BY ID
            """)
    List<Contact> listContacts();

    /**
     * Finds one contact using generated optional-record mapping.
     *
     * Result shape is similar to listContacts, but with optional()
     * - allows zero or one row
     *
     * @param id contact identifier
     * @return matching contact, or empty when no row exists
     */
    @Jdbc.Statement("""
            SELECT ID AS id, NAME AS name
            FROM CONTACT
            WHERE ID = :id
            """)
    Optional<Contact> findContact(long id);

    /**
     * Maps SQL labels to a differently shaped record using an explicitly selected row-mapper service.
     *
     * - Explicit @Jdbc.RowMapper selects the exact mapper service type with terminal operation one()
     * - Service Registry supplies the mapper to the generated repository
     * - ContactNameMapper maps SQL id to contactNumber and formats SQL name as displayName
     * - Explicit selection is unambiguous even though another mapper service also produces ContactName
     * - Missing row causes cardinality failure because the method expects one ContactName
     * - Does not reduce multiple rows
     *
     * @param id contact identifier
     * @return matching contact-name projection
     */
    @Jdbc.Statement("SELECT ID AS id, NAME AS name FROM CONTACT WHERE ID = :id")
    @Jdbc.RowMapper(ContactNameMapper.class)
    ContactName mappedContact(long id);

    /**
     * Maps an aggregate SQL row with a second explicitly selected mapper for {@link ContactName}.
     * <p>
     * Both {@link ContactNameMapper} and {@link ContactSummaryMapper} implement
     * {@code JdbcClient.RowMapper<ContactName>}. The class-valued {@link Jdbc.RowMapper} annotation tells the generated
     * repository which service to inject for this method. This mapper consumes the additional aggregate columns and
     * creates a summary display name, so choosing a mapper only from the return type would be ambiguous.
     *
     * @param id contact identifier
     * @return matching contact with phone and tag counts in its display name
     */
    @Jdbc.Statement("""
            SELECT c.ID AS id,
                   c.NAME AS name,
                   COUNT(DISTINCT p.ID) AS phoneCount,
                   COUNT(DISTINCT t.ID) AS tagCount
            FROM CONTACT c
            LEFT JOIN PHONE p ON p.CONTACT_ID = c.ID
            LEFT JOIN TAG t ON t.PHONE_ID = p.ID
            WHERE c.ID = :id
            GROUP BY c.ID, c.NAME
            """)
    @Jdbc.RowMapper(ContactSummaryMapper.class)
    ContactName mappedContactSummary(long id);

    /**
     * Finds the first phone for a contact using a row mapper selected by its generic service contract.
     * <p>
     * The marker form {@code @Jdbc.RowMapper()} does not name an implementation class. It requires the Service Registry
     * to supply {@code JdbcClient.RowMapper<ContactPhone>}. {@link ContactPhoneMapper} satisfies that contract without
     * being referenced by the repository. It combines the selected phone type and number into an application-specific
     * label, showing why an application mapper can be useful when the result does not match SQL columns one-for-one.
     *
     * @param contactId contact identifier
     * @return first phone for the matching contact
     */
    @Jdbc.Statement("""
            SELECT c.ID AS contactId,
                   c.NAME AS contactName,
                   p.TYPE AS phoneType,
                   p.PHONE AS phoneNumber
            FROM CONTACT c
            JOIN PHONE p ON p.CONTACT_ID = c.ID
            WHERE c.ID = :contactId
              AND p.ID = (SELECT MIN(first_phone.ID)
                          FROM PHONE first_phone
                          WHERE first_phone.CONTACT_ID = c.ID)
            """)
    @Jdbc.RowMapper
    ContactPhone mappedPrimaryPhone(long contactId);

    /**
     * Lists one selected scalar column.
     *
     * @return contact names ordered by identifier
     */
    @Jdbc.Statement("SELECT NAME FROM CONTACT ORDER BY ID")
    List<String> listNames();

    /**
     * Lists detached flat rows from a three-table join.
     *
     * Phone and tag identifiers are boxed because a left join can produce {@code NULL} child columns.
     *
     * - join produces one physical row for each contact/phone/tag combination
     * - no deduplication or graph construction
     *
     * @return joined contact detail rows
     */
    @Jdbc.Statement("""
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
     * - GROUP BY produces one row per ContactCard
     * - No reducer required here
     *
     * @return contact summary cards
     */
    @Jdbc.Statement("""
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
     * - Generated graph reduction from explicit Java identity paths
     * - join returns repeated contact/phone/tag data
     * - root and collection scopes declare identity paths
     * - generated reducer deduplicates objects within their parent and skips null outer-join children
     * - canonical record constructors create the record graph after all rows are consumed
     *
     * @return contacts with deduplicated phones and tags
     */
    @Jdbc.Statement("""
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
    @Jdbc.IdentityReducer(identityPaths = {"id", "phones.id", "phones.tags.id"})
    List<ContactGraph> listGraphs();

    /**
     * Reduces the contact join into a custom record graph using application-defined composite phone identity.
     * <p>
     * The application reducer identifies a phone by its type and number, builds mutable state only while consuming the
     * result set, and returns records from {@code finish()}.
     *
     * @return contacts with ordered, deduplicated phones and tags
     */
    @Jdbc.Statement("""
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
    @Jdbc.RowReducer(CustomContactGraphReducer.class)
    List<CustomContactGraph> listCustomGraphs();

    /**
     * Applies a small application reducer to a query that intentionally repeats each contact row.
     * <p>
     * Unlike {@link #listCustomGraphs()}, this method demonstrates only custom root deduplication. It keeps the
     * example of the smallest useful {@link Jdbc.RowReducer} beside the complete custom graph reducer.
     *
     * - custom deduplication
     * - useful when SQL returns repeated logical roots
     *
     * @return deduplicated contacts in first-seen order
     */
    @Jdbc.Statement("""
            SELECT ID AS id, NAME AS name FROM CONTACT
            UNION ALL
            SELECT ID AS id, NAME AS name FROM CONTACT
            ORDER BY id
            """)
    @Jdbc.RowReducer(ContactRowReducer.class)
    List<Contact> listWithCustomReducer();
}
