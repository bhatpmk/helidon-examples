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
import java.util.Optional;

import io.helidon.common.Api;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.examples.declarative.data.jdbc.model.PokemonRepository;
import io.helidon.examples.declarative.data.jdbc.model.TypeRow;
import io.helidon.examples.declarative.data.jdbc.model.TypeRepository;
import io.helidon.http.Http;
import io.helidon.service.registry.Service;
import io.helidon.transaction.Tx;
import io.helidon.webserver.http.RestServer;

@SuppressWarnings(Api.SUPPRESS_INCUBATING) // Helidon declarative is an incubating feature
@Http.Path("/pokemon")
@Service.Singleton
@RestServer.Endpoint
class PokemonEndpoint {

    private final PokemonRepository pokemonRepository;
    private final TypeRepository typeRepository;

    @Service.Inject
    PokemonEndpoint(PokemonRepository pokemonRepository,
                    TypeRepository typeRepository) {
        this.pokemonRepository = pokemonRepository;
        this.typeRepository = typeRepository;
    }

    @Http.GET
    @Http.Path("/all")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    List<PokemonDto> all() {
        return pokemonRepository.listOrderByName()
                .stream()
                .map(PokemonDto::create)
                .toList();
    }

    @Http.GET
    @Http.Path("/type/{name}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    List<PokemonDto> type(@Http.PathParam("name") String name) {
        return pokemonRepository.listByTypeName(name)
                .stream()
                .map(PokemonDto::create)
                .toList();
    }

    @Http.GET
    @Http.Path("/get/{name}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    Optional<PokemonDto> pokemon(@Http.PathParam("name") String name) {
        return pokemonRepository.findByName(name)
                .map(PokemonDto::create);
    }

    @Http.POST
    @Http.Consumes(MediaTypes.APPLICATION_JSON_VALUE)
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    PokemonDto insert(@Http.Entity PokemonDto pokemonDto) {
        return insertPokemon(pokemonDto);
    }

    @Tx.Required
    PokemonDto insertPokemon(PokemonDto pokemonDto) {
        // These repository calls share one resource-local JDBC transaction managed by Helidon Transactions.
        TypeRow type = typeRepository.getByName(pokemonDto.type());
        pokemonRepository.insert(pokemonDto.name(), type.id());
        return PokemonDto.create(pokemonRepository.getByName(pokemonDto.name()));
    }

    @Http.DELETE
    @Http.Path("/{id}")
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    String delete(@Http.PathParam("id") int id) {
        return "Deleted: " + pokemonRepository.deleteById(id) + " values";
    }

    @Http.GET
    @Http.Path("/failure/invalid-sql")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    List<PokemonDto> invalidSqlSyntax() {
        // This endpoint exists only to demonstrate failure propagation from invalid @Data.Query SQL.
        return pokemonRepository.listWithInvalidSqlSyntax()
                .stream()
                .map(PokemonDto::create)
                .toList();
    }

}
