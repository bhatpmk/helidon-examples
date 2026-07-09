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

import java.util.function.Consumer;
import java.util.function.Predicate;

import io.helidon.data.Data;

/**
 * Declarative repository demonstrating callback-based JDBC row traversal.
 * <p>
 * {@link OrderRow} is a record whose components match the SQL labels, so code generation creates the row mapper
 * without an annotation. The trailing callback selects the traversal terminal. A {@code RowReducer} is intentionally
 * not involved because result-set reduction must finish before it can expose a logical graph, while these methods
 * deliver mapped physical rows synchronously as the provider advances the cursor.
 */
@Data.Repository
@Data.Provider("jdbc")
@Data.PersistenceUnit("streaming")
public interface OrderRepository {

    /**
     * Visit every matching order while the provider owns the JDBC resources.
     *
     * @param minimumId first order identifier to include
     * @param action    synchronous row consumer
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
    void visitOrders(long minimumId, Consumer<OrderRow> action);

    /**
     * Visit matching orders until all rows are exhausted or the predicate returns {@code false}.
     *
     * Consume stops normally when the predicate returns false and
     *   reports whether exhaustion was normal
     *
     * @param minimumId first order identifier to include
     * @param action    synchronous continuation predicate
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
    boolean visitOrdersUntil(long minimumId, Predicate<OrderRow> action);


    /**
     * Consume matching orders while the provider owns the JDBC resources.
     * <p>
     * The callback must process the iterable synchronously and must not retain it after returning.
     *
     * @param minimumId first order identifier to include
     * @param action    synchronous row consumer
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
    void withRows(long minimumId, Consumer<Iterable<OrderRow>> action);
}
