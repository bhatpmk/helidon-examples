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

import io.helidon.examples.declarative.data.jdbc.mapping.model.Contact;
import io.helidon.json.binding.Json;

/**
 * HTTP representation of a contact aggregate.
 *
 * @param id     contact identifier
 * @param name   contact name
 * @param phones phone numbers
 */
@Json.Entity
public record ContactDto(Long id,
                         String name,
                         List<PhoneDto> phones) {

    static ContactDto create(Contact contact) {
        return new ContactDto(contact.id(),
                              contact.name(),
                              contact.phones()
                                      .stream()
                                      .map(PhoneDto::create)
                                      .toList());
    }
}
