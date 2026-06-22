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
package io.helidon.examples.declarative.data.jdbc.model;

import java.util.List;

/**
 * Reduced type row with its pokemon children.
 *
 * @param id      type identifier
 * @param name    type name
 * @param pokemon pokemon rows for the type
 */
public record TypeWithPokemon(int id,
                              String name,
                              List<PokemonSummary> pokemon) {
}
