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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression checks for the generated declarative callable repository.
 */
class StoredProceduresExampleTest {

    @Test
    void generatedRepositoryContainsCallableLayoutsAndBindings() throws Exception {
        String source = generatedRepository();

        assertTrue(source.contains(".in(1).in(2).in(3).inOut(4, \"attempts\", 4, int.class)"), source);
        assertTrue(source.contains(".out(5, \"status\", 12, String.class)"), source);
        assertTrue(source.contains(".out(6, \"reservedAmount\", 3, BigDecimal.class)"), source);
        assertTrue(source.contains(".bind(1, orderId).bind(2, customerId).bind(3, requestedBy)"
                                          + ".bind(4, attempts).call(CALL_RESERVE_ORDER, request)"), source);
    }

    @Test
    void generatedRepositoryUsesDirectCallForTheMySqlResultSetProcedure() throws Exception {
        String source = generatedRepository();

        assertTrue(source.contains("JdbcCall CALL_ORDER_LINES = JdbcCall.builder().in(1).in(2).build()"), source);
        assertTrue(source.contains("jdbcClient.create(SQL_ORDER_LINES)"), source);
        assertTrue(source.contains(".bind(1, orderId).bind(2, includeBackordered).call(CALL_ORDER_LINES, request)"),
                   source);
        assertFalse(source.contains("ResultSet"), source);
    }

    @Test
    void generatedRepositoryRegistersTheFunctionReturnAtPositionOne() throws Exception {
        String source = generatedRepository();

        assertTrue(source.contains(".returns(\"fee\", 3, BigDecimal.class).in(2).in(3).build()"), source);
        assertTrue(source.contains(".bind(2, orderTotal).bind(3, priority)"
                                           + ".callForOutputs(CALL_RESERVATION_FEE)"),
                   source);
        assertTrue(source.contains("callOutputValues.required(\"fee\", BigDecimal.class)"), source);
    }

    @Test
    void generatedRoutineDefinitionRepositoryExecutesDdlAsUpdates() throws Exception {
        String source = generatedRoutineRepository();

        assertTrue(source.contains("jdbcClient.create(SQL_DROP_RESERVE_ORDER).execute()"), source);
        assertTrue(source.contains("jdbcClient.create(SQL_CREATE_RESERVE_ORDER).execute()"), source);
        assertTrue(source.contains("jdbcClient.create(SQL_CREATE_ORDER_LINES).execute()"), source);
        assertTrue(source.contains("jdbcClient.create(SQL_CREATE_RESERVATION_FEE).execute()"), source);
        assertFalse(source.contains("JdbcCall"), source);
    }

    @Test
    void generatesJsonBindingsForAllHttpInputAndOutputRecords() {
        assertGeneratedJsonBinding("ReserveOrderRequest");
        assertGeneratedJsonBinding("ReservationResult");
        assertGeneratedJsonBinding("OrderLine");
        assertGeneratedJsonBinding("OrderLines");
    }

    private static String generatedRepository() throws Exception {
        return Files.readString(Path.of("target/generated-sources/annotations/io/helidon/examples/declarative/data/jdbc/"
                                                 + "storedprocedures/model/OrderRepository__Jdbc.java"));
    }

    private static String generatedRoutineRepository() throws Exception {
        return Files.readString(Path.of("target/generated-sources/annotations/io/helidon/examples/declarative/data/jdbc/"
                                                 + "storedprocedures/model/OrderRoutineRepository__Jdbc.java"));
    }

    private static void assertGeneratedJsonBinding(String typeName) {
        Path generatedSource = Path.of("target/generated-sources/annotations/io/helidon/examples/declarative/data/jdbc/"
                                              + "storedprocedures/model/"
                                              + typeName
                                              + "__GeneratedConverter.java");
        assertTrue(Files.isRegularFile(generatedSource), () -> "Missing JSON binding: " + generatedSource);
    }
}
