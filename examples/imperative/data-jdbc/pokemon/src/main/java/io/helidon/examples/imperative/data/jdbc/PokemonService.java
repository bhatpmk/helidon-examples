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
package io.helidon.examples.imperative.data.jdbc;

import java.sql.JDBCType;
import java.util.List;
import java.util.Optional;

import io.helidon.data.jdbc.JdbcClient;
import io.helidon.service.registry.Service;
import io.helidon.transaction.Tx;

/**
 * Pokémon data access implemented with the current imperative JDBC API.
 * <p>
 * The methods mirror the declarative Pokémon repository: records are mapped with direct row-mapper lambdas, scalar
 * counts use {@code map(long.class)}, reads use positional binds, DML uses {@code execute()}, and generated keys use
 * {@code generatedKeys(...).one()}. Paging remains explicit MySQL SQL, and the typed VARCHAR bind demonstrates an
 * explicit JDBC type override for an imperative parameter.
 */
@Service.Singleton
class PokemonService {

    private static final String POKEMON_SELECT = """
            SELECT p.ID AS id, p.NAME AS name, p.TYPE_ID AS typeId, t.NAME AS typeName
            FROM POKEMON p
            JOIN TYPE t ON t.ID = p.TYPE_ID
            """;

    private static final JdbcClient.RowMapper<PokemonRow> POKEMON_MAPPER = row -> new PokemonRow(
            row.required("id", Integer.class),
            row.required("name", String.class),
            row.required("typeId", Integer.class),
            row.required("typeName", String.class));

    private static final JdbcClient.RowMapper<TypeRow> TYPE_MAPPER = row -> new TypeRow(
            row.required("id", Integer.class),
            row.required("name", String.class));

    private final JdbcClient jdbcClient;

    @Service.Inject
    PokemonService(@Service.Named("pokemon") JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    List<PokemonDto> listOrderByName() {
        return jdbcClient.create(POKEMON_SELECT + " ORDER BY p.NAME")
                .map(POKEMON_MAPPER)
                .list()
                .stream()
                .map(PokemonDto::create)
                .toList();
    }

    List<PokemonDto> listByTypeName(String typeName) {
        return jdbcClient.create(POKEMON_SELECT + " WHERE t.NAME = ? ORDER BY p.NAME")
                .bind(1, typeName, JDBCType.VARCHAR)
                .map(POKEMON_MAPPER)
                .list()
                .stream()
                .map(PokemonDto::create)
                .toList();
    }

    PokemonPageDto page(int page, int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page must be non-negative and size must be positive");
        }
        int offset = Math.multiplyExact(page, size);
        List<PokemonRow> rows = jdbcClient.create(POKEMON_SELECT + " ORDER BY p.ID LIMIT ? OFFSET ?")
                .bind(1, size)
                .bind(2, offset)
                .map(POKEMON_MAPPER)
                .list();
        long total = count();
        return PokemonPageDto.create(page, size, rows, Math.toIntExact(total));
    }

    List<PokemonDto> after(int id, int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
        return jdbcClient.create(POKEMON_SELECT + " WHERE p.ID > ? ORDER BY p.ID LIMIT ?")
                .bind(1, id)
                .bind(2, size)
                .map(POKEMON_MAPPER)
                .list()
                .stream()
                .map(PokemonDto::create)
                .toList();
    }

    long count() {
        return jdbcClient.create("SELECT COUNT(*) FROM POKEMON")
                .map(long.class)
                .one();
    }

    List<TypeRow> listTypes() {
        return jdbcClient.create("SELECT ID AS id, NAME AS name FROM TYPE ORDER BY NAME")
                .map(TYPE_MAPPER)
                .list();
    }

    Optional<PokemonDto> findByName(String name) {
        return jdbcClient.create(POKEMON_SELECT + " WHERE p.NAME = ?")
                .bind(1, name)
                .map(POKEMON_MAPPER)
                .optional()
                .map(PokemonDto::create);
    }

    @Tx.Required
    PokemonDto insert(PokemonDto pokemon) {
        TypeRow type = getTypeByName(pokemon.type());
        int id = jdbcClient.create("INSERT INTO POKEMON (NAME, TYPE_ID) VALUES (?, ?)")
                .bind(1, pokemon.name())
                .bind(2, type.id())
                .generatedKeys(row -> row.required(1, Integer.class), "ID")
                .one();
        return PokemonDto.create(getById(id));
    }

    @Tx.Required
    long deleteById(int id) {
        return jdbcClient.create("DELETE FROM POKEMON WHERE ID = ?")
                .bind(1, id)
                .execute();
    }

    private TypeRow getTypeByName(String name) {
        return jdbcClient.create("SELECT ID AS id, NAME AS name FROM TYPE WHERE NAME = ?")
                .bind(1, name)
                .map(TYPE_MAPPER)
                .one();
    }

    private PokemonRow getById(int id) {
        return jdbcClient.create(POKEMON_SELECT + " WHERE p.ID = ?")
                .bind(1, id)
                .map(POKEMON_MAPPER)
                .one();
    }
}
