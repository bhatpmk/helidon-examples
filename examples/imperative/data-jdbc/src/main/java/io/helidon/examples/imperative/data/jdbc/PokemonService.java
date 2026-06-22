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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.helidon.data.jdbc.JdbcClient;

/**
 * Pokemon data access implemented with the imperative JDBC API.
 */
final class PokemonService {

    private static final String POKEMON_SELECT = """
            SELECT p.ID AS id, p.NAME AS name, t.ID AS typeId, t.NAME AS typeName
            FROM POKEMON p JOIN TYPE t ON t.ID = p.TYPE_ID
            """;

    private final JdbcClient jdbcClient;

    PokemonService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    List<PokemonDto> listOrderByName() {
        return jdbcClient.query(POKEMON_SELECT + " ORDER BY p.NAME")
                .fetchSize(32)
                .list(PokemonService::pokemonRow)
                .stream()
                .map(PokemonDto::create)
                .toList();
    }

    List<PokemonDto> listByTypeName(String typeName) {
        return jdbcClient.query(POKEMON_SELECT + " WHERE t.NAME = ? ORDER BY p.NAME")
                .bind(1, typeName)
                .list(PokemonService::pokemonRow)
                .stream()
                .map(PokemonDto::create)
                .toList();
    }

    Optional<PokemonDto> findByName(String name) {
        return jdbcClient.query(POKEMON_SELECT + " WHERE p.NAME = ?")
                .bind(1, name)
                .optional(PokemonService::pokemonRow)
                .map(PokemonDto::create);
    }

    PokemonDto insert(PokemonDto pokemon) {
        TypeRow type = getTypeByName(pokemon.type());
        int id = jdbcClient.update("INSERT INTO POKEMON (NAME, TYPE_ID) VALUES (?, ?)")
                .bind(1, pokemon.name())
                .bind(2, type.id())
                .generatedKey(row -> ((Number) row.get(1)).intValue(), "ID")
                .orElseThrow();
        return PokemonDto.create(getById(id));
    }

    long deleteById(int id) {
        return jdbcClient.update("DELETE FROM POKEMON WHERE ID = ?")
                .bind(1, id)
                .execute();
    }

    List<TypeWithPokemon> listTypesWithPokemon() {
        List<TypePokemonRow> rows = jdbcClient.query("""
                        SELECT t.ID AS id,
                               t.NAME AS name,
                               p.ID AS pokemonId,
                               p.NAME AS pokemonName
                        FROM TYPE t
                        LEFT JOIN POKEMON p ON p.TYPE_ID = t.ID
                        ORDER BY t.NAME, p.NAME
                        """)
                .list(PokemonService::typePokemonRow);

        Map<Integer, TypeAccumulator> types = new LinkedHashMap<>();
        for (TypePokemonRow row : rows) {
            TypeAccumulator type = types.computeIfAbsent(row.id(), id -> new TypeAccumulator(id, row.name()));
            if (row.pokemonId() != null) {
                type.pokemon().add(new PokemonSummary(row.pokemonId(), row.pokemonName()));
            }
        }
        return types.values()
                .stream()
                .map(TypeAccumulator::toTypeWithPokemon)
                .toList();
    }

    List<PokemonDto> listWithInvalidSqlSyntax() {
        return jdbcClient.query("SELECT FROM POKEMON")
                .list(PokemonService::pokemonRow)
                .stream()
                .map(PokemonDto::create)
                .toList();
    }

    private TypeRow getTypeByName(String name) {
        return jdbcClient.query("SELECT ID AS id, NAME AS name FROM TYPE WHERE NAME = ?")
                .bind(1, name)
                .one(row -> new TypeRow(row.intValue("id"), row.string("name")));
    }

    private PokemonRow getById(int id) {
        return jdbcClient.query(POKEMON_SELECT + " WHERE p.ID = ?")
                .bind(1, id)
                .one(PokemonService::pokemonRow);
    }

    private static PokemonRow pokemonRow(JdbcClient.Row row) {
        return new PokemonRow(row.intValue("id"),
                              row.string("name"),
                              row.intValue("typeId"),
                              row.string("typeName"));
    }

    private static TypePokemonRow typePokemonRow(JdbcClient.Row row) {
        Number pokemonId = (Number) row.get("pokemonId");
        return new TypePokemonRow(row.intValue("id"),
                                  row.string("name"),
                                  pokemonId == null ? null : pokemonId.intValue(),
                                  row.string("pokemonName"));
    }

    private record TypePokemonRow(int id, String name, Integer pokemonId, String pokemonName) {
    }

    private record TypeAccumulator(int id, String name, List<PokemonSummary> pokemon) {

        private TypeAccumulator(int id, String name) {
            this(id, name, new ArrayList<>());
        }

        private TypeWithPokemon toTypeWithPokemon() {
            return new TypeWithPokemon(id, name, List.copyOf(pokemon));
        }
    }
}
