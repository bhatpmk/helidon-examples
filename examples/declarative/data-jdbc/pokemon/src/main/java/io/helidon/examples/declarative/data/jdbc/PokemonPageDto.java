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
package io.helidon.examples.declarative.data.jdbc;

import java.util.List;

import io.helidon.examples.declarative.data.jdbc.model.PokemonRow;
import io.helidon.json.binding.Json;

/**
 * HTTP representation of a materialized pokemon page.
 *
 * @param page       zero-based page number
 * @param size       requested page size
 * @param totalSize  total number of matching rows
 * @param totalPages total number of pages
 * @param items      current page content
 */
@Json.Entity
public record PokemonPageDto(int page,
                             int size,
                             int totalSize,
                             int totalPages,
                             List<PokemonDto> items) {

    static PokemonPageDto create(int page,
                                 int size,
                                 List<PokemonRow> rows,
                                 int totalSize) {
        int totalPages = (totalSize + size - 1) / size;
        return new PokemonPageDto(page,
                                  size,
                                  totalSize,
                                  totalPages,
                                  rows.stream().map(PokemonDto::create).toList());
    }
}
