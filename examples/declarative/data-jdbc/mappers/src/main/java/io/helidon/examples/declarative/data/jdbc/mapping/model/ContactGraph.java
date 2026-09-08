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
package io.helidon.examples.declarative.data.jdbc.mapping.model;

import java.util.ArrayList;
import java.util.List;

import io.helidon.json.binding.Json;

/**
 * Mutable root used by the generated identity-defined graph reducer.
 */
@Json.Entity
public class ContactGraph {
    private Long id;
    private String name;
    private List<PhoneGraph> phones = new ArrayList<>();

    /**
     * Creates an empty graph root for generated bean mapping.
     */
    public ContactGraph() {
    }

    /**
     * Returns the contact identifier.
     *
     * @return contact identifier
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the contact identifier.
     *
     * @param id contact identifier
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the contact name.
     *
     * @return contact name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the contact name.
     *
     * @param name contact name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the phone collection.
     *
     * @return phones, never {@code null}
     */
    public List<PhoneGraph> getPhones() {
        return phones;
    }

    /**
     * Sets the phone collection.
     *
     * @param phones phone collection
     */
    public void setPhones(List<PhoneGraph> phones) {
        this.phones = phones;
    }
}
