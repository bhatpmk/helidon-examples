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
import java.util.Optional;

import io.helidon.common.Api;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.examples.declarative.data.jdbc.mapping.model.Contact;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactCard;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactDetail;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactGraph;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactName;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactPhone;
import io.helidon.examples.declarative.data.jdbc.mapping.model.ContactRepository;
import io.helidon.examples.declarative.data.jdbc.mapping.model.CustomContactGraph;
import io.helidon.http.Http;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.RestServer;

/**
 * HTTP views of the mapping strategies declared by {@link ContactRepository}.
 * <p>
 * The endpoints expose generated record mapping, exact and generic one-row mapper service selection, generated graph
 * reduction, and an application reducer that creates a custom record graph with composite phone identity. The
 * endpoint never receives a JDBC row or resource; all mapping and reduction finishes before a repository method returns.
 */
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
    @Http.Path("/all")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    List<Contact> all() {
        return contacts.listContacts();
    }

    @Http.GET
    @Http.Path("/get/{id}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    Optional<Contact> get(@Http.PathParam("id") long id) {
        return contacts.findContact(id);
    }

    @Http.GET
    @Http.Path("/mapped/{id}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    ContactName mapped(@Http.PathParam("id") long id) {
        return contacts.mappedContact(id);
    }

    @Http.GET
    @Http.Path("/mapped-summary/{id}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    ContactName mappedSummary(@Http.PathParam("id") long id) {
        return contacts.mappedContactSummary(id);
    }

    @Http.GET
    @Http.Path("/mapped-phone/{id}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    ContactPhone mappedPhone(@Http.PathParam("id") long id) {
        return contacts.mappedPrimaryPhone(id);
    }

    @Http.GET
    @Http.Path("/names")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    List<String> names() {
        return contacts.listNames();
    }

    @Http.GET
    @Http.Path("/details")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    List<ContactDetail> details() {
        return contacts.listDetails();
    }

    @Http.GET
    @Http.Path("/cards")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    List<ContactCard> cards() {
        return contacts.listCards();
    }

    @Http.GET
    @Http.Path("/graphs")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    List<ContactGraph> graphs() {
        return contacts.listGraphs();
    }

    @Http.GET
    @Http.Path("/custom-graphs")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    List<CustomContactGraph> customGraphs() {
        return contacts.listCustomGraphs();
    }

    @Http.GET
    @Http.Path("/custom-reducer")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    List<Contact> customReducer() {
        return contacts.listWithCustomReducer();
    }
}
