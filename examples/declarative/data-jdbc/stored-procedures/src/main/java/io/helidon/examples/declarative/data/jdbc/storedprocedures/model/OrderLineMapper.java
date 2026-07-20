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
package io.helidon.examples.declarative.data.jdbc.storedprocedures.model;

import java.math.BigDecimal;

import io.helidon.data.jdbc.JdbcClient;
import io.helidon.service.registry.Service;

/**
 * Explicit mapper for rows returned directly by the MySQL procedure.
 * <p>
 * Direct callable result sets are mapped by the application callback, rather than by a repository return-type
 * inference rule. The mapper is stateless and safe for concurrent use by the singleton endpoint.
 */
@Service.Singleton
public final class OrderLineMapper implements JdbcClient.RowMapper<OrderLine> {

    /**
     * Creates the stateless mapper.
     */
    public OrderLineMapper() {
    }

    /**
     * Maps the current callback-scoped row.
     *
     * @param row current row
     * @return detached order line
     */
    @Override
    public OrderLine map(JdbcClient.Row row) {
        return new OrderLine(row.required("lineId", Long.class),
                             row.required("sku", String.class),
                             row.required("quantity", Integer.class),
                             row.required("unitPrice", BigDecimal.class),
                             row.required("lineTotal", BigDecimal.class));
    }
}
