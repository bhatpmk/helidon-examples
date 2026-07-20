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
package io.helidon.examples.imperative.data.jdbc.streaming;

import java.math.BigDecimal;

import io.helidon.data.jdbc.JdbcClient;
import io.helidon.data.jdbc.JdbcResultRequest;

/**
 * Imperative callback-based row traversal that mirrors the declarative repository.
 * <p>
 * Both methods use the same SQL, mapper, and public client calls that generated repositories use. The request carries
 * the callback and invocation-specific statement settings. The provider owns the connection, statement, and result
 * set, and closes them before each terminal returns.
 */
final class OrderService {

    private static final String SQL = """
            SELECT ID AS id,
                   CUSTOMER AS customer,
                   REGION AS region,
                   AMOUNT AS amount
            FROM SALES_ORDER
            WHERE ID >= ?
            ORDER BY ID
            """;

    private static final JdbcClient.RowMapper<OrderRow> MAPPER = row -> new OrderRow(
            row.required("id", Long.class),
            row.required("customer", String.class),
            row.required("region", String.class),
            row.required("amount", BigDecimal.class));

    private final JdbcClient jdbcClient;

    OrderService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    void visitOrders(JdbcResultRequest.VisitAll<OrderRow> request, long minimumId) {
        jdbcClient.create(SQL)
                .bind(1, minimumId)
                .map(MAPPER)
                .visitAll(request);
    }

    boolean visitOrdersUntil(JdbcResultRequest.VisitWhile<OrderRow> request, long minimumId) {
        return jdbcClient.create(SQL)
                .bind(1, minimumId)
                .map(MAPPER)
                .visitWhile(request);
    }
}
