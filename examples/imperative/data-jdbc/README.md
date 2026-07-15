Helidon Data Imperative JDBC Examples
----

This directory contains imperative Helidon Data JDBC examples. All examples create a `JdbcClient`
from a configured JDBC `DataSource` and use it directly from application code. They do not use
repository interfaces, declarative endpoints, or generated repository implementations.

## Examples

- `pokemon` - explicit SQL, statement options, positional binding, paging, updates, generated keys, and
  local JDBC transactions.
- `mappers` - scalar, record, bean, explicit row-mapper, flat-join, generated-style graph-reducer, and
  application-specific reducer examples.
- `streaming` - provider-owned `visitAll` and `visitWhile` row traversal over an H2 datasource.

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
