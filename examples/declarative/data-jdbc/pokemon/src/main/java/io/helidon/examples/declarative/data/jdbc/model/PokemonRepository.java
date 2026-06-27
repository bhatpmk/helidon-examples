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
import java.util.Optional;

import io.helidon.data.Data;

@Data.Repository
public interface PokemonRepository extends Data.GenericRepository<PokemonRow, Integer> {

    /**
     * Lists all pokemon rows ordered by name.
     *
     * @return ordered pokemon rows
     */
    @Data.Map(value = "type_id", target = "typeId")
    @Data.Query("""
            SELECT p.ID AS id, p.NAME AS name, t.ID AS type_id, t.NAME AS typeName
            FROM POKEMON p JOIN TYPE t ON t.ID = p.TYPE_ID
            ORDER BY p.NAME
            """)
    List<PokemonRow> listOrderByName();

    /**
     * Lists pokemon rows by type name.
     *
     * @param requestedTypeName pokemon type name
     * @return matching pokemon rows
     */
    @Data.Query("""
            SELECT p.ID AS id, p.NAME AS name, t.ID AS typeId, t.NAME AS typeName
            FROM POKEMON p JOIN TYPE t ON t.ID = p.TYPE_ID
            WHERE t.NAME = :typeName
            ORDER BY p.NAME
            """)
    List<PokemonRow> listByTypeName(@Data.Param("typeName") String requestedTypeName);

    /**
     * Finds one pokemon row by name.
     *
     * @param name pokemon name
     * @return matching row, or empty when the row is not present
     */
    @Data.Query("""
            SELECT p.ID AS id, p.NAME AS name, t.ID AS typeId, t.NAME AS typeName
            FROM POKEMON p JOIN TYPE t ON t.ID = p.TYPE_ID
            WHERE p.NAME = :name
            """)
    Optional<PokemonRow> findByName(String name);

    /**
     * Reads one pokemon row by identifier.
     *
     * @param pokemonId pokemon identifier
     * @return matching row
     */
    @Data.Query("""
            SELECT p.ID AS id, p.NAME AS name, t.ID AS typeId, t.NAME AS typeName
            FROM POKEMON p JOIN TYPE t ON t.ID = p.TYPE_ID
            WHERE p.ID = ?
            """)
    PokemonRow getById(@Data.Param(index = 1) int pokemonId);

    /**
     * Inserts a pokemon row.
     *
     * @param name   pokemon name
     * @param typeId pokemon type identifier
     * @return generated pokemon identifier
     */
    @Data.Query("INSERT INTO POKEMON (NAME, TYPE_ID) VALUES (:name, :typeId)")
    @Data.GeneratedKeys("ID")
    int insert(String name, int typeId);

    /**
     * Deletes a pokemon row by identifier.
     *
     * @param id pokemon identifier
     * @return number of rows deleted
     */
    @Data.Query("DELETE FROM POKEMON WHERE ID = :id")
    long deleteById(int id);

    /**
     * Demonstrates how invalid user supplied SQL fails at runtime.
     * <p>
     * The missing select list is intentional. Helidon Data JDBC does not parse or validate SQL at
     * build time; the database reports syntax errors when the generated repository method executes the statement.
     *
     * @return this method is expected to fail before returning rows
     */
    @Data.Query("""
            SELECT FROM POKEMON
            """)
    List<PokemonRow> listWithInvalidSqlSyntax();

}
