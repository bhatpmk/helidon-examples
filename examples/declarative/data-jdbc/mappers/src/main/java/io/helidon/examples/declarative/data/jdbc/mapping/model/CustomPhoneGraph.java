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
 * Custom phone node in the application-reduced contact graph.
 * <p>
 * {@link CustomContactGraphReducer} deliberately identifies a phone by the composite {@code (type, phone)} key and
 * maps SQL aliases that do not match this record's component paths. This application-controlled projection and
 * validation policy demonstrates when to select {@code @Jdbc.RowReducer} instead of generated identity reduction.
 *
 * @param databaseId database identifier retained as projected data
 * @param type       phone type
 * @param phone      phone number
 * @param tags       ordered tags belonging to the phone
 */
@Json.Entity
public record CustomPhoneGraph(Long databaseId,
                                  String type,
                                  String phone,
                                  List<CustomTagGraph> tags) {
}
