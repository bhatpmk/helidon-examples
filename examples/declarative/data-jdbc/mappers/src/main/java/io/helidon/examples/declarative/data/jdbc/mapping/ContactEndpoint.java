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

import java.util.List;

import io.helidon.common.Api;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactRepository;
import io.helidon.http.Http;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

@SuppressWarnings(Api.SUPPRESS_INCUBATING) // Helidon declarative is an incubating feature
@Http.Path("/contacts")
@Service.Singleton
@RestServer.Endpoint
class ContactEndpoint {

    private final ContactRepository contacts;

    @Service.Inject
    ContactEndpoint(ContactRepository contacts) {
        this.contacts = contacts;
    }

    @Http.GET
    @Http.Path("/automatic")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    List<ContactDto> automaticReducer() {
        return contacts.listWithAutomaticReducer()
                .stream()
                .map(ContactDto::create)
                .toList();
    }

    @Http.GET
    @Http.Path("/explicit")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    List<ContactDto> explicitReducer() {
        return contacts.listWithExplicitReducer()
                .stream()
                .map(ContactDto::create)
                .toList();
    }

    @Http.GET
    @Http.Path("/cards")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    List<ContactCardDto> cards() {
        return contacts.listCards()
                .stream()
                .map(ContactCardDto::create)
                .toList();
    }

}
