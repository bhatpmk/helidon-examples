Helidon Data Imperative JDBC Mapping Example
----

This application demonstrates the same mapping and reduction cases as the declarative `mappers` example, but every
operation is written directly against the public `JdbcClient` API. There are no repository interfaces or generated
repository classes.

The application demonstrates:

- scalar and flat contact record mapping;
- `Optional<T>` and `one()` cardinality;
- an explicit `JdbcClient.RowMapper<Contact>`;
- scalar `map(String.class)` mapping;
- one `ContactDetail` per physical left-join row;
- aggregate `ContactCard` records;
- result-set reduction into ordered contact/phone/tag graphs using scalar identities;
- a full application reducer that builds immutable graph records and uses composite `(phone type, phone number)`
  identity;
- a small root-only reducer that removes duplicates from a deliberately repeated query.

The imperative chain has the same shape used by generated declarative repositories:

```java
jdbcClient.create(SQL)
        .options(JdbcStatementOptions.builder().fetchSize(32).build())
        .bind(1, value)
        .map(MAPPER)
        .list();

jdbcClient.create(JOIN_SQL)
        .reduce(new ContactGraphReducer(false));
```

The mapper receives one callback-scoped `JdbcClient.Row`. The reducer receives every physical row and returns its
logical result from `finish()`. Neither API exposes a `ResultSet`, `Statement`, or `Connection` to the application.

## Start the database

```shell
docker run --name mysql-contacts \
       -p 3306:3306 \
       -e MYSQL_DATABASE='contacts' \
       -e MYSQL_RANDOM_ROOT_PASSWORD='yes' \
       -e MYSQL_USER='user' \
       -e MYSQL_PASSWORD='changeit' \
       -d mysql
```

The named `contacts` JDBC persistence unit runs `src/main/resources/init.sql` during provider startup.

## Build and run

```shell
mvn package
java -jar target/helidon-examples-imperative-data-jdbc-mappers.jar
```

## Endpoints

```shell
curl http://localhost:8080/contacts/all
curl http://localhost:8080/contacts/get/1
curl http://localhost:8080/contacts/mapped/1
curl http://localhost:8080/contacts/names
curl http://localhost:8080/contacts/details
curl http://localhost:8080/contacts/cards
curl http://localhost:8080/contacts/graphs
curl http://localhost:8080/contacts/immutable-graphs
curl http://localhost:8080/contacts/custom-reducer
```

`/contacts/graphs` uses the database phone identifier and tag identifier to reproduce the generated declarative graph
semantics. `/contacts/immutable-graphs` uses application-defined composite phone identity and immutable output records.
That second reducer is intentionally more flexible than generated V27 graph reduction. `/contacts/details` remains flat:
repeated contact and phone values are not deduplicated.
