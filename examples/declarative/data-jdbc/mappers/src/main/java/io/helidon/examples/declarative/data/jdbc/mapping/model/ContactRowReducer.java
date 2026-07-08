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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.helidon.data.jdbc.JdbcClient;

/**
 * Application reducer used when the SQL deliberately contains duplicate contact rows.
 *
 * <p>The reducer demonstrates that an explicit reducer can define identity and duplicate handling without exposing a
 * JDBC {@code ResultSet} to application code. It is the minimal root-only counterpart to
 * {@link ImmutableContactGraphReducer}, which demonstrates nested immutable graph construction and composite child
 * identity.</p>
 */
public final class ContactRowReducer implements JdbcClient.RowReducer<List<Contact>> {
    private final Map<Long, Contact> contacts = new LinkedHashMap<>();

    /**
     * Creates a reducer for one repository invocation.
     */
    public ContactRowReducer() {
    }

    /**
     * Adds one physical row, retaining the first row for each contact identifier.
     *
     * @param row callback-scoped JDBC row
     */
    @Override
    public void accept(JdbcClient.Row row) {
        Long id = row.required("id", Long.class);
        contacts.putIfAbsent(id, new Contact(id, row.get("name", String.class)));
    }

    /**
     * Returns contacts in first-seen SQL order.
     *
     * @return deduplicated contacts
     */
    @Override
    public List<Contact> finish() {
        return List.copyOf(contacts.values());
    }
}
