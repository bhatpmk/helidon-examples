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
package io.helidon.examples.declarative.data.jdbc.streaming;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.helidon.examples.declarative.data.jdbc.streaming.model.OrderRepository;
import io.helidon.examples.declarative.data.jdbc.streaming.model.OrderRow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for generated streaming terminal selection.
 */
class StreamingExampleTest {

    @Test
    void generatedSourceUsesAllProviderOwnedStreamingTerminals() throws Exception {
        String source = generatedRepository();

        assertTrue(source.contains(".map(MAPPER_WITH_ROWS).withRows(action)"), source);
        assertTrue(source.contains(".map(MAPPER_VISIT_ORDERS).forEach(action)"), source);
        assertTrue(source.contains(".map(MAPPER_VISIT_ORDERS_UNTIL).forEachWhile(action)"), source);
        assertTrue(source.contains("Consumer<Iterable<OrderRow>>"), source);
        assertTrue(source.contains("Predicate<OrderRow>"), source);
        assertFalse(source.contains("execute().params"), source);
        assertFalse(source.contains("ResultSet"), source);
    }

    @Test
    void summaryRepresentsEmptyTraversalWithoutInventingRows() {
        OrderSummary summary = new OrderSummary(100, 0, -1, -1, java.math.BigDecimal.ZERO,
                                                 java.util.Map.of(), true);

        assertEquals(0, summary.orderCount());
        assertEquals(-1, summary.firstOrderId());
        assertTrue(summary.exhausted());
    }

    @Test
    void endpointUsesWithRowsForScopedPullConsumption() {
        OrderSummary summary = new OrderEndpoint(recordingRepository()).summary(11);

        assertEquals(2, summary.orderCount());
        assertEquals(11, summary.firstOrderId());
        assertEquals(12, summary.lastOrderId());
        assertTrue(summary.exhausted());
    }

    @Test
    void endpointUsesForEachToConsumeEveryRow() {
        OrderSummary summary = new OrderEndpoint(recordingRepository()).visitOrders(10);

        assertEquals(3, summary.orderCount());
        assertEquals(10, summary.firstOrderId());
        assertEquals(12, summary.lastOrderId());
        assertTrue(summary.exhausted());
    }

    @Test
    void endpointUsesForEachWhileForPredicateDirectedTermination() {
        OrderSummary summary = new OrderEndpoint(recordingRepository()).visitOrdersUntil(10, 2);

        assertEquals(2, summary.orderCount());
        assertEquals(10, summary.firstOrderId());
        assertEquals(11, summary.lastOrderId());
        assertFalse(summary.exhausted());
    }

    @Test
    void endpointReportsNormalExhaustionWhenLimitExceedsRows() {
        OrderSummary summary = new OrderEndpoint(recordingRepository()).visitOrdersUntil(12, 2);

        assertEquals(1, summary.orderCount());
        assertEquals(12, summary.firstOrderId());
        assertEquals(12, summary.lastOrderId());
        assertTrue(summary.exhausted());
    }

    private static OrderRepository recordingRepository() {
        return new OrderRepository() {
            private final List<OrderRow> rows = List.of(
                    new OrderRow(10, "Acme", "Americas", new java.math.BigDecimal("10.00")),
                    new OrderRow(11, "Bravo", "Europe", new java.math.BigDecimal("20.00")),
                    new OrderRow(12, "Cedar", "Asia Pacific", new java.math.BigDecimal("30.00")));

            @Override
            public void withRows(long minimumId, Consumer<Iterable<OrderRow>> action) {
                action.accept(rows.stream().filter(row -> row.id() >= minimumId).toList());
            }

            @Override
            public void visitOrders(long minimumId, Consumer<OrderRow> action) {
                rows.stream().filter(row -> row.id() >= minimumId).forEach(action);
            }

            @Override
            public boolean visitOrdersUntil(long minimumId, Predicate<OrderRow> action) {
                for (OrderRow row : rows) {
                    if (row.id() >= minimumId && !action.test(row)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    private static String generatedRepository() throws Exception {
        return Files.readString(Path.of("target/generated-sources/annotations/io/helidon/examples/declarative/data/jdbc/"
                                                 + "streaming/model/OrderRepository__Jdbc.java"));
    }
}
