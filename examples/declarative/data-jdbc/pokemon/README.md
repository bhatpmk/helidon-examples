Helidon Data Declarative JDBC Example
----

This example demonstrates Helidon Data JDBC declarative APIs.
It uses explicit SQL repository methods and does not depend on Jakarta Persistence or EclipseLink.
For complex result mapping, declarative mapper contracts, and relationship reducer examples, see the
neighboring `../mappers` example.

The example uses two repository interfaces:

- `PokemonRepository`
- `TypeRepository`

The application puts the Helidon Data JDBC code generator on the annotation processor path, so these
repositories do not need `@Data.Provider("jdbc")`. The application configures a single JDBC persistence unit under
`data.persistence-units.jdbc` in `application.yaml`, so the generated repositories use it without a repository-level
`@Data.PersistenceUnit` annotation. Repository methods use `@Data.Query`; query-by-method-name and JPA
entity mapping are intentionally not used by this example. One joined query method uses
`@Data.Map` to illustrate mapping a SQL column label to a record component, and the insert method uses
`@Data.GeneratedKeys` to return the database-generated primary key. `TypeRepository.listWithPokemon()`
uses quoted dotted SQL column labels to demonstrate automatic relationship reducer generation.

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

The application provides `http://localhost:8080/pokemon` endpoint.

The following commands map each `PokemonRepository` method to the HTTP endpoint that invokes it.
`TypeRepository.getByName(String name)` is used internally by the insert flow to resolve the supplied
type name to a `TYPE.ID` value. The insert flow is annotated with `@Tx.Required`, so the type lookup,
insert, and final lookup by generated id run in one resource-local JDBC transaction.

### PokemonRepository.listOrderByName()

Lists all pokemon rows ordered by name.

```shell
curl http://localhost:8080/pokemon/all
```

This invokes:

```java
pokemonRepository.listOrderByName()
```

### PokemonRepository.listByTypeName(String typeName)

Lists all pokemon rows for one type. The sample below uses `Normal`.

```shell
curl http://localhost:8080/pokemon/type/Normal
```

This invokes:

```java
pokemonRepository.listByTypeName("Normal")
```

### PokemonRepository.findByName(String name)

Finds a pokemon row by name and returns an empty response body if no row is found.
The sample below uses `Meowth`.

```shell
curl http://localhost:8080/pokemon/get/Meowth
```

This invokes:

```java
pokemonRepository.findByName("Meowth")
```

### TypeRepository.listWithPokemon()

Lists pokemon types with their pokemon rows. The SQL labels use paths such as `"pokemon.id"` and
`"pokemon.name"`, so the JDBC code generator creates a relationship reducer automatically.

```shell
curl http://localhost:8080/pokemon/types
```

This invokes:

```java
typeRepository.listWithPokemon()
```

### PokemonRepository.insert(String name, int typeId)

Inserts a pokemon row. The HTTP endpoint delegates to a `@Tx.Required` method that first calls
`typeRepository.getByName("Fire")` to resolve the type id, then calls
`pokemonRepository.insert("Charmander", type.id())`. The repository method is annotated with
`@Data.GeneratedKeys("ID")`, so it returns the generated pokemon id.

```shell
curl -i -X POST -H 'Content-type: application/json' -d '{"name":"Charmander","type":"Fire"}' http://localhost:8080/pokemon
```

This invokes:

```java
@Tx.Required
TypeRow type = typeRepository.getByName("Fire");
int id = pokemonRepository.insert("Charmander", type.id());
pokemonRepository.getById(id);
```

### PokemonRepository.getById(int id)

Reads the inserted row back by generated id after the insert operation.

```shell
curl http://localhost:8080/pokemon/get/Charmander
```

This direct curl uses `findByName`, because the sample exposes optional lookup as the public GET API.
The `getById` repository method itself is exercised by the POST command above.

### PokemonRepository.deleteById(int id)

Deletes a pokemon row by id. If you inserted `Charmander` using the command above, the expected id is `20`
when the database was initialized by this sample.

```shell
curl -i -X DELETE http://localhost:8080/pokemon/20
```

This invokes:

```java
pokemonRepository.deleteById(20)
```

### PokemonRepository.listWithInvalidSqlSyntax()

Demonstrates a failure caused by invalid SQL supplied in `@Data.Query`. The repository method contains
an intentionally malformed `SELECT FROM POKEMON` statement, so the JDBC driver reports a syntax error
when this endpoint is invoked.

```shell
curl -i http://localhost:8080/pokemon/failure/invalid-sql
```

This invokes:

```java
pokemonRepository.listWithInvalidSqlSyntax()
```

The expected result is an HTTP `500 Internal Server Error`. This demonstrates that Helidon Data JDBC generates
the repository call and resource handling correctly, but SQL syntax validation is delegated to the database at
execution time.
