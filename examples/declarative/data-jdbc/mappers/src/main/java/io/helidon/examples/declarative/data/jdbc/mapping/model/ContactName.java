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

import io.helidon.json.binding.Json;

/**
 * Application-defined contact name projection whose components intentionally differ from the SQL column labels.
 *
 * @param contactNumber value read from the SQL {@code id} column
 * @param displayName formatted value derived from the SQL {@code name} column
 */
@Json.Entity
public record ContactName(Long contactNumber,
                          String displayName) {
}
