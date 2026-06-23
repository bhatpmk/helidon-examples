Helidon Data Imperative JDBC Mapping Example
----

This example demonstrates complex result mapping with the Helidon Data JDBC imperative API in an application. 
It complements the simpler `../pokemon` example. The Pokemon example focuses
on basic CRUD-style SQL and generated keys; this example focuses on joined rows and explicit mapping
patterns customers commonly need.

The example uses a contacts schema:

- `CONTACT`
- `PHONE`
- `TAG`

The application creates a `JdbcClient` from a configured Hikari `DataSource` and uses it directly.
There are no repository interfaces and no generated mappers or reducers. The service code contains
the row mappers and the reducer that assembles `Contact` aggregates with `Phone` and `Tag` children.

## Start the Database

To run the application, start a MySQL database:

```shell
docker run --name mysql-contacts \
       -p 3306:3306 \
       -e MYSQL_DATABASE='contacts' \
       -e MYSQL_RANDOM_ROOT_PASSWORD='yes' \
       -e MYSQL_USER='user' \
       -e MYSQL_PASSWORD='changeit' \
       -d mysql
```

The application runs `src/main/resources/init.sql` when it starts. This is intended for examples,
tests, and simple bootstrap data. It is not a database migration facility; use a migration tool for
production schema evolution.

## Build and Run

1. Build the application:

```shell
mvn package
```

2. Run the application:

```shell
java -jar target/helidon-examples-imperative-data-jdbc-mappers.jar
```

## Test Example

The application provides `http://localhost:8080/contacts` endpoints.

### Dotted Label Mapping

```shell
curl http://localhost:8080/contacts/automatic
```

The SQL aliases use dotted labels:

```sql
c.ID    AS "id",
c.NAME  AS "name",
p.ID    AS "phones.id",
p.TYPE  AS "phones.type",
p.PHONE AS "phones.phone",
t.ID    AS "phones.tags.id",
t.NAME  AS "phones.tags.name"
```

The imperative service maps those row labels explicitly and then groups rows into nested contact
aggregates.

### Explicit Alias Mapping

```shell
curl http://localhost:8080/contacts/explicit
```

This query uses database-oriented aliases such as `contact_key`, `phone_key`, and `tag_key`. The
imperative service maps those aliases to the same reducer row shape and uses the key columns to
deduplicate contacts, phones, and tags.

### Contact Card Mapping

```shell
curl http://localhost:8080/contacts/cards
```

This query returns one summary row per contact using aliases such as `contact_display_name`,
`phone_count`, and `tag_count`. The service maps each row directly to a `ContactCard` projection.
