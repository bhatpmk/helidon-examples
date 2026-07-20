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
package io.helidon.examples.imperative.data.jdbc.mapping.model;

import io.helidon.data.jdbc.JdbcClient;

/**
 * Explicit imperative mapper for one contact row.
 * <p>
 * It demonstrates the same public row-mapper contract selected declaratively with {@code @Data.RowMapper}. The
 * mapper handles one physical row only; it does not deduplicate or assemble a graph.
 */
public final class ContactNameMapper implements JdbcClient.RowMapper<Contact> {

    /**
     * Creates the stateless mapper.
     */
    public ContactNameMapper() {
    }

    /**
     * Maps the current callback-scoped row.
     *
     * @param row current row
     * @return mapped contact
     */
    @Override
    public Contact map(JdbcClient.Row row) {
        return new Contact(row.required("id", Long.class), row.required("name", String.class));
    }
}
