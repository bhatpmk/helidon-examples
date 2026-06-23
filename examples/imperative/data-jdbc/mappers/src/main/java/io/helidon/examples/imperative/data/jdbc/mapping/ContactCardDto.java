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

import io.helidon.examples.imperative.data.jdbc.mapping.model.ContactCard;
import io.helidon.json.binding.Json;

/**
 * HTTP representation of a contact card projection.
 *
 * @param id contact identifier
 * @param displayName display name
 * @param primaryPhone primary phone number
 * @param phoneCount phone count
 * @param tagCount tag count
 */
@Json.Entity
public record ContactCardDto(Long id,
                             String displayName,
                             String primaryPhone,
                             long phoneCount,
                             long tagCount) {

    static ContactCardDto create(ContactCard card) {
        return new ContactCardDto(card.id(),
                                  card.displayName(),
                                  card.primaryPhone(),
                                  card.phoneCount(),
                                  card.tagCount());
    }
}
