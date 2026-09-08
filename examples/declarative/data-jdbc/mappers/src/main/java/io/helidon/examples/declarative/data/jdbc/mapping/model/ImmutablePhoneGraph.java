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

import java.util.List;

import io.helidon.json.binding.Json;

/**
 * Immutable phone node in the application-reduced contact graph.
 * <p>
 * {@link ImmutableContactGraphReducer} deliberately identifies a phone by the composite {@code (type, phone)} key.
 * The generated graph reducer supports one scalar identity property, so this application-defined identity is one
 * reason to select {@code @Data.RowReducer} instead.
 *
 * @param databaseId database identifier retained as projected data
 * @param type       phone type
 * @param phone      phone number
 * @param tags       ordered, immutable tags belonging to the phone
 */
@Json.Entity
public record ImmutablePhoneGraph(Long databaseId,
                                  String type,
                                  String phone,
                                  List<ImmutableTagGraph> tags) {
}
