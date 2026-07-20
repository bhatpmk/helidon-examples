Helidon Declarative JDBC Stored Procedures Example
----

This example models a fulfillment service that uses stored procedures for important order transitions. It demonstrates:

- declarative UPDATE methods that create and replace MySQL procedures and functions at application startup;
- a declarative procedure call with three IN parameters, one INOUT parameter, and two scalar OUT parameters;
- a declarative MySQL function call using detached scalar return mode;
- a callback-scoped result consumer that reads all outputs while the callable statement is open;
- a MySQL procedure that returns order lines as a direct result set;
- explicit row mapping for direct procedure results;
- statement options for a large result (`fetchSize` and `queryTimeout`); and
- provider-owned JDBC resources: the application receives detached records, never a `ResultSet` or cursor.

The repository uses `@Jdbc.Execution(Jdbc.ExecutionType.CALL)` on `@Jdbc.Statement`.

## Routine creation is UPDATE execution

Routine definition is DDL, not callable execution. `OrderRoutineRepository` creates each MySQL procedure and function
with `@Jdbc.Statement` and `@Jdbc.Execution(Jdbc.ExecutionType.UPDATE)`. It also uses separate UPDATE methods for the
idempotent `DROP ... IF EXISTS` statements. JDBC sends each `CREATE PROCEDURE` or `CREATE FUNCTION` declaration as one
complete statement; MySQL's `DELIMITER` directive is not SQL and is never sent through JDBC.

`OrderRoutineInstaller` runs these UPDATE methods at startup, after the JDBC persistence unit has created the sample
tables and rows. The order-facing repository then invokes those routines with `@Jdbc.Execution(CALL)`.

## Why the cursor example uses a direct result set

MySQL stored procedures do not provide a portable `Types.REF_CURSOR` OUT parameter. The MySQL idiom is to execute a
`SELECT` inside the procedure. The MySQL JDBC driver exposes that result as a direct result set from
`CallableStatement.execute()`. The repository consumes it through `call.results().visit(...)`, which is the
provider-owned callback equivalent of traversing a cursor.

On a database and driver that support `REF_CURSOR`, the repository declaration changes to an OUT declaration:

```java
@Jdbc.Statement("{call GET_ORDER_LINES_CURSOR(:orderId, :lines)}")
@Jdbc.Execution(Jdbc.ExecutionType.CALL)
@Jdbc.OutParameter(name = "lines", jdbcType = Types.REF_CURSOR)
OrderLines orderLines(
        JdbcResultRequest.CallWith<OrderLines> request,
        @Jdbc.InParameter(name = "orderId") long orderId);
```

The callback would consume it in the same resource-safe scope:

```java
return repository.orderLines(
        JdbcResultRequest.call(call -> {
            List<OrderLine> lines = call.outputs()
                    .cursor("lines")
                    .map(orderLineMapper)
                    .list();
            return new OrderLines(orderId, lines);
        }),
        orderId);
```

Do not use this `REF_CURSOR` declaration against MySQL. Select the declaration and procedure syntax supported by the
production database and JDBC driver.

## Declarative repository shape

The reservation method uses named markers in the source declaration. The annotation processor resolves the markers to
physical JDBC positions and generates an immutable `JdbcCall` layout. The runtime binds IN and INOUT values using the
normal input path, registers output positions, and invokes the callback while the call is still open.

```java
@Jdbc.Statement("{call RESERVE_ORDER(:orderId, :customerId, :requestedBy, :attempts, :status, :reservedAmount)}")
@Jdbc.Execution(Jdbc.ExecutionType.CALL)
@Jdbc.OutParameter(name = "status", jdbcType = Types.VARCHAR, javaType = String.class)
@Jdbc.OutParameter(name = "reservedAmount", jdbcType = Types.DECIMAL, javaType = BigDecimal.class)
ReservationResult reserveOrder(
        JdbcResultRequest.CallWith<ReservationResult> request,
        @Jdbc.InParameter(name = "orderId") long orderId,
        @Jdbc.InParameter(name = "customerId") long customerId,
        @Jdbc.InParameter(name = "requestedBy") String requestedBy,
        @Jdbc.InOutParameter(name = "attempts", jdbcType = Types.INTEGER) int attempts);
```

The leading `CallWith` request is a control value, not a SQL parameter. It creates the detached return value before the
provider closes the statement:

```java
return orders.reserveOrder(
        JdbcResultRequest.call(call -> {
            // Direct result channels must be consumed before OUT values.
            call.results().discard();
            int updatedAttempts = call.outputs().required("attempts", Integer.class);
            String status = call.outputs().required("status", String.class);
            BigDecimal amount = call.outputs().required("reservedAmount", BigDecimal.class);
            return new ReservationResult(orderId, updatedAttempts, status, amount);
        }),
        orderId,
        request.customerId(),
        request.requestedBy(),
        request.attempts());
```

The direct-result method maps rows inside the callback:

```java
return orders.orderLines(
        JdbcResultRequest.call(call -> {
            List<OrderLine> lines = new ArrayList<>();
            call.results().visit(new JdbcClient.CallResultVisitor() {
                @Override
                public void rows(int resultSetIndex, JdbcClient.CallRows rows) {
                    rows.map(orderLineMapper)
                            .visitAll(JdbcResultRequest.visitAll(lines::add));
                }
            });
            return new OrderLines(orderId, List.copyOf(lines));
        }).withOptions(JdbcStatementOptions.builder()
                .fetchSize(100)
                .queryTimeout(Duration.ofSeconds(10))
                .build()),
        orderId,
        includeBackordered);
```

For a large result, replace `list`/`List.copyOf` with callback-scoped row traversal and retain only the required
aggregate or side effects. The callback must finish before the endpoint returns, so the provider can close the result
set, statement, and logical connection lease deterministically.

## Function return

`CALCULATE_RESERVATION_FEE` is a pure MySQL function that applies the standard or priority fee rate to a supplied
order total. The endpoint obtains that total through a normal declarative `QUERY` before it invokes the function. The
function is correctly declared `DETERMINISTIC NO SQL`, but that does **not** by itself grant permission to create it.
When MySQL binary logging is enabled, the creator needs `SUPER` (or modern MySQL's `SET_USER_ID`) in addition to
`CREATE ROUTINE`, unless a database administrator enables `log_bin_trust_function_creators=1`. A function return is
not an ordinary OUT parameter: JDBC reserves physical position `1` for it. The declaration therefore uses
`@Jdbc.ReturnParameter`, and the return marker appears before the function inputs:

```java
@Jdbc.Statement("{:fee = call CALCULATE_RESERVATION_FEE(:orderTotal, :priority)}")
@Jdbc.Execution(Jdbc.ExecutionType.CALL)
@Jdbc.ReturnParameter(name = "fee", jdbcType = Types.DECIMAL, javaType = BigDecimal.class)
BigDecimal reservationFee(
        @Jdbc.InParameter(name = "orderTotal") BigDecimal orderTotal,
        @Jdbc.InParameter(name = "priority") boolean priority);
```

The generated repository invokes `callForOutputs`, reads the declared logical output, closes the JDBC resources, and
returns the detached value. Application code calls the method normally:

```java
BigDecimal fee = repository.reservationFee(orderTotal, priority);
```

## Database setup

The application initializes tables and sample data through the Helidon JDBC `init-script`. After that initialization,
the startup installer creates the procedures and function through the declarative UPDATE repository.

Start MySQL 8.4 with the supplied helper:

```shell
./bin/start-mysql.sh
```

The helper starts a disposable MySQL container with binary logging disabled. It uses a generated root credential and
waits for the configured non-root application account to authenticate successfully; the helper never logs in as root.
Disabling binary logging avoids MySQL's privileged stored-function-creation rule for this isolated example. It is not a
production database policy. For another MySQL server, a DBA must either provision the function through an authorized
migration account or explicitly approve `log_bin_trust_function_creators=1`; no JDBC annotation or function
characteristic can bypass this server-side control.

If the named container was created by an earlier version of the helper, recreate its disposable sample data so the
new server option takes effect:

```shell
docker rm -f helidon-stored-procedures-mysql
./bin/start-mysql.sh
```

Build the application from this directory:

```shell
mvn package
```

Run the application:

```shell
java -jar target/helidon-examples-declarative-data-jdbc-stored-procedures.jar
```

The application creates the tables, sample data, procedures, and function when it starts when the MySQL server permits
function creation as described above. No separate database-client command is required for the supplied helper.

The sample init script intentionally recreates its tables on application startup. This is convenient for a repeatable
example and is not a production migration strategy.

## Invoke the application

Run all demonstrations with the supplied script:

```shell
./bin/invoke.sh
```

Or invoke each endpoint directly.

Reserve order `1001`. This supplies two path/body IN values plus the `requestedBy` IN value, supplies `attempts` as
INOUT, and receives `attempts`, `status`, and `reservedAmount` as outputs:

```shell
curl -X POST http://localhost:8080/orders/1001/reserve \
     -H 'Content-Type: application/json' \
     -d '{"customerId":501,"requestedBy":"fulfillment-service","attempts":0}'
```

Expected result on the first invocation is similar to:

```json
{
  "orderId": 1001,
  "attempts": 1,
  "status": "RESERVED",
  "reservedAmount": 149.97
}
```

Calculate the priority reservation fee through the function-return slot:

```shell
curl http://localhost:8080/orders/1001/reservation-fee/true
```

The sample order total is `149.97`; with a 1.5% priority rate, the JSON response is `2.25`.

Call the direct-result procedure and exclude backordered lines:

```shell
curl http://localhost:8080/orders/1001/lines/false
```

Include backordered lines for order `1002`:

```shell
curl http://localhost:8080/orders/1002/lines/true
```

The endpoint uses a provider-owned callback to map each direct result row. The application never receives a JDBC
`ResultSet`, `CallableStatement`, stream, or closeable cursor.

## Important portability notes

- MySQL direct result sets are not the same feature as `Types.REF_CURSOR`; use the driver-specific procedure contract.
- On a binary-logged MySQL server, stored-function DDL is privileged even when the function is `DETERMINISTIC NO SQL`.
  Provision such functions through an approved migration account, or make the server-level trust decision explicitly.
- Some drivers require direct result sets to be consumed before scalar OUT values are read. The sample always discards
  or consumes direct results first.
- A `REF_CURSOR` output must be consumed or discarded before another cursor or scalar output is accessed.
- `fetchSize` is a JDBC driver hint, not a guarantee that the database or driver will avoid buffering.
- A callback must return a detached value. Retaining `CallScope`, `CallRows`, or row views after the
  callback returns is invalid.
