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
import io.helidon.service.registry.Service;

/**
 * Generic mapper service selected by {@code @Jdbc.RowMapper()} for {@link ContactPhone}.
 * <p>
 * The repository does not name this implementation. Code generation requests the
 * {@code JdbcClient.RowMapper<ContactPhone>} service contract, and the Service Registry supplies this singleton. The
 * mapper demonstrates application logic by combining two SQL columns into the {@link ContactPhone#phoneLabel()}
 * component.
 */
@Service.Singleton
public final class ContactPhoneMapper implements JdbcClient.RowMapper<ContactPhone> {

    /**
     * Creates the stateless mapper service.
     */
    public ContactPhoneMapper() {
    }

    /**
     * Maps one joined contact and phone row.
     *
     * @param row current callback-scoped row
     * @return mapped contact phone
     */
    @Override
    public ContactPhone map(JdbcClient.Row row) {
        String phoneLabel = row.required("phoneType", String.class)
                + ": "
                + row.required("phoneNumber", String.class);
        return new ContactPhone(row.required("contactId", Long.class),
                                row.required("contactName", String.class),
                                phoneLabel);
    }
}
