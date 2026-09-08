Helidon Data Declarative JDBC Examples
----

This directory contains declarative Helidon Data JDBC examples. The examples use explicit SQL repository
methods, JDBC persistence units, generated repository implementations, and no Jakarta Persistence runtime.

## Examples

- `pokemon` - a basic JDBC repository application using explicit SQL, named parameters, generated keys,
  local JDBC transactions, and database-specific paging SQL.
- `mappers` - a result mapping application using scalar and record mapping, an explicit row mapper, generated mutable
  graph reduction, and application reducers including an immutable graph with composite child identity.
- `streaming` - callback-based row streaming with provider-owned JDBC resource closure using an embedded H2 database.

The Petclinic and stored-procedure examples are not part of this checkout. Stored-procedure support remains deferred
in the Helidon Data JDBC design, so no callable tests are enabled here.

## Build

Build all examples from this directory:

```shell
mvn package
```

Build one example from this directory:

```shell
mvn -pl pokemon package
mvn -pl mappers package
mvn -pl streaming package
```

Each child example has its own README with database setup, run commands, and curl commands.
