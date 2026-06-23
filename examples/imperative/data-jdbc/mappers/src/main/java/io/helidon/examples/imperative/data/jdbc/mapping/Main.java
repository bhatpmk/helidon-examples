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
package io.helidon.examples.imperative.data.jdbc.mapping;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.helidon.config.Config;
import io.helidon.data.jdbc.JdbcClient;
import io.helidon.http.media.MediaContext;
import io.helidon.http.media.json.binding.JsonBindingSupport;
import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.Services;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;

/**
 * The application main class.
 */
public final class Main {

    /**
     * Cannot be instantiated.
     */
    private Main() {
    }

    /**
     * Application main entry point.
     *
     * @param args command line arguments.
     */
    public static void main(String[] args) {
        LogConfig.configureRuntime();

        Config config = Services.get(Config.class);
        HikariDataSource dataSource = dataSource(config);
        Runtime.getRuntime().addShutdownHook(new Thread(dataSource::close));

        config.get("data.init-script")
                .asString()
                .ifPresent(script -> runInitScript(dataSource, script));

        ContactService contactService = new ContactService(JdbcClient.create(dataSource));
        WebServer server = WebServer.builder()
                .config(config.get("server"))
                .mediaContext(MediaContext.builder()
                                      .addMediaSupport(JsonBindingSupport.create())
                                      .build())
                .routing(routing -> routing(routing, contactService))
                .build()
                .start();

        System.out.println("WEB server is up! http://localhost:" + server.port() + "/contacts");
    }

    private static void routing(HttpRouting.Builder routing, ContactService contactService) {
        routing.register("/contacts", new ContactRoutes(contactService));
    }

    private static HikariDataSource dataSource(Config config) {
        Config hikari = config.get("data.sources.sql.0.provider.hikari");
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setUsername(hikari.get("username").asString().get());
        hikariConfig.setPassword(hikari.get("password").asString().get());
        hikariConfig.setJdbcUrl(hikari.get("url").asString().get());
        hikariConfig.setDriverClassName(hikari.get("jdbc-driver-class-name").asString().get());
        return new HikariDataSource(hikariConfig);
    }

    private static void runInitScript(DataSource dataSource, String script) {
        try (InputStream stream = Main.class.getResourceAsStream("/" + script)) {
            if (stream == null) {
                throw new IllegalStateException("Classpath resource not found: " + script);
            }
            executeScript(dataSource, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read init script: " + script, e);
        }
    }

    private static void executeScript(DataSource dataSource, String script) {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            for (String sql : script.split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) {
                    statement.execute(trimmed);
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to execute init script.", e);
        }
    }
}
