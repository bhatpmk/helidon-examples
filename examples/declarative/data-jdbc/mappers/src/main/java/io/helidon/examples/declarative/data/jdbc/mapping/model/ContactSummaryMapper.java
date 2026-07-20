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
 * Explicit mapper that creates a contact display value from an aggregate SQL row.
 * <p>
 * This mapper and {@link ContactNameMapper} both implement {@code RowMapper<ContactName>}. The repository therefore
 * names this service with {@code @Jdbc.RowMapper(ContactSummaryMapper.class)} when it needs the aggregate
 * interpretation. The mapper is stateless because the Service Registry may share this singleton among concurrent
 * repository calls.
 */
@Service.Singleton
public final class ContactSummaryMapper implements JdbcClient.RowMapper<ContactName> {

    /**
     * Creates the stateless mapper.
     */
    public ContactSummaryMapper() {
    }

    /**
     * Combines the contact name and aggregate counts into one display value.
     *
     * @param row current callback-scoped row
     * @return contact summary
     */
    @Override
    public ContactName map(JdbcClient.Row row) {
        long phoneCount = row.required("phoneCount", Long.class);
        long tagCount = row.required("tagCount", Long.class);
        String displayName = row.required("name", String.class)
                + " (" + phoneCount + " phones, " + tagCount + " tags)";
        return new ContactName(row.required("id", Long.class), displayName);
    }
}
