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

/**
 * Mutable tag node used by the generated graph reducer.
 */
public class TagGraph {
    private Long id;
    private String name;

    /**
     * Creates an empty tag node for generated bean mapping.
     */
    public TagGraph() {
    }

    /** @return tag identifier */
    public Long getId() {
        return id;
    }

    /** @param id tag identifier */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return tag name */
    public String getName() {
        return name;
    }

    /** @param name tag name */
    public void setName(String name) {
        this.name = name;
    }
}
