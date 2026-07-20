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

import io.helidon.data.jdbc.JdbcResultRequest;
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
    void generatedSourceUsesTypedProviderOwnedStreamingTerminals() throws Exception {
        String source = generatedRepository();

        assertTrue(source.contains(".map(orderRowRowMapper).visitAll(request)"), source);
        assertTrue(source.contains(".map(orderRowRowMapper).visitWhile(request)"), source);
        assertTrue(source.contains("JdbcResultRequest.VisitAll<OrderRow> request"), source);
        assertTrue(source.contains("JdbcResultRequest.VisitWhile<OrderRow> request"), source);
        assertFalse(source.contains("withRows"), source);
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
    void summaryEndpointUsesDirectVisitAllRequest() {
        OrderSummary summary = new OrderEndpoint(recordingRepository()).summary(11);

        assertEquals(2, summary.orderCount());
        assertEquals(11, summary.firstOrderId());
        assertEquals(12, summary.lastOrderId());
        assertTrue(summary.exhausted());
    }

    @Test
    void endpointUsesVisitAllToConsumeEveryRow() {
        OrderSummary summary = new OrderEndpoint(recordingRepository()).visitOrders(10);

        assertEquals(3, summary.orderCount());
        assertEquals(10, summary.firstOrderId());
        assertEquals(12, summary.lastOrderId());
        assertTrue(summary.exhausted());
    }

    @Test
    void endpointUsesVisitWhileForPredicateDirectedTermination() {
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
            public void visitOrders(JdbcResultRequest.VisitAll<OrderRow> request, long minimumId) {
                rows.stream().filter(row -> row.id() >= minimumId).forEach(request);
            }

            @Override
            public boolean visitOrdersUntil(JdbcResultRequest.VisitWhile<OrderRow> request, long minimumId) {
                for (OrderRow row : rows) {
                    if (row.id() >= minimumId && !request.test(row)) {
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
