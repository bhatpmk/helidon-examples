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
 * Mutable phone node used by the generated graph reducer.
 */
@Json.Entity
public class PhoneGraph {
    private Long id;
    private String type;
    private String phone;
    private List<TagGraph> tags = new ArrayList<>();

    /**
     * Creates an empty phone node for generated bean mapping.
     */
    public PhoneGraph() {
    }

    /** @return phone identifier */
    public Long getId() {
        return id;
    }

    /** @param id phone identifier */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return phone type */
    public String getType() {
        return type;
    }

    /** @param type phone type */
    public void setType(String type) {
        this.type = type;
    }

    /** @return phone number */
    public String getPhone() {
        return phone;
    }

    /** @param phone phone number */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /** @return tags, never {@code null} */
    public List<TagGraph> getTags() {
        return tags;
    }

    /** @param tags tag collection */
    public void setTags(List<TagGraph> tags) {
        this.tags = tags;
    }
}
