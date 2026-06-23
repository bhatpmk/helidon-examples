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

import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

/**
 * HTTP routes for the contact mapping sample.
 */
final class ContactRoutes implements HttpService {

    private final ContactService contacts;

    ContactRoutes(ContactService contacts) {
        this.contacts = contacts;
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/automatic", this::automaticReducer)
                .get("/explicit", this::explicitReducer)
                .get("/cards", this::cards);
    }

    private void automaticReducer(ServerRequest req, ServerResponse res) {
        res.send(contacts.listWithDottedLabels()
                         .stream()
                         .map(ContactDto::create)
                         .toList());
    }

    private void explicitReducer(ServerRequest req, ServerResponse res) {
        res.send(contacts.listWithExplicitMapping()
                         .stream()
                         .map(ContactDto::create)
                         .toList());
    }

    private void cards(ServerRequest req, ServerResponse res) {
        res.send(contacts.listCards()
                         .stream()
                         .map(ContactCardDto::create)
                         .toList());
    }
}
