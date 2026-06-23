Helidon Data Imperative JDBC Examples
----

This directory contains imperative Helidon Data JDBC examples. Both examples create a `JdbcClient`
from a configured JDBC `DataSource` and use it directly from application code. They do not use
repository interfaces, declarative endpoints, or generated repository implementations.

## Examples

- `pokemon` - a basic JDBC client application using explicit SQL, statement options, positional
  binding, generated keys, and manual grouping of a simple joined result.
- `mappers` - a complex result mapping application using explicit row mappers and manual reducers
  for joined contact, phone, and tag rows.

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
