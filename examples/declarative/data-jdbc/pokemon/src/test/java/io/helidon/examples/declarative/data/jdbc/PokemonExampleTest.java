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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import io.helidon.examples.declarative.data.jdbc.model.PokemonRow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for generated Pokemon repository calls and DTO behavior.
 */
class PokemonExampleTest {

    @Test
    void generatedSourceUsesAllQueryTerminals() throws Exception {
        String source = generatedRepository();

        assertTrue(source.contains(".map(pokemonRowRowMapper).list()"), source);
        assertTrue(source.contains(".map(pokemonRowRowMapper).optional()"), source);
        assertTrue(source.contains("jdbcClient.create(SQL_COUNT).map(long.class).one()"), source);
    }

    @Test
    void generatedSourceUsesDmlAndGeneratedKeys() throws Exception {
        String source = generatedRepository();

        assertTrue(source.contains(".generatedKeys(row -> row.required(1, Integer.class)).one()"), source);
        assertTrue(source.contains("jdbcClient.create(SQL_DELETE_BY_ID)"), source);
        assertTrue(source.contains(".execute();"), source);
        assertTrue(source.contains(".bind(1, typeName).map(pokemonRowRowMapper).list()"), source);
        assertTrue(source.contains("LIMIT ? OFFSET ?"), source);
        assertFalse(source.contains("PageRequest"), source);
        assertFalse(source.contains("GenericRepository"), source);
    }

    @Test
    void dtoConversionPreservesOptionalIdentifier() {
        PokemonDto dto = PokemonDto.create(new PokemonRow(7, "Ekans", 4, "Poison"));

        assertEquals(Optional.of(7), dto.id());
        assertEquals("Ekans", dto.name());
        assertEquals("Poison", dto.type());
    }

    @Test
    void pageDtoCalculatesMetadataFromExplicitSqlResults() {
        PokemonPageDto page = PokemonPageDto.create(1,
                                                     2,
                                                     java.util.List.of(new PokemonRow(3, "Machop", 2, "Fighting")),
                                                     5);

        assertEquals(1, page.page());
        assertEquals(3, page.totalPages());
        assertEquals(1, page.items().size());
    }

    private static String generatedRepository() throws Exception {
        return Files.readString(Path.of("target/generated-sources/annotations/io/helidon/examples/declarative/data/jdbc/"
                                                 + "model/PokemonRepository__Jdbc.java"));
    }
}
