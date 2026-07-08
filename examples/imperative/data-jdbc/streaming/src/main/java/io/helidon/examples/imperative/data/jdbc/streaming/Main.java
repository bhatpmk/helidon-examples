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

import io.helidon.config.Config;
import io.helidon.data.jdbc.JdbcClient;
import io.helidon.http.media.MediaContext;
import io.helidon.http.media.json.binding.JsonBindingSupport;
import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.Services;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;

/**
 * Entry point for the imperative provider-owned streaming example.
 * <p>
 * The service registry creates the named JDBC client from the configured persistence unit. Application code then
 * builds public {@code JdbcClient} chains and keeps every traversal callback synchronous.
 */
public final class Main {

    private Main() {
    }

    /**
     * Starts the HTTP server.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        LogConfig.configureRuntime();
        Config config = Services.get(Config.class);
        OrderService orders = new OrderService(Services.getNamed(JdbcClient.class, "streaming"));
        WebServer server = WebServer.builder()
                .config(config.get("server"))
                .mediaContext(MediaContext.builder()
                                      .addMediaSupport(JsonBindingSupport.create())
                                      .build())
                .routing(routing -> routing(routing, orders))
                .build()
                .start();
        System.out.println("WEB server is up! http://localhost:" + server.port() + "/orders");
    }

    private static void routing(HttpRouting.Builder routing, OrderService orders) {
        routing.register("/orders", new OrderRoutes(orders));
    }
}
