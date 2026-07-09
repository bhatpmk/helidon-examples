Helidon Data Declarative JDBC Mapping Example
----

This example demonstrates the mapping supported by the current Helidon Data JDBC provider. A repository declares SQL
with `@Data.Query`, and build-time code generation creates direct row-mapping calls for scalar values, Java records, and
mutable beans. Runtime reflection is not used.

The example uses only the current mapping annotations. `@Data.BeanMapping` declares generated mutable-bean mapping,
while `@Data.RowMapper` selects
an explicitly authored mapper, and repeated bean-mapper declarations describe a joined object graph. Older annotations
such as `@Data.Mapper`, `@Data.Map`, `@Data.Key`, `@Data.MapWith`, and `@Data.ReduceWith` are not supported.

The repository demonstrates nine supported mapping forms:

- a list of `Contact` records;
- an optional `Contact` record;
- a list of scalar contact names;
- an explicitly selected `ContactNameMapper`;
- flat `ContactDetail` records from a three-table left join;
- aggregate `ContactCard` records;
- an identity-defined mutable contact, phone, and tag graph;
- an application-reduced immutable graph with composite phone identity;
- an explicit application reducer that removes duplicate contact rows.

SQL column labels match record component names. For example, `AS contactId` maps to the `contactId` component of
`ContactDetail`. The generator uses these names to select the required columns and invoke the record constructor.

## Start the Database

Start a MySQL database for the example:

```shell
docker run --name mysql-contacts \
       -p 3306:3306 \
       -e MYSQL_DATABASE='contacts' \
       -e MYSQL_RANDOM_ROOT_PASSWORD='yes' \
       -e MYSQL_USER='user' \
       -e MYSQL_PASSWORD='changeit' \
       -d mysql
```

The JDBC persistence unit runs `src/main/resources/init.sql` when the application starts. The script recreates the
example tables and data. It is example bootstrap logic, not a production database migration mechanism.

## Build and Run

```shell
mvn package
java -jar target/helidon-examples-declarative-data-jdbc-mappers.jar
```

## Generated Record Mapping

List all contacts:

```shell
curl http://localhost:8080/contacts/all
```

Find an optional contact. An unknown identifier returns `404 Not Found`:

```shell
curl http://localhost:8080/contacts/get/1
```

## Generated Scalar Mapping

Return only contact names. The generated mapper reads the first selected column as `String`:

```shell
curl http://localhost:8080/contacts/names
```

## Explicit Row Mapper

`mappedContact` selects `ContactNameMapper` with `@Data.RowMapper`. The generated repository constructs that mapper
once and passes it to the public `JdbcClient` API.

```shell
curl http://localhost:8080/contacts/mapped/1
```

## Flat Join Mapping

Return one `ContactDetail` per joined result row:

```shell
curl http://localhost:8080/contacts/details
```

The result deliberately remains flat. Repeated contact and phone values demonstrate the row shape returned by SQL.

## Graph Reduction

The graph endpoint reduces the same relationship into contacts with ordered, deduplicated phone and tag collections:

```shell
curl http://localhost:8080/contacts/graphs
```

The repository declares `@Data.BeanMapping` for the root and each collection property path. Every declaration supplies a local
identity property. The generated reducer uses the contact, phone, and tag identifiers to avoid duplicate objects. A
null child identifier from an outer join does not create a child object.

## Application Row Reducer

The `/contacts/immutable-graphs` endpoint selects `ImmutableContactGraphReducer` with `@Data.RowReducer`. It consumes
the same contact, phone, and tag relationship as the generated reducer, but it deliberately uses behavior outside the
generated graph contract:

- `ImmutableContactGraph`, `ImmutablePhoneGraph`, and `ImmutableTagGraph` are records;
- a phone is identified within its contact by the composite `(type, phone number)` key;
- mutable maps exist only inside one reducer invocation;
- `finish()` creates immutable roots and nested lists;
- duplicate rows, first-seen ordering, null outer-join children, and inconsistent projections are controlled by the
  application reducer.

```shell
curl http://localhost:8080/contacts/immutable-graphs
```

The generated repository constructs a fresh reducer and calls only the public client terminal:

```java
return jdbcClient.create(SQL_LIST_IMMUTABLE_GRAPHS)
        .reduce(new ImmutableContactGraphReducer());
```

The reducer receives callback-scoped `JdbcClient.Row` values. It never receives or retains a JDBC `ResultSet`,
statement, or connection.

### Minimal Application Row Reducer

The `/contacts/custom-reducer` endpoint uses `@Data.RowReducer(ContactRowReducer.class)`. Its SQL repeats every
contact with `UNION ALL`, and the application reducer keeps the first row for each contact identifier in SQL order.
This demonstrates application-controlled duplicate handling without exposing a JDBC `ResultSet`.

```shell
curl http://localhost:8080/contacts/custom-reducer
```

## Aggregate Record Mapping

Return one aggregate `ContactCard` per contact:

```shell
curl http://localhost:8080/contacts/cards
```

The SQL labels `id`, `displayName`, `firstPhone`, `phoneCount`, and `tagCount` match the record components exactly.
