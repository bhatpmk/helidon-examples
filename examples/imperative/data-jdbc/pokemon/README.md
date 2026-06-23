Helidon Data Imperative JDBC Pokemon Example
----

This example demonstrates the Helidon Data JDBC imperative API in an application.
It uses the same pokemon schema and HTTP shape as the declarative JDBC pokemon example, but
it does not use repository interfaces, annotation processing, declarative endpoints, or
generated repository implementations.

For complex result mapping and manual relationship reducer examples, see the neighboring
`../mappers` example.

The application creates a `JdbcClient` from a configured Hikari `DataSource` and uses it directly:

- `query(...).list(...)` for multi-row queries
- `query(...).optional(...)` for optional lookup
- `query(...).one(...)` for required lookup
- `update(...).execute()` for DML
- `update(...).generatedKey(...)` for inserts that return generated keys

The `/pokemon/types` endpoint shows manual grouping of joined rows into a nested response. The
declarative sample generates this relationship reducer from dotted column labels; this imperative
sample keeps the grouping code in the application.

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

The application runs `src/main/resources/init.sql` when it starts. This is intended for examples,
tests, and simple bootstrap data. It is not a database migration facility; use a migration tool for
production schema evolution. The script drops and recreates the sample tables on startup.

## Build and Run

1. Build the application using Maven:

```shell
mvn package
```

2. Run the application:

```shell
java -jar target/helidon-examples-imperative-data-jdbc-pokemon.jar
```

## Test Example

The application provides `http://localhost:8080/pokemon` endpoint.

### List all pokemon

```shell
curl http://localhost:8080/pokemon/all
```

This invokes:

```java
jdbcClient.query(sql).fetchSize(32).list(rowMapper)
```

### List pokemon by type

```shell
curl http://localhost:8080/pokemon/type/Normal
```

This invokes a positional JDBC bind:

```java
jdbcClient.query(sql).bind(1, "Normal").list(rowMapper)
```

### Find pokemon by name

```shell
curl http://localhost:8080/pokemon/get/Meowth
```

This invokes:

```java
jdbcClient.query(sql).bind(1, "Meowth").optional(rowMapper)
```

### List types with pokemon

```shell
curl http://localhost:8080/pokemon/types
```

The query returns joined rows, and the application groups them into nested `TypeWithPokemon`
responses.

### Insert pokemon and read the generated key

```shell
curl -i -X POST -H 'Content-type: application/json' -d '{"name":"Charmander","type":"Fire"}' http://localhost:8080/pokemon
```

This invokes:

```java
jdbcClient.update("INSERT INTO POKEMON (NAME, TYPE_ID) VALUES (?, ?)")
        .bind(1, "Charmander")
        .bind(2, type.id())
        .generatedKey(row -> ((Number) row.get(1)).intValue(), "ID")
```

The sample schema sets `AUTO_INCREMENT=20`, so the first generated pokemon id is expected to be `20`
after startup initialization.

### Delete pokemon by id

```shell
curl -i -X DELETE http://localhost:8080/pokemon/20
```

This invokes:

```java
jdbcClient.update("DELETE FROM POKEMON WHERE ID = ?").bind(1, 20).execute()
```

### Invalid SQL failure

```shell
curl -i http://localhost:8080/pokemon/failure/invalid-sql
```

This endpoint demonstrates driver error reporting for malformed SQL supplied to the imperative API.
