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

import io.helidon.data.Data;

/**
 * Explicit SQL repository showing generated mappers and relationship reducers.
 */
@Data.Repository
public interface ContactRepository extends Data.GenericRepository<Contact, Long> {

    /**
     * Lists contacts using dotted SQL labels. The JDBC generator infers the reducer from paths such as
     * {@code phones.id} and {@code phones.tags.id}.
     *
     * @return contact aggregates
     */
    @Data.Query("""
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
            """)
    List<Contact> listWithAutomaticReducer();

    /**
     * Lists contacts using non-path SQL aliases and an explicit reducer mapping contract.
     *
     * @return contact aggregates
     */
    @Data.Query("""
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
            """)
    @Data.ReduceWith(ContactGraphMapping.class)
    List<Contact> listWithExplicitReducer();

    /**
     * Lists summary cards using a declarative mapper contract.
     *
     * @return contact summary cards
     */
    @Data.Query("""
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
            """)
    @Data.MapWith(ContactCardMapping.class)
    List<ContactCard> listCards();

}
