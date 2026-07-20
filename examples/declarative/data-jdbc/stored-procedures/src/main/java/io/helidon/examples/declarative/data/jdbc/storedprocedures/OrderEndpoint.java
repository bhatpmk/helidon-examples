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
package io.helidon.examples.declarative.data.jdbc.storedprocedures;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.helidon.common.Api;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.data.jdbc.JdbcClient;
import io.helidon.data.jdbc.JdbcResultRequest;
import io.helidon.data.jdbc.JdbcStatementOptions;
import io.helidon.examples.declarative.data.jdbc.storedprocedures.model.OrderLine;
import io.helidon.examples.declarative.data.jdbc.storedprocedures.model.OrderLineMapper;
import io.helidon.examples.declarative.data.jdbc.storedprocedures.model.OrderLines;
import io.helidon.examples.declarative.data.jdbc.storedprocedures.model.OrderRepository;
import io.helidon.examples.declarative.data.jdbc.storedprocedures.model.ReservationResult;
import io.helidon.examples.declarative.data.jdbc.storedprocedures.model.ReserveOrderRequest;
import io.helidon.http.Http;
import io.helidon.service.registry.Service;
import io.helidon.transaction.Tx;
import io.helidon.webserver.http.RestServer;

/**
 * HTTP facade for the declarative stored-procedure repository.
 * <p>
 * Resource-bearing direct results are consumed inside callbacks; scalar-only function output uses detached return mode.
 * Every returned value is detached, so no JDBC resource can outlive the repository invocation.
 */
@SuppressWarnings(Api.SUPPRESS_INCUBATING) // Helidon declarative is an incubating feature
@Http.Path("/orders")
@Service.Singleton
@RestServer.Endpoint
class OrderEndpoint {

    private final OrderRepository orders;
    private final OrderLineMapper orderLineMapper;

    @Service.Inject
    OrderEndpoint(OrderRepository orders, OrderLineMapper orderLineMapper) {
        this.orders = orders;
        this.orderLineMapper = orderLineMapper;
    }

    /**
     * Calls a procedure with three IN values, one INOUT value, and two scalar OUT values.
     * <p>
     * The transaction is important because the procedure locks and updates the order while reserving it.
     *
     * @param orderId order to reserve
     * @param request procedure input values
     * @return detached procedure result
     */
    @Http.POST
    @Http.Path("/{orderId}/reserve")
    @Http.Consumes(MediaTypes.APPLICATION_JSON_VALUE)
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    @Tx.Required
    ReservationResult reserve(@Http.PathParam("orderId") long orderId,
                              @Http.Entity ReserveOrderRequest request) {
        return orders.reserveOrder(
                JdbcResultRequest.call(call -> {
                    // Direct results must be consumed before any OUT or INOUT value is read.
                    call.results().discard();
                    int attempts = call.outputs().required("attempts", Integer.class);
                    String status = call.outputs().required("status", String.class);
                    BigDecimal amount = call.outputs().required("reservedAmount", BigDecimal.class);
                    return new ReservationResult(orderId, attempts, status, amount);
                }),
                orderId,
                request.customerId(),
                request.requestedBy(),
                request.attempts());
    }

    /**
     * Calls a MySQL procedure that returns order lines as a direct result set.
     * <p>
     * MySQL does not support a portable {@code REF_CURSOR} OUT parameter. Its normal equivalent is a {@code SELECT}
     * emitted by the procedure, which Helidon exposes through the ordered direct-result callback API.
     *
     * @param orderId order whose lines should be returned
     * @param includeBackordered whether backordered lines should be included
     * @return detached order lines
     */
    @Http.GET
    @Http.Path("/{orderId}/lines/{includeBackordered}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    OrderLines lines(@Http.PathParam("orderId") long orderId,
                     @Http.PathParam("includeBackordered") boolean includeBackordered) {
        JdbcStatementOptions options = JdbcStatementOptions.builder()
                .fetchSize(100)
                .queryTimeout(Duration.ofSeconds(10))
                .build();
        return orders.orderLines(
                JdbcResultRequest.call(call -> {
                    List<OrderLine> lines = new ArrayList<>();
                    call.results().visit(new JdbcClient.CallResultVisitor() {
                        @Override
                        public void rows(int resultSetIndex, JdbcClient.CallRows rows) {
                            rows.map(orderLineMapper)
                                    .visitAll(JdbcResultRequest.visitAll(lines::add));
                        }
                    });
                    return new OrderLines(orderId, List.copyOf(lines));
                }).withOptions(options),
                orderId,
                includeBackordered);
    }

    /**
     * Obtains an order total with a query, then calls a scalar MySQL function using detached return mode.
     *
     * @param orderId order whose total supplies the calculation
     * @param priority whether priority pricing applies
     * @return detached function return value
     */
    @Http.GET
    @Http.Path("/{orderId}/reservation-fee/{priority}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    BigDecimal reservationFee(@Http.PathParam("orderId") long orderId,
                              @Http.PathParam("priority") boolean priority) {
        BigDecimal orderTotal = orders.orderTotal(orderId);
        return orders.reservationFee(orderTotal, priority);
    }
}
