Helidon Data Declarative JDBC Streaming Example
----

This example demonstrates both provider-owned JDBC traversal terminals from a declarative repository. It uses an
embedded H2 database, so no external database setup is required.

The repository accepts a typed request as its first parameter rather than returning a JDBC-backed `Stream<T>`:

```java
@Jdbc.Statement("""
        SELECT ID AS id, CUSTOMER AS customer, REGION AS region, AMOUNT AS amount
        FROM SALES_ORDER
        WHERE ID >= :minimumId
        ORDER BY ID
        """)
void visitOrders(JdbcResultRequest.VisitAll<OrderRow> request, long minimumId);

@Jdbc.Statement(SELECT_ORDERS)
boolean visitOrdersUntil(JdbcResultRequest.VisitWhile<OrderRow> request, long minimumId);
```

`SELECT_ORDERS` above abbreviates the same SQL shown on `visitOrders`; the source repeats the annotation value because an
example repository should not expose SQL as a public interface field.

The request type and repository return type select the terminal operation at compile time. The request must be the first
parameter and is never bound to SQL:

| Leading request | Repository return | Generated terminal | Use |
| --- | --- | --- | --- |
| `JdbcResultRequest.VisitAll<OrderRow>` | `void` | `visitAll(request)` | Visit every mapped row with the callback |
| `JdbcResultRequest.VisitWhile<OrderRow>` | primitive `boolean` | `visitWhile(request)` | Push rows until the predicate returns `false` |

The generated repository maps each current row and calls the same public JDBC client API available to imperative code:

```java
jdbcClient.create(SQL_FOR_EACH)
        .bind(1, minimumId)
        .map(MAPPER_FOR_EACH)
        .visitAll(request);

return jdbcClient.create(SQL_FOR_EACH_WHILE)
        .bind(1, minimumId)
        .map(MAPPER_FOR_EACH_WHILE)
        .visitWhile(request);
```

`visitAll` and `visitWhile` use the same internal cursor and cleanup path. `visitWhile` returns `false` immediately
when its predicate returns `false`, and returns `true` only after normal result-set exhaustion. Both terminals close the
result set, statement, and logical connection handle before returning. The provider also closes them after callback,
mapper, or JDBC failure. No JDBC resource is exposed to application code.

The application can create a request directly when driver defaults are suitable:

```java
JdbcResultRequest.VisitAll<OrderRow> request =
        JdbcResultRequest.visitAll(order -> summary.accept(order));

JdbcResultRequest.VisitWhile<OrderRow> limited =
        JdbcResultRequest.visitWhile(order -> {
            summary.accept(order);
            return summary.orderCount() < rowLimit;
        });
```

The request factories create immutable callback requests. Invocation-specific statement settings can be added without
changing the repository signature:

```java
JdbcResultRequest.VisitAll<OrderRow> request = JdbcResultRequest
        .visitAll(summary::accept)
        .withOptions(JdbcStatementOptions.builder().fetchSize(100).build());
```

`OrderEndpoint` uses descriptive repository methods (`visitOrders` and `visitOrdersUntil`) that select the `visitAll`
and `visitWhile` terminals from their request types. It consumes rows one at a time to calculate a bounded summary.
It does not materialize all matching orders
in a list, and the endpoint receives only the completed `OrderSummary` after JDBC resources have closed.

## Build and Run

From this directory:

```shell
mvn package
java -jar target/helidon-examples-declarative-data-jdbc-streaming.jar
```

## Invoke the Streaming Repository

Request a summary beginning with order identifier 4:

```shell
curl http://localhost:8080/orders/summary/4
```

The response shows that nine rows were consumed while retaining only aggregate state:

```json
{
  "minimumId": 4,
  "orderCount": 9,
  "firstOrderId": 4,
  "lastOrderId": 12,
  "totalAmount": 8842.24,
  "ordersByRegion": {
    "Americas": 4,
    "Europe": 3,
    "Asia Pacific": 2
  },
  "exhausted": true
}
```

Invoke the `visitAll` repository method with the same result range:

```shell
curl http://localhost:8080/orders/for-each/4
```

Invoke `visitWhile` and stop after three rows:

```shell
curl http://localhost:8080/orders/for-each-while/4/3
```

The last response contains rows 4 through 6 and reports `"exhausted": false` because the predicate, rather than normal
result-set exhaustion, ended traversal. Use a limit larger than the result to see `"exhausted": true`:

```shell
curl http://localhost:8080/orders/for-each-while/10/10
```

For bounded application queries and ordinary REST responses, pagination or materialized terminals remain preferable.
Callback streaming is intended for sequential processing where retaining the complete result would be unnecessary.
