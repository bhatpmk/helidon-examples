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

import io.helidon.data.Data;

/**
 * Explicit SQL repository for pokemon type lookup.
 */
@Data.Repository
public interface TypeRepository extends Data.GenericRepository<TypeRow, Integer> {

    /**
     * Reads a pokemon type by name.
     *
     * @param name type name
     * @return matching type row
     */
    @Data.Query("SELECT ID AS id, NAME AS name FROM TYPE WHERE NAME = :name")
    TypeRow getByName(String name);

    /**
     * Lists pokemon types with their pokemon rows.
     *
     * @return reduced type aggregates
     */
    @Data.Query("""
            SELECT t.ID AS "id",
                   t.NAME AS "name",
                   p.ID AS "pokemon.id",
                   p.NAME AS "pokemon.name"
            FROM TYPE t
            LEFT JOIN POKEMON p ON p.TYPE_ID = t.ID
            ORDER BY t.NAME, p.NAME
            """)
    List<TypeWithPokemon> listWithPokemon();

}
