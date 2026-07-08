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
import io.helidon.data.DataException;
import io.helidon.examples.declarative.data.jdbc.model.PokemonRepository;
import io.helidon.examples.declarative.data.jdbc.model.TypeRepository;
import io.helidon.examples.declarative.data.jdbc.model.TypeRow;
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
    @Http.Path("/page/{page}/{size}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    PokemonPageDto page(@Http.PathParam("page") int page,
                        @Http.PathParam("size") int size) {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("page must be non-negative and size must be positive");
        }
        int offset = Math.multiplyExact(page, size);
        return PokemonPageDto.create(page,
                                     size,
                                     pokemonRepository.pageOrderById(size, offset),
                                     Math.toIntExact(pokemonRepository.count()));
    }

    @Http.GET
    @Http.Path("/after/{id}/{size}")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    List<PokemonDto> after(@Http.PathParam("id") int id,
                           @Http.PathParam("size") int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
        return pokemonRepository.sliceAfterId(id, size)
                .stream()
                .map(PokemonDto::create)
                .toList();
    }

    @Http.GET
    @Http.Path("/types")
    @Http.Produces(MediaTypes.APPLICATION_JSON_VALUE)
    List<TypeRow> types() {
        return typeRepository.listOrderByName();
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
    @Tx.Required
    PokemonDto insert(@Http.Entity PokemonDto pokemon) {
        TypeRow type = typeRepository.getByName(pokemon.type());
        int generatedId = pokemonRepository.insertPokemon(pokemon.name(), type.id());
        return pokemonRepository.findById(generatedId)
                .map(PokemonDto::create)
                .orElseThrow(() -> new DataException("Inserted pokemon row was not found: " + generatedId));
    }

    @Http.DELETE
    @Http.Path("/{id}")
    @Http.Produces(MediaTypes.TEXT_PLAIN_VALUE)
    @Tx.Required
    String delete(@Http.PathParam("id") int id) {
        long count = pokemonRepository.deleteById(id);
        return "Deleted rows: " + count;
    }
}
