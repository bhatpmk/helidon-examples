Helidon Data Declarative JDBC Examples
----

This directory contains declarative Helidon Data JDBC examples. Both examples use explicit SQL repository
methods, JDBC persistence units, generated repository implementations, and no Jakarta Persistence runtime.

## Examples

- `pokemon` - a basic JDBC repository application using explicit SQL, named parameters, generated keys,
  local JDBC transactions, and a simple automatic relationship reducer.
- `mappers` - a complex result mapping application using automatic relationship reducers, explicit
  reducer contracts, and declarative mapper contracts.

## Build

Build both examples from this directory:

```shell
mvn package
```

Build one example from this directory:

```shell
mvn -pl pokemon package
mvn -pl mappers package
```

Each child example has its own README with database setup, run commands, and curl commands.
