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

import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

/**
 * HTTP routes for the imperative mapping sample.
 * <p>
 * The route set mirrors the declarative mapper application. The service has already completed row mapping or
 * reduction before a response is serialized; routes never access JDBC resources directly.
 */
final class ContactRoutes implements HttpService {

    private final ContactService contacts;

    ContactRoutes(ContactService contacts) {
        this.contacts = contacts;
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/all", this::all)
                .get("/get/{id}", this::get)
                .get("/mapped/{id}", this::mapped)
                .get("/names", this::names)
                .get("/details", this::details)
                .get("/cards", this::cards)
                .get("/graphs", this::graphs)
                .get("/immutable-graphs", this::immutableGraphs)
                .get("/custom-reducer", this::customReducer);
    }

    private void all(ServerRequest req, ServerResponse res) {
        res.send(contacts.listContacts());
    }

    private void get(ServerRequest req, ServerResponse res) {
        long id = Long.parseLong(req.path().pathParameters().get("id"));
        contacts.findContact(id).ifPresentOrElse(res::send, () -> res.status(Status.NOT_FOUND_404).send());
    }

    private void mapped(ServerRequest req, ServerResponse res) {
        long id = Long.parseLong(req.path().pathParameters().get("id"));
        res.send(contacts.mappedContact(id));
    }

    private void names(ServerRequest req, ServerResponse res) {
        res.send(contacts.listNames());
    }

    private void details(ServerRequest req, ServerResponse res) {
        res.send(contacts.listDetails());
    }

    private void cards(ServerRequest req, ServerResponse res) {
        res.send(contacts.listCards());
    }

    private void graphs(ServerRequest req, ServerResponse res) {
        res.send(contacts.listGraphs());
    }

    private void immutableGraphs(ServerRequest req, ServerResponse res) {
        res.send(contacts.listImmutableGraphs());
    }

    private void customReducer(ServerRequest req, ServerResponse res) {
        res.send(contacts.listWithCustomReducer());
    }
}
