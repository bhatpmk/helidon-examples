# Helidon Data JDBC imperative row-traversal example

This example uses the imperative `JdbcClient` API to demonstrate the same two provider-owned traversal terminals as
the declarative `streaming` example. It uses an in-memory H2 database and a named JDBC persistence unit whose
`init.sql` script creates sample orders.

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

`OrderService` uses one static SQL statement and one row mapper for both methods. Application code creates a typed
request that carries the callback and any invocation-specific statement settings:

```java
JdbcStatementOptions options = JdbcStatementOptions.builder()
        .fetchSize(100)
        .queryTimeout(Duration.ofSeconds(30))
        .build();

JdbcResultRequest.VisitAll<OrderRow> request = JdbcResultRequest
        .visitAll(action)
        .withOptions(options);

jdbcClient.create(SQL)
        .bind(1, minimumId)
        .map(MAPPER)
        .visitAll(request);

JdbcResultRequest.VisitWhile<OrderRow> request = JdbcResultRequest
        .visitWhile(action)
        .withOptions(options);

boolean exhausted = jdbcClient.create(SQL)
        .bind(1, minimumId)
        .map(MAPPER)
        .visitWhile(request);
```

`visitAll` invokes the consumer for every mapped row. `visitWhile` stops when the predicate returns `false` and reports
whether normal exhaustion occurred. Neither operation materializes the complete result. The provider closes the result
set, statement, and non-transactional connection before each method returns. Application code never receives a JDBC
resource or closeable cursor.
