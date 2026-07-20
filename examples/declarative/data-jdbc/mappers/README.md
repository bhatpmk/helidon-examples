Helidon Data Declarative JDBC Mapping Example
----

This example demonstrates the mapping supported by the current Helidon Data JDBC provider. A repository declares SQL
with `@Jdbc.Statement`, and build-time code generation creates direct row-mapping calls for scalar values and Java records.
Runtime reflection is not used.

The example uses only the current mapping annotations. `@Jdbc.IdentityReducer` selects generated reduction for a
joined record graph. `@Jdbc.RowMapper(SomeMapper.class)` selects an exact mapper service for one physical row, while
the marker form `@Jdbc.RowMapper()` selects a mapper service by its generic result type. `@Jdbc.RowReducer` selects an
application-authored reducer for several physical rows.

The repository demonstrates these supported mapping forms:

- unannotated list and optional `Contact` methods with an optional mapper service and generated fallback;
- a list of scalar contact names;
- two exact mapper services that produce different `ContactName` views, demonstrating explicit disambiguation;
- generic `RowMapper<ContactPhone>` service selection for a joined row and an application-formatted phone label;
- flat `ContactDetail` records from a three-table left join;
- aggregate `ContactCard` records;
- an identity-reduced contact, phone, and tag record graph;
- an application-reduced custom record graph with composite phone identity;
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

## Unannotated Record Mapping

List all contacts or find an optional contact. These unannotated methods receive an optional `RowMapper<Contact>` and
retain a generated record mapper as their fallback. No `RowMapper<Contact>` service is registered, so both methods use
the generated mapper.

```shell
curl http://localhost:8080/contacts/all
curl http://localhost:8080/contacts/get/1
```

## Generated Scalar Mapping

Return only contact names. The generated mapper reads the first selected column as `String`:

```shell
curl http://localhost:8080/contacts/names
```

## Row Mapper Services

`mappedContact` selects `ContactNameMapper` with `@Jdbc.RowMapper(ContactNameMapper.class)`. The SQL returns columns
labeled `id` and `name`, while the method returns `ContactName(contactNumber, displayName)`. The mapper explicitly
renames `id` to `contactNumber` and converts `name` to an uppercase `displayName`. It is a singleton service that the
generated repository receives through constructor injection and passes to the public `JdbcClient` API. Generated code
never constructs it directly.

```shell
curl http://localhost:8080/contacts/mapped/1
```

`mappedContactSummary` returns the same `ContactName` type but selects `ContactSummaryMapper`. Its aggregate SQL also
returns phone and tag counts, which the mapper incorporates into the display name. Both mapper classes are singleton
services implementing `RowMapper<ContactName>`. Naming the mapper class in each annotation makes the method-level
choice explicit and avoids an ambiguous generic service lookup.

```shell
curl http://localhost:8080/contacts/mapped-summary/1
```

`mappedPrimaryPhone` uses the marker form `@Jdbc.RowMapper()`. Its SQL joins contacts and phones. The generated
repository requires a `RowMapper<ContactPhone>` service instead of naming an implementation. `ContactPhoneMapper`
satisfies that generic contract and combines the `phoneType` and `phoneNumber` columns into one `phoneLabel` component.

```shell
curl http://localhost:8080/contacts/mapped-phone/1
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

The repository declares the Java record-component paths `id`, `phones.id`, and `phones.tags.id` in one
`@Jdbc.IdentityReducer` annotation. Removing the final component from an identity path identifies its record scope.
The SQL aliases use the same component paths, so the generator can map values to canonical record constructors. The
generated reducer uses the declared identities to avoid duplicate objects within their parent and preserves SQL
encounter order. A null child identity from an outer join does not create a child record.

```java
@Jdbc.IdentityReducer(identityPaths = {"id", "phones.id", "phones.tags.id"})
List<ContactGraph> listGraphs();
```

## Application Row Reducer

The `/contacts/custom-graphs` endpoint selects `CustomContactGraphReducer` with `@Jdbc.RowReducer`. It consumes
the same contact, phone, and tag relationship as the generated reducer, but it deliberately uses behavior outside the
generated graph contract:

- `CustomContactGraph`, `CustomPhoneGraph`, and `CustomTagGraph` are records;
- a phone is identified within its contact by the composite `(type, phone number)` key;
- mutable maps exist only inside one reducer invocation;
- `finish()` creates record roots and nested lists;
- duplicate rows, first-seen ordering, null outer-join children, and inconsistent projections are controlled by the
  application reducer.

```shell
curl http://localhost:8080/contacts/custom-graphs
```

The generated repository constructs a fresh reducer and calls only the public client terminal:

```java
return jdbcClient.create(SQL_LIST_CUSTOM_GRAPHS)
        .reduce(new CustomContactGraphReducer());
```

The reducer receives callback-scoped `JdbcClient.Row` values. It never receives or retains a JDBC `ResultSet`,
statement, or connection.

### Minimal Application Row Reducer

The `/contacts/custom-reducer` endpoint uses `@Jdbc.RowReducer(ContactRowReducer.class)`. Its SQL repeats every
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
