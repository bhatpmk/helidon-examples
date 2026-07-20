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

import io.helidon.examples.declarative.data.jdbc.storedprocedures.model.OrderRoutineRepository;
import io.helidon.service.registry.Service;

/**
 * Installs the sample's MySQL routines after the JDBC persistence unit has initialized its tables and data.
 * <p>
 * The repository methods intentionally execute DDL with UPDATE execution. The installer is startup-only sample code;
 * production applications should use a migration process with the appropriate operational controls.
 */
@Service.Singleton
@Service.RunLevel(Service.RunLevel.STARTUP)
class OrderRoutineInstaller {
    private final OrderRoutineRepository routines;

    @Service.Inject
    OrderRoutineInstaller(OrderRoutineRepository routines) {
        this.routines = routines;
    }

    /**
     * Recreates the sample routines deterministically after the table initialization script has run.
     */
    @Service.PostConstruct
    void install() {
        routines.dropReserveOrder();
        routines.dropOrderLines();
        routines.dropReservationFee();
        routines.createReserveOrder();
        routines.createOrderLines();
        routines.createReservationFee();
    }
}
