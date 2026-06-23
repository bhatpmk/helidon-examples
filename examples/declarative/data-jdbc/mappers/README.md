Helidon Data Declarative JDBC Mapping Example
----

This example demonstrates complex result mapping using Helidon Data JDBC declarative API.
It complements the simpler `../pokemon` example. The Pokemon example focuses on basic explicit SQL
repositories, generated keys, transactions, and a simple automatic reducer. This example focuses on result
mapping patterns customers commonly need when they join several tables.

The example uses a contacts schema:

- `CONTACT`
- `PHONE`
- `TAG`

The application exposes one repository interface, `ContactRepository`, with three query styles:

- Automatic relationship reducer generation from dotted SQL labels such as `"phones.id"` and `"phones.tags.id"`.
- Explicit relationship reducer generation with `@Data.ReduceWith` and a declarative `@Data.Mapper` contract.
- Declarative row mapping with `@Data.MapWith` and a mapper contract for a summary projection.

The application does not depend on Jakarta Persistence or EclipseLink. Repository methods use explicit SQL
through `@Data.Query`, and Helidon generates the repository implementation, binders, mappers, and reducers at
build time.

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

The application runs `src/main/resources/init.sql` through the JDBC persistence unit `init-script`
setting when it starts. This is intended for examples, tests, and simple bootstrap data. It is not a
database migration facility; use a migration tool for production schema evolution.

## Build and Run

1. Build the application:

```shell
mvn package
```

2. Run the application:

```shell
java -jar target/helidon-examples-declarative-data-jdbc-mappers.jar
```

## Test Example

The application provides `http://localhost:8080/contacts` endpoints.

### Automatic Relationship Reducer

This endpoint invokes `ContactRepository.listWithAutomaticReducer()`.

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

The JDBC code generator uses those labels and the Java model to assemble each `Contact` with its `Phone`
children and each phone's `Tag` children.

### Explicit Relationship Reducer

This endpoint invokes `ContactRepository.listWithExplicitReducer()`.

```shell
curl http://localhost:8080/contacts/explicit
```

The SQL aliases do not use Java property paths. Instead, `ContactGraphMapping` declares the mapping:

```java
@Data.Mapper(target = Contact.class)
@Data.Map(source = "contact_key", target = "id")
@Data.Map(source = "contact_name", target = "name")
@Data.Map(source = "phone_key", target = "phones.id")
@Data.Map(source = "phone_kind", target = "phones.type")
@Data.Map(source = "phone_number", target = "phones.phone")
@Data.Map(source = "tag_key", target = "phones.tags.id")
@Data.Map(source = "tag_name", target = "phones.tags.name")
@Data.Key(source = "contact_key")
@Data.Key(source = "phone_key", target = "phones")
@Data.Key(source = "tag_key", target = "phones.tags")
interface ContactGraphMapping {
}
```

This is the preferred style when SQL aliases are database-oriented, when identities need to be explicit, or
when automatic dotted-label mapping is not expressive enough.

### Declarative Mapper Contract

This endpoint invokes `ContactRepository.listCards()`.

```shell
curl http://localhost:8080/contacts/cards
```

The query returns one summary row per contact using aliases such as `contact_display_name`, `phone_count`,
and `tag_count`. `ContactCardMapping` maps those aliases to the `ContactCard` record components. The
application declares mapping intent, and Helidon generates the executable mapper.
