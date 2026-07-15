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
package io.helidon.examples.declarative.data.jdbc.streaming.model;

import io.helidon.data.Data;
import io.helidon.data.jdbc.JdbcQueryRequest;

/**
 * Declarative repository demonstrating callback-based JDBC row traversal.
 * <p>
 * {@link OrderRow} is a record whose components match the SQL labels, so code generation creates the row mapper
 * without an annotation. The leading request selects the traversal terminal and is not bound to SQL. A
 * {@code RowReducer} is intentionally not involved because these methods deliver independent physical rows while the
 * provider advances the cursor.
 */
@Data.Repository
@Data.Provider("jdbc")
@Data.PersistenceUnit("streaming")
public interface OrderRepository {

    /**
     * Visit every matching order while the provider owns the JDBC resources.
     *
     * @param request   callback and invocation-specific statement settings
     * @param minimumId first order identifier to include
     */
    @Data.Query("""
            SELECT ID AS id,
            CUSTOMER AS customer,
            REGION AS region,
            AMOUNT AS amount
              FROM SALES_ORDER
              WHERE ID >= :minimumId
              ORDER BY ID
            """)
    void visitOrders(JdbcQueryRequest.VisitAll<OrderRow> request, long minimumId);

    /**
     * Visit matching orders until all rows are exhausted or the predicate returns {@code false}.
     *
     * Consume stops normally when the predicate returns false and
     *   reports whether exhaustion was normal
     *
     * @param request   continuation predicate and invocation-specific statement settings
     * @param minimumId first order identifier to include
     * @return {@code true} after normal exhaustion, or {@code false} after predicate-directed termination
     */
    @Data.Query("""
            SELECT ID AS id,
                   CUSTOMER AS customer,
                   REGION AS region,
                   AMOUNT AS amount
            FROM SALES_ORDER
            WHERE ID >= :minimumId
            ORDER BY ID
            """)
    boolean visitOrdersUntil(JdbcQueryRequest.VisitWhile<OrderRow> request, long minimumId);
}
