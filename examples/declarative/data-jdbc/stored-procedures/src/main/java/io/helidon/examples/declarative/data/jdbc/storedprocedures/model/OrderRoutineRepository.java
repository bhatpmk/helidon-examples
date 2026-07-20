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

import io.helidon.data.Data;
import io.helidon.data.jdbc.Jdbc;

/**
 * Declarative MySQL routine-definition repository.
 * <p>
 * Routine definition is DDL, so every method uses UPDATE execution. In contrast, callers use CALL execution through
 * {@link OrderRepository}. MySQL's {@code DELIMITER} directive is intentionally absent: it is a mysql-client command,
 * not SQL, and JDBC sends each annotated statement as one complete statement to the server.
 */
@Data.Repository
@Data.Provider("jdbc")
@Data.PersistenceUnit("orders")
public interface OrderRoutineRepository {

    /**
     * Removes the reservation procedure when it exists.
     */
    @Jdbc.Statement("DROP PROCEDURE IF EXISTS RESERVE_ORDER")
    @Jdbc.Execution(Jdbc.ExecutionType.UPDATE)
    void dropReserveOrder();

    /**
     * Removes the direct-result procedure when it exists.
     */
    @Jdbc.Statement("DROP PROCEDURE IF EXISTS GET_ORDER_LINES")
    @Jdbc.Execution(Jdbc.ExecutionType.UPDATE)
    void dropOrderLines();

    /**
     * Removes the fee-calculation function when it exists.
     */
    @Jdbc.Statement("DROP FUNCTION IF EXISTS CALCULATE_RESERVATION_FEE")
    @Jdbc.Execution(Jdbc.ExecutionType.UPDATE)
    void dropReservationFee();

    /**
     * Creates the procedure with IN, INOUT, and OUT parameters.
     */
    @Jdbc.Statement("""
            CREATE PROCEDURE RESERVE_ORDER(
                IN p_order_id BIGINT,
                IN p_customer_id BIGINT,
                IN p_requested_by VARCHAR(80),
                INOUT p_attempts INT,
                OUT p_status VARCHAR(20),
                OUT p_reserved_amount DECIMAL(12, 2)
            )
            BEGIN
                DECLARE v_status VARCHAR(20);
                DECLARE v_total_amount DECIMAL(12, 2);

                SELECT STATUS, TOTAL_AMOUNT
                  INTO v_status, v_total_amount
                  FROM SALES_ORDER
                 WHERE ID = p_order_id
                   AND CUSTOMER_ID = p_customer_id
                 FOR UPDATE;

                IF v_status IS NULL THEN
                    SET p_status = 'NOT_FOUND';
                    SET p_reserved_amount = 0.00;
                ELSEIF v_status = 'RESERVED' THEN
                    SET p_status = 'ALREADY_RESERVED';
                    SET p_reserved_amount = v_total_amount;
                ELSEIF v_status <> 'PENDING' THEN
                    SET p_status = 'NOT_ELIGIBLE';
                    SET p_reserved_amount = 0.00;
                ELSEIF p_attempts >= 3 THEN
                    SET p_status = 'RETRY_LIMIT';
                    SET p_reserved_amount = 0.00;
                ELSE
                    SET p_attempts = p_attempts + 1;
                    UPDATE SALES_ORDER
                       SET STATUS = 'RESERVED',
                           ATTEMPTS = p_attempts,
                           RESERVED_AT = CURRENT_TIMESTAMP
                     WHERE ID = p_order_id;
                    INSERT INTO FULFILLMENT_AUDIT (ORDER_ID, REQUESTED_BY, ATTEMPTS)
                    VALUES (p_order_id, p_requested_by, p_attempts);
                    SET p_status = 'RESERVED';
                    SET p_reserved_amount = v_total_amount;
                END IF;
            END
            """)
    @Jdbc.Execution(Jdbc.ExecutionType.UPDATE)
    void createReserveOrder();

    /**
     * Creates the procedure that exposes order rows as a MySQL direct result set.
     */
    @Jdbc.Statement("""
            CREATE PROCEDURE GET_ORDER_LINES(
                IN p_order_id BIGINT,
                IN p_include_backordered BOOLEAN
            )
            BEGIN
                SELECT ID AS lineId,
                       SKU AS sku,
                       QUANTITY AS quantity,
                       UNIT_PRICE AS unitPrice,
                       CAST(QUANTITY * UNIT_PRICE AS DECIMAL(12, 2)) AS lineTotal
                  FROM ORDER_LINE
                 WHERE ORDER_ID = p_order_id
                   AND (p_include_backordered OR NOT BACKORDERED)
                 ORDER BY ID;
            END
            """)
    @Jdbc.Execution(Jdbc.ExecutionType.UPDATE)
    void createOrderLines();

    /**
     * Creates the pure scalar MySQL function used by the function-return example.
     * <p>
     * The declaration honestly identifies the function as {@code DETERMINISTIC NO SQL}. With MySQL binary logging,
     * function creation nevertheless requires an authorized migration account or the server setting
     * {@code log_bin_trust_function_creators=1}; the disposable sample database disables binary logging. The caller
     * supplies the already-queried order total.
     */
    @Jdbc.Statement("""
            CREATE FUNCTION CALCULATE_RESERVATION_FEE(
                p_order_total DECIMAL(12, 2),
                p_priority BOOLEAN
            )
            RETURNS DECIMAL(12, 2)
            DETERMINISTIC
            NO SQL
            RETURN ROUND(p_order_total * IF(p_priority, 0.015, 0.025), 2)
            """)
    @Jdbc.Execution(Jdbc.ExecutionType.UPDATE)
    void createReservationFee();
}
