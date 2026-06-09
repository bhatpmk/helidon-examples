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

/**
 * Explicit SQL repository used by the JDBC POC.
 * <p>
 * The interface intentionally does not extend {@link Data.CrudRepository}. The current POC supports
 * user-declared repository methods backed by {@link Data.Query}; it does not use JPA entity metadata or
 * query-by-method-name generation.
 */
@Data.Repository
public interface PokemonRepository {

    /**
     * Lists all pokemon rows ordered by name.
     *
     * @return ordered pokemon rows
     */
    @Data.Query("""
            SELECT p.ID AS id, p.NAME AS name, t.ID AS typeId, t.NAME AS typeName
            FROM POKEMON p JOIN TYPE t ON t.ID = p.TYPE_ID
            ORDER BY p.NAME
            """)
    List<PokemonRow> listOrderByName();

    /**
     * Lists pokemon rows by type name.
     *
     * @param typeName pokemon type name
     * @return matching pokemon rows
     */
    @Data.Query("""
            SELECT p.ID AS id, p.NAME AS name, t.ID AS typeId, t.NAME AS typeName
            FROM POKEMON p JOIN TYPE t ON t.ID = p.TYPE_ID
            WHERE t.NAME = :typeName
            ORDER BY p.NAME
            """)
    List<PokemonRow> listByTypeName(String typeName);

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
     * Reads one pokemon row by name.
     *
     * @param name pokemon name
     * @return matching row
     */
    @Data.Query("""
            SELECT p.ID AS id, p.NAME AS name, t.ID AS typeId, t.NAME AS typeName
            FROM POKEMON p JOIN TYPE t ON t.ID = p.TYPE_ID
            WHERE p.NAME = :name
            """)
    PokemonRow getByName(String name);

    /**
     * Inserts a pokemon row.
     *
     * @param name   pokemon name
     * @param typeId pokemon type identifier
     * @return number of rows inserted
     */
    @Data.Query("INSERT INTO POKEMON (NAME, TYPE_ID) VALUES (:name, :typeId)")
    long insert(String name, int typeId);

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
     * The missing select list is intentional. The JDBC POC does not parse or validate SQL at
     * build time; the database reports syntax errors when the generated repository method executes the statement.
     *
     * @return this method is expected to fail before returning rows
     */
    @Data.Query("""
            SELECT FROM POKEMON
            """)
    List<PokemonRow> listWithInvalidSqlSyntax();

}
