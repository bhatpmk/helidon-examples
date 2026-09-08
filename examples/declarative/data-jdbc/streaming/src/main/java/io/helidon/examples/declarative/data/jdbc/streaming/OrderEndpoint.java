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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import io.helidon.common.Api;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.examples.declarative.data.jdbc.streaming.model.OrderRepository;
import io.helidon.examples.declarative.data.jdbc.streaming.model.OrderRow;
import io.helidon.http.Http;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

@SuppressWarnings(Api.SUPPRESS_INCUBATING) // Helidon declarative is an incubating feature
@Http.Path("/orders")
@Service.Singleton
@RestServer.Endpoint
class OrderEndpoint {

    private final OrderRepository orders;

    @Service.Inject
    OrderEndpoint(OrderRepository orders) {
        this.orders = orders;
    }

    /**
     * Consume matching rows inside the repository callback and return only a bounded summary.
     *
     * @param minimumId first order identifier to include
     * @return summary produced while the JDBC cursor is open
     */
    @Http.GET
    @Http.Path("/summary/{minimumId}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    OrderSummary summary(@Http.PathParam("minimumId") long minimumId) {
        SummaryAccumulator summary = new SummaryAccumulator(minimumId);

        // The generated repository keeps JDBC resources open only while this callback executes.
        orders.withRows(minimumId, rows -> rows.forEach(order -> summary.accept(order)));

        // withRows has now closed the result set, statement, and logical connection handle.
        return summary.result(true);
    }

    /**
     * Push every matching order into the application callback and return a bounded summary.
     *
     * @param minimumId first order identifier to include
     * @return summary produced from all matching rows
     */
    @Http.GET
    @Http.Path("/for-each/{minimumId}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    OrderSummary visitOrders(@Http.PathParam("minimumId") long minimumId) {
        SummaryAccumulator summary = new SummaryAccumulator(minimumId);

        // The callback receives one mapped row at a time; no result list is created.
        orders.visitOrders(minimumId, order -> summary.accept(order));
        return summary.result(true);
    }

    /**
     * Push matching orders until the requested row limit is reached.
     *
     * @param minimumId first order identifier to include
     * @param rowLimit  maximum number of rows to consume
     * @return summary and whether the database result was normally exhausted
     */
    @Http.GET
    @Http.Path("/for-each-while/{minimumId}/{rowLimit}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    OrderSummary visitOrdersUntil(@Http.PathParam("minimumId") long minimumId,
                                   @Http.PathParam("rowLimit") int rowLimit) {
        if (rowLimit < 1) {
            throw new IllegalArgumentException("rowLimit must be greater than zero");
        }

        SummaryAccumulator summary = new SummaryAccumulator(minimumId);
        boolean exhausted = orders.visitOrdersUntil(minimumId, order -> {
            summary.accept(order);
            return summary.orderCount < rowLimit;
        });
        return summary.result(exhausted);
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
