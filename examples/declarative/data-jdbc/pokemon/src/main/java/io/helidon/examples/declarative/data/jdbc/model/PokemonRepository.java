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

import java.sql.JDBCType;
import java.util.List;
import java.util.Optional;

import io.helidon.data.Data;

/**
 * Explicit SQL repository for Pokémon rows and their joined type projection.
 * <p>
 * Every read method returns either a supported scalar or {@link PokemonRow}, whose record components match the SQL
 * column labels. Helidon therefore generates a direct row mapper and uses {@code one()}, {@code optional()}, or
 * {@code list()} according to the declared return type. The join produces one flat record per physical result row, so
 * neither {@link Data.BeanMapper} nor {@link Data.RowReducer} is needed.
 */
@Data.Repository
@Data.Provider("jdbc")
@Data.PersistenceUnit("pokemon")
public interface PokemonRepository {

    /**
     * Lists all pokemon rows ordered by name.
     *
     * @return ordered pokemon rows
     */
    @Data.Query("""
            SELECT p.ID AS id, p.NAME AS name, p.TYPE_ID AS typeId, t.NAME AS typeName
            FROM POKEMON p
            JOIN TYPE t ON t.ID = p.TYPE_ID
            ORDER BY p.NAME
            """)
    List<PokemonRow> listOrderByName();

    /**
     * Reads one page using explicit database-specific paging SQL.
     *
     * @param size   maximum number of rows
     * @param offset zero-based row offset
     * @return rows in identifier order
     */
    @Data.Query("""
            SELECT p.ID AS id, p.NAME AS name, p.TYPE_ID AS typeId, t.NAME AS typeName
            FROM POKEMON p
            JOIN TYPE t ON t.ID = p.TYPE_ID
            ORDER BY p.ID
            LIMIT :size OFFSET :offset
            """)
    List<PokemonRow> pageOrderById(int size, int offset);

    /**
     * Reads rows after an identifier using explicit keyset-paging SQL.
     *
     * @param id   exclusive identifier boundary
     * @param size maximum number of rows
     * @return rows in identifier order
     */
    @Data.Query("""
            SELECT p.ID AS id, p.NAME AS name, p.TYPE_ID AS typeId, t.NAME AS typeName
            FROM POKEMON p
            JOIN TYPE t ON t.ID = p.TYPE_ID
            WHERE p.ID > :id
            ORDER BY p.ID
            LIMIT :size
            """)
    List<PokemonRow> sliceAfterId(int id, int size);

    /**
     * Counts all pokemon rows.
     *
     * @return total row count
     */
    @Data.Query("SELECT COUNT(*) FROM POKEMON")
    long count();

    /**
     * Lists pokemon rows by type name.
     *
     * @param typeName pokemon type name
     * @return matching pokemon rows
     */
    @Data.Query("""
            SELECT p.ID AS id, p.NAME AS name, p.TYPE_ID AS typeId, t.NAME AS typeName
            FROM POKEMON p
            JOIN TYPE t ON t.ID = p.TYPE_ID
            WHERE t.NAME = :typeName
            ORDER BY p.NAME
            """)
    List<PokemonRow> listByTypeName(@Data.JdbcType(JDBCType.VARCHAR) String typeName);

    /**
     * Finds one pokemon row by name.
     *
     * @param name pokemon name
     * @return matching row, or empty when the row is not present
     */
    @Data.Query("""
            SELECT p.ID AS id, p.NAME AS name, p.TYPE_ID AS typeId, t.NAME AS typeName
            FROM POKEMON p
            JOIN TYPE t ON t.ID = p.TYPE_ID
            WHERE p.NAME = :name
            """)
    Optional<PokemonRow> findByName(String name);

    /**
     * Finds one pokemon row by identifier.
     *
     * @param id pokemon identifier
     * @return matching row, or empty when the row is not present
     */
    @Data.Query("""
            SELECT p.ID AS id, p.NAME AS name, p.TYPE_ID AS typeId, t.NAME AS typeName
            FROM POKEMON p
            JOIN TYPE t ON t.ID = p.TYPE_ID
            WHERE p.ID = :id
            """)
    Optional<PokemonRow> findById(int id);

    /**
     * Inserts one pokemon row.
     *
     * @param pokemonName pokemon name
     * @param typeId      pokemon type identifier
     * @return database-generated pokemon identifier
     */
    @Data.Update("INSERT INTO POKEMON (NAME, TYPE_ID) VALUES (:pokemonName, :typeId)")
    @Data.GeneratedKeys
    int insertPokemon(String pokemonName, int typeId);

    /**
     * Deletes one pokemon row by identifier.
     *
     * @param id pokemon identifier
     * @return affected row count
     */
    @Data.Update("DELETE FROM POKEMON WHERE ID = :id")
    long deleteById(int id);
}
