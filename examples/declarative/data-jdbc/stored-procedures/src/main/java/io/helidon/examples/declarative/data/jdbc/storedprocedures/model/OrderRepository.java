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
import java.sql.Types;

import io.helidon.data.Data;
import io.helidon.data.jdbc.Jdbc;
import io.helidon.data.jdbc.JdbcResultRequest;

/**
 * Declarative stored-procedure repository for a small order fulfillment workflow.
 * <p>
 * The first method demonstrates multiple IN values, one INOUT value, and two scalar OUT values. The second method
 * demonstrates a MySQL procedure that returns a direct result set. MySQL does not expose Oracle/PostgreSQL-style
 * {@link Types#REF_CURSOR} OUT parameters, so the direct result set is the portable MySQL variation of a procedure
 * returning order rows. The callback consumes it before the provider closes the callable statement.
 */
@Data.Repository
@Data.Provider("jdbc")
@Data.PersistenceUnit("orders")
public interface OrderRepository {

    /**
     * Reserves an order and records the caller in the fulfillment audit trail.
     *
     * @param request callback that consumes outputs while the callable statement is open
     * @param orderId order to reserve
     * @param customerId customer owning the order
     * @param requestedBy service or operator requesting the reservation
     * @param attempts current retry count, returned as an updated INOUT value
     * @return detached reservation result containing all output values
     */
    @Jdbc.Statement("{call RESERVE_ORDER(:orderId, :customerId, :requestedBy, :attempts, :status, :reservedAmount)}")
    @Jdbc.Execution(Jdbc.ExecutionType.CALL)
    @Jdbc.OutParameter(name = "status", jdbcType = Types.VARCHAR, javaType = String.class)
    @Jdbc.OutParameter(name = "reservedAmount", jdbcType = Types.DECIMAL, javaType = BigDecimal.class)
    ReservationResult reserveOrder(
            JdbcResultRequest.CallWith<ReservationResult> request,
            @Jdbc.InParameter(name = "orderId") long orderId,
            @Jdbc.InParameter(name = "customerId") long customerId,
            @Jdbc.InParameter(name = "requestedBy") String requestedBy,
            @Jdbc.InOutParameter(name = "attempts", jdbcType = Types.INTEGER) int attempts);

    /**
     * Returns order lines as a direct result set produced by the MySQL procedure.
     *
     * @param request callback that must consume or discard direct results before returning
     * @param orderId order whose lines should be returned
     * @param includeBackordered whether backordered lines should be included
     * @return detached rows collected by the callback
     */
    @Jdbc.Statement("{call GET_ORDER_LINES(:orderId, :includeBackordered)}")
    @Jdbc.Execution(Jdbc.ExecutionType.CALL)
    OrderLines orderLines(
            JdbcResultRequest.CallWith<OrderLines> request,
            @Jdbc.InParameter(name = "orderId") long orderId,
            @Jdbc.InParameter(name = "includeBackordered") boolean includeBackordered);

    /**
     * Obtains the order total used as the input to the pure fee-calculation function.
     *
     * @param orderId order whose total should be returned
     * @return order total
     */
    @Jdbc.Statement("SELECT TOTAL_AMOUNT FROM SALES_ORDER WHERE ID = :orderId")
    @Jdbc.Execution(Jdbc.ExecutionType.QUERY)
    BigDecimal orderTotal(long orderId);

    /**
     * Calculates the reservation fee through a pure MySQL scalar function.
     *
     * @param orderTotal order total supplied to the calculation
     * @param priority whether the order receives the priority fee rate
     * @return detached function return value read and closed by the provider
     */
    @Jdbc.Statement("{:fee = call CALCULATE_RESERVATION_FEE(:orderTotal, :priority)}")
    @Jdbc.Execution(Jdbc.ExecutionType.CALL)
    @Jdbc.ReturnParameter(name = "fee", jdbcType = Types.DECIMAL, javaType = BigDecimal.class)
    BigDecimal reservationFee(
            @Jdbc.InParameter(name = "orderTotal") BigDecimal orderTotal,
            @Jdbc.InParameter(name = "priority") boolean priority);
}
