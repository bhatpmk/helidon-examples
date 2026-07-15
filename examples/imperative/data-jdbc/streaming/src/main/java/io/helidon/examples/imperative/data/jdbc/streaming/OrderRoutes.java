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
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.helidon.data.jdbc.JdbcQueryRequest;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

/**
 * HTTP routes that demonstrate both provider-owned row traversal terminals.
 */
final class OrderRoutes implements HttpService {

    private final OrderService orders;

    OrderRoutes(OrderService orders) {
        this.orders = orders;
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/summary/{minimumId}", this::summary)
                .get("/for-each/{minimumId}", this::visitAll)
                .get("/for-each-while/{minimumId}/{rowLimit}", this::visitWhile);
    }

    private void summary(ServerRequest request, ServerResponse response) {
        long minimumId = Long.parseLong(request.path().pathParameters().get("minimumId"));
        SummaryAccumulator summary = new SummaryAccumulator(minimumId);
        JdbcQueryRequest.VisitAll<OrderRow> query = JdbcQueryRequest.visitAll(order -> summary.accept(order));
        orders.visitOrders(query, minimumId);
        response.send(summary.result(true));
    }

    private void visitAll(ServerRequest request, ServerResponse response) {
        long minimumId = Long.parseLong(request.path().pathParameters().get("minimumId"));
        SummaryAccumulator summary = new SummaryAccumulator(minimumId);
        JdbcQueryRequest.VisitAll<OrderRow> query = JdbcQueryRequest.<OrderRow>builder()
                .fetchSize(100)
                .queryTimeout(Duration.ofSeconds(30))
                .visitAll(order -> summary.accept(order));
        orders.visitOrders(query, minimumId);
        response.send(summary.result(true));
    }

    private void visitWhile(ServerRequest request, ServerResponse response) {
        long minimumId = Long.parseLong(request.path().pathParameters().get("minimumId"));
        int rowLimit = Integer.parseInt(request.path().pathParameters().get("rowLimit"));
        if (rowLimit < 1) {
            throw new IllegalArgumentException("rowLimit must be greater than zero");
        }
        SummaryAccumulator summary = new SummaryAccumulator(minimumId);
        JdbcQueryRequest.VisitWhile<OrderRow> query = JdbcQueryRequest.<OrderRow>builder()
                .fetchSize(100)
                .queryTimeout(Duration.ofSeconds(30))
                .visitWhile(order -> {
                    summary.accept(order);
                    return summary.orderCount < rowLimit;
                });
        boolean exhausted = orders.visitOrdersUntil(query, minimumId);
        response.send(summary.result(exhausted));
    }

    private static final class SummaryAccumulator {
        private final long minimumId;
        private final Map<String, Integer> ordersByRegion = new LinkedHashMap<>();
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private long firstOrderId = -1;
        private long lastOrderId = -1;
        private int orderCount;

        private SummaryAccumulator(long minimumId) {
            this.minimumId = minimumId;
        }

        private void accept(OrderRow order) {
            if (orderCount == 0) {
                firstOrderId = order.id();
            }
            lastOrderId = order.id();
            orderCount++;
            totalAmount = totalAmount.add(order.amount());
            ordersByRegion.merge(order.region(), 1, Integer::sum);
        }

        private OrderSummary result(boolean exhausted) {
            return new OrderSummary(minimumId,
                                    orderCount,
                                    firstOrderId,
                                    lastOrderId,
                                    totalAmount,
                                    Collections.unmodifiableMap(new LinkedHashMap<>(ordersByRegion)),
                                    exhausted);
        }
    }
}
