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

import io.helidon.http.Status;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

/**
 * HTTP routes for the imperative Pokémon sample.
 *
 * <p>The routes mirror the declarative example. The service behind them uses the same SQL and terminal shapes,
 * but constructs the public {@code JdbcClient} chains directly.</p>
 */
final class PokemonRoutes implements HttpService {

    private final PokemonService pokemonService;

    PokemonRoutes(PokemonService pokemonService) {
        this.pokemonService = pokemonService;
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get("/all", this::list)
                .get("/type/{name}", this::listByType)
                .get("/page/{page}/{size}", this::page)
                .get("/after/{id}/{size}", this::after)
                .get("/types", this::listTypes)
                .get("/get/{name}", this::findByName)
                .post("/", this::insert)
                .delete("/{id}", this::delete);
    }

    private void list(ServerRequest req, ServerResponse res) {
        res.send(pokemonService.listOrderByName());
    }

    private void listByType(ServerRequest req, ServerResponse res) {
        String typeName = req.path().pathParameters().get("name");
        res.send(pokemonService.listByTypeName(typeName));
    }

    private void page(ServerRequest req, ServerResponse res) {
        int page = Integer.parseInt(req.path().pathParameters().get("page"));
        int size = Integer.parseInt(req.path().pathParameters().get("size"));
        res.send(pokemonService.page(page, size));
    }

    private void after(ServerRequest req, ServerResponse res) {
        int id = Integer.parseInt(req.path().pathParameters().get("id"));
        int size = Integer.parseInt(req.path().pathParameters().get("size"));
        res.send(pokemonService.after(id, size));
    }

    private void listTypes(ServerRequest req, ServerResponse res) {
        res.send(pokemonService.listTypes());
    }

    private void findByName(ServerRequest req, ServerResponse res) {
        String name = req.path().pathParameters().get("name");
        pokemonService.findByName(name).ifPresentOrElse(res::send, () -> res.status(Status.NOT_FOUND_404).send());
    }

    private void insert(ServerRequest req, ServerResponse res) {
        PokemonDto pokemon = req.content().as(PokemonDto.class);
        res.send(pokemonService.insert(pokemon));
    }

    private void delete(ServerRequest req, ServerResponse res) {
        int id = Integer.parseInt(req.path().pathParameters().get("id"));
        long count = pokemonService.deleteById(id);
        res.send("Deleted: " + count + " values");
    }

}
