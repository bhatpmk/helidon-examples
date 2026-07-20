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
package io.helidon.examples.declarative.data.jdbc.mapping;

import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.ServiceRegistryManager;

/**
 * Entry point for the declarative JDBC mapping and reduction example.
 * <p>
 * The application compares implicit scalar and record mapping, an explicit {@code @Jdbc.RowMapper}, a generated
 * record graph selected by {@code @Jdbc.IdentityReducer}, and application-owned {@code @Jdbc.RowReducer}
 * implementations. The full custom reducer demonstrates application-specific graph construction and a composite child
 * identity. Generated repositories execute every path through the public {@code JdbcClient} API.
 */
@Service.GenerateBinding
public class Main {

    static {
        LogConfig.initClass();
    }

    private Main() {
        throw new UnsupportedOperationException("Instances of Main class are not allowed");
    }

    /**
     * Starts the generated declarative application binding.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String... args) {
        LogConfig.configureRuntime();
        ServiceRegistryManager.start(ApplicationBinding.create());
    }

}
