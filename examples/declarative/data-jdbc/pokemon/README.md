Helidon Data Declarative JDBC Example
----

This example demonstrates Helidon Data JDBC declarative APIs.
It uses explicit SQL repository methods and does not depend on Jakarta Persistence or EclipseLink.

The example uses two repository interfaces:

- `PokemonRepository`
- `TypeRepository`

The application puts the Helidon Data JDBC code generator on the annotation processor path and marks
repositories with `@Data.Provider("jdbc")`. The repositories use the named JDBC persistence unit
`pokemon`, configured under `data.persistence-units.jdbc` in `application.yaml`.

Repository methods use `@Jdbc.Statement` for SQL. Most reads are inferred from their mapped return types. The scalar
count declares `@Jdbc.Execution(QUERY)`, and the update-count method declares `@Jdbc.Execution(UPDATE)`, because a
primitive numeric return alone cannot distinguish those operations. The paginated SQL contains database-side `LIMIT`
and `OFFSET` clauses, so the SQL remains explicit and database-specific. The insert method uses `@Jdbc.GeneratedKeys`,
which implies update execution and returns the generated pokemon identifier.

Query-by-method-name, JPA entity mapping, relationship reducers, and historical POC mapper annotations
such as `@Data.Map`, `@Data.Mapper`, `@Data.MapWith`, and `@Data.ReduceWith` are intentionally not active
in this sample.

## Start the Database

To run the application, a MySQL database is required. You can start the database with the necessary
configuration using the following Docker command:

```shell
docker run --name mysql \
       -p 3306:3306 \
       -e MYSQL_DATABASE='pokemons' \
       -e MYSQL_RANDOM_ROOT_PASSWORD='yes' \
       -e MYSQL_USER='user' \
       -e MYSQL_PASSWORD='changeit' \
       -d mysql
```

The application runs `src/main/resources/init.sql` through the JDBC persistence unit `init-script`
setting when it starts. This is intended for examples, tests, and simple bootstrap data. It is not a
database migration facility; use a migration tool for production schema evolution. The script drops
and recreates the sample tables on startup.

## Build and Run

1. Build the application using Maven:

```shell
mvn package
```

2. Run the application:

```shell
java -jar target/helidon-examples-declarative-data-jdbc-pokemon.jar
```

## Test Example

The application provides the `http://localhost:8080/pokemon` endpoint.

### PokemonRepository.listOrderByName()

Lists all pokemon rows ordered by name.

```shell
curl http://localhost:8080/pokemon/all
```

### PokemonRepository.listByTypeName(String typeName)

Lists all pokemon rows for one type. The sample below uses `Normal`.

```shell
curl http://localhost:8080/pokemon/type/Normal
```

### PokemonRepository.pageOrderById(int size, int offset)

Returns an offset-based page with five rows. The method passes the row limit and offset as ordinary SQL parameters.
The endpoint runs the separate count query to calculate total rows and pages.

```shell
curl http://localhost:8080/pokemon/page/0/5
curl http://localhost:8080/pokemon/page/3/5
curl http://localhost:8080/pokemon/page/7/5
```

### PokemonRepository.sliceAfterId(int afterId, int size)

Returns the next five rows after pokemon identifier `10`. This is keyset pagination: `afterId` is part of the database
predicate, and `size` supplies the SQL row limit.

```shell
curl http://localhost:8080/pokemon/after/10/5
curl http://localhost:8080/pokemon/after/15/5
```

### PokemonRepository.findByName(String name)

Finds a pokemon row by name and returns `404 Not Found` if no row is found.
The sample below uses `Meowth`.

```shell
curl http://localhost:8080/pokemon/get/Meowth
```

### TypeRepository.listOrderByName()

Lists pokemon types ordered by name.

```shell
curl http://localhost:8080/pokemon/types
```

### PokemonRepository.insertPokemon(String pokemonName, int typeId)

Inserts one pokemon row through a declarative repository method. `@Jdbc.GeneratedKeys` makes the
repository return the database-generated identifier, which the endpoint uses for the read-back. The
type lookup, insert, and read-back execute in one `@Tx.Required` transaction.

```shell
curl -X POST http://localhost:8080/pokemon \
        -H 'Content-Type: application/json' \
        -d '{"name":"Electabuzz","type":"Electric"}'
```

### PokemonRepository.deleteById(int id)

Deletes one pokemon row through a declarative repository method and returns the affected-row count.
The endpoint is transactional.

```shell
curl -X DELETE http://localhost:8080/pokemon/100
```
