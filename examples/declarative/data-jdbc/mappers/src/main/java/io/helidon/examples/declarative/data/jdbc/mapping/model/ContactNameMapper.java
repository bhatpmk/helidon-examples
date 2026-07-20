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

import java.util.Locale;

import io.helidon.data.jdbc.JdbcClient;
import io.helidon.service.registry.Service;

/**
 * Explicit mapper from SQL labels {@code id} and {@code name} to the differently shaped {@link ContactName} record.
 * <p>
 * {@code @Jdbc.RowMapper(ContactNameMapper.class)} selects this exact service type for the annotated repository method.
 * The generated repository receives the service through constructor injection and passes it to
 * {@code JdbcClient.map}. The mapper changes {@code id} to {@code contactNumber} and converts {@code name} to the
 * uppercase {@code displayName}; generated record mapping cannot infer either rule from the result type. This singleton
 * is stateless because one instance may serve concurrent repository calls. It maps one physical row and performs no
 * cross-row aggregation or duplicate suppression.
 */
@Service.Singleton
public final class ContactNameMapper implements JdbcClient.RowMapper<ContactName> {

    /**
     * Creates the stateless mapper.
     */
    public ContactNameMapper() {
    }

    /**
     * Maps the current row to an application-defined contact name.
     *
     * @param row current callback-scoped row
     * @return mapped and formatted contact name
     */
    @Override
    public ContactName map(JdbcClient.Row row) {
        return new ContactName(row.required("id", Long.class),
                               row.required("name", String.class).toUpperCase(Locale.ROOT));
    }
}
