# Helidon Data JDBC imperative streaming example

This example uses the imperative `JdbcClient` API to demonstrate the same three provider-owned streaming terminals as the declarative streaming example. It uses an in-memory H2 database and a named JDBC persistence unit whose `init.sql` script creates sample orders.

## Build and run

```shell
mvn package
java -jar target/helidon-examples-imperative-data-jdbc-streaming.jar
```

The routes are available below `http://localhost:8080/orders`:

```shell
curl http://localhost:8080/orders/summary/3
curl http://localhost:8080/orders/for-each/3
curl http://localhost:8080/orders/for-each-while/3/2
```

`OrderService` uses one static SQL statement and one row mapper for all three methods:

```java
jdbcClient.create(SQL)
        .options(options)
        .bind(1, minimumId)
        .map(MAPPER)
        .withRows(action);

jdbcClient.create(SQL)
        .options(options)
        .bind(1, minimumId)
        .map(MAPPER)
        .forEach(action);

boolean exhausted = jdbcClient.create(SQL)
        .options(options)
        .bind(1, minimumId)
        .map(MAPPER)
        .forEachWhile(action);
```

`withRows` gives the callback a single-use iterable for ordinary loop control. `forEach` pushes every row and is the smallest choice when all rows must be consumed. `forEachWhile` stops when the predicate returns `false` and reports whether normal exhaustion occurred. The provider closes the result set, statement, and non-transactional connection before each method returns; application code never receives a JDBC resource or a closeable cursor.
