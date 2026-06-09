Helidon Data SE Declarative JDBC Example
----

This example demonstrates Helidon Data JDBC for an SE declarative application.
It uses explicit SQL repository methods and does not depend on Jakarta Persistence or EclipseLink.

The example uses two repository interfaces:

- `PokemonRepository`
- `TypeRepository`

> **NOTE:** Repository methods must use `@Data.Query`; query-by-method-name and JPA entity mapping
> are intentionally not used by this example.

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

## Build and Run

1. Build the application using Maven:

```shell
mvn package
```

2. Run the application:

```shell
java -jar target/helidon-examples-declarative-data-jdbc.jar
```

## Test Example

The application provides `http://localhost:8080/pokemon` endpoint.

The following commands map each `PokemonRepository` method to the HTTP endpoint that invokes it.
`TypeRepository.getByName(String name)` is used internally by the insert flow to resolve the supplied
type name to a `TYPE.ID` value. The insert flow is annotated with `@Tx.Required`, so the type lookup,
insert, and final lookup run in one resource-local JDBC transaction.

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

### PokemonRepository.insert(String name, int typeId)

Inserts a pokemon row. The HTTP endpoint delegates to a `@Tx.Required` method that first calls
`typeRepository.getByName("Fire")` to resolve the type id, then calls
`pokemonRepository.insert("Charmander", type.id())`.

```shell
curl -i -X POST -H 'Content-type: application/json' -d '{"name":"Charmander","type":"Fire"}' http://localhost:8080/pokemon
```

This invokes:

```java
@Tx.Required
TypeRow type = typeRepository.getByName("Fire");
pokemonRepository.insert("Charmander", type.id());
pokemonRepository.getByName("Charmander");
```

### PokemonRepository.getByName(String name)

Reads the inserted row back after the insert operation so the HTTP response can include the
database-generated id.

```shell
curl http://localhost:8080/pokemon/get/Charmander
```

This direct curl uses `findByName`, because the sample exposes optional lookup as the public GET API.
The `getByName` repository method itself is exercised by the POST command above.

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
