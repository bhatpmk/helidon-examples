Helidon Data Declarative JDBC Streaming Example
----

This example demonstrates all three provider-owned JDBC traversal terminals from a declarative repository. It uses an
embedded H2 database, so no external database setup is required.

The repository declares a synchronous callback rather than returning a plain `Stream<T>`:

```java
@Data.Query("""
        SELECT ID AS id, CUSTOMER AS customer, REGION AS region, AMOUNT AS amount
        FROM SALES_ORDER
        WHERE ID >= :minimumId
        ORDER BY ID
        """)
void withRows(long minimumId, Consumer<Iterable<OrderRow>> action);

@Data.Query(SELECT_ORDERS)
void visitOrders(long minimumId, Consumer<OrderRow> action);

@Data.Query(SELECT_ORDERS)
boolean visitOrdersUntil(long minimumId, Predicate<OrderRow> action);
```

`SELECT_ORDERS` above abbreviates the same SQL shown on `withRows`; the source repeats the annotation value because an
example repository should not expose SQL as a public interface field.

The callback type and repository return type select the terminal operation at compile time:

| Repository callback | Repository return | Generated terminal | Use |
| --- | --- | --- | --- |
| `Consumer<Iterable<OrderRow>>` | `void` | `withRows(action)` | Callback-scoped pull traversal; the callback may use a loop and `break` |
| `Consumer<OrderRow>` | `void` | `forEach(action)` | Push every mapped row to the callback |
| `Predicate<OrderRow>` | `boolean` | `forEachWhile(action)` | Push rows until the predicate returns `false` |

The generated repository maps each current row and calls the same public JDBC client API available to imperative code:

```java
jdbcClient.create(SQL_WITH_ROWS)
        .bind(1, minimumId)
        .map(MAPPER_WITH_ROWS)
        .withRows(action);

jdbcClient.create(SQL_FOR_EACH)
        .bind(1, minimumId)
        .map(MAPPER_FOR_EACH)
        .forEach(action);

return jdbcClient.create(SQL_FOR_EACH_WHILE)
        .bind(1, minimumId)
        .map(MAPPER_FOR_EACH_WHILE)
        .forEachWhile(action);
```

`withRows` acquires the logical connection, prepares and executes the statement, and invokes the callback while the
result set is open. It closes the result set, statement, and logical connection handle before returning, including when
the callback uses `break`, returns early, or throws. Its iterable is single-use and thread-confined and must not be
retained. `forEach` and `forEachWhile` use the same internal cursor and cleanup path. `forEachWhile` returns `false`
immediately when its predicate returns `false`, and returns `true` only after normal result-set exhaustion.

`OrderEndpoint` uses descriptive repository methods (`visitOrders` and `visitOrdersUntil`) that select the `forEach`
and `forEachWhile` terminals from their callback signatures. It consumes rows one at a time to calculate a bounded summary.
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

Invoke the `forEach` repository method with the same result range:

```shell
curl http://localhost:8080/orders/for-each/4
```

Invoke `forEachWhile` and stop after three rows:

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
