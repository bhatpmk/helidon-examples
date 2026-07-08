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

import io.helidon.data.jdbc.JdbcClient;

/**
 * Explicit mapper for one physical contact row.
 * <p>
 * {@code @Data.RowMapper(ContactNameMapper.class)} tells code generation to construct this class directly and pass it
 * to {@code JdbcClient.map}. The generated repository keeps one mapper instance, so this implementation is stateless.
 * It performs no cross-row aggregation or duplicate suppression.
 */
public final class ContactNameMapper implements JdbcClient.RowMapper<Contact> {

    /**
     * Creates the stateless mapper.
     */
    public ContactNameMapper() {
    }

    /**
     * Maps the current row to a contact record.
     *
     * @param row current callback-scoped row
     * @return mapped contact
     */
    @Override
    public Contact map(JdbcClient.Row row) {
        return new Contact(row.get("id", Long.class), row.get("name", String.class));
    }
}
