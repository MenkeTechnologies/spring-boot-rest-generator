```
 ___  ____  ____  ____  _  _  ___    ____  _____  _____  ____
/ __)(  _ \(  _ \(_  _)( \( )/ __)  (  _ \(  _  )(  _  )(_  _)
\__ \ )___/ )   / _)(_  )  (( (_-.   ) _ < )(_)(  )(_)(   )(
(___/(__)  (_)\_)(____)(_)\_)\___/  (____/(_____)(_____)  (__)
 ____  ____  ___  ____     ___  ____  _  _  ____  ____    __   ____  _____  ____
(  _ \( ___)/ __)(_  _)   / __)( ___)( \( )( ___)(  _ \  /__\ (_  _)(  _  )(  _ \
 )   / )__) \__ \  )(    ( (_-. )__)  )  (  )__)  )   / /(__)\  )(   )(_)(  )   /
(_)\_)(____)(___/ (__)    \___/(____)(_(\_)(____)(_)\_)(__)(__)(__) (_____)(_)\_)
```

<p align="center">
<code>// JACK INTO YOUR DATABASE. GENERATE THE BACKEND. OWN THE GRID.</code>
</p>

---

## `> SYSTEM OVERVIEW`

**Spring Boot REST Generator** is a zero-config code generation engine that parses raw MySQL DDL dumps and outputs a fully wired Spring Boot REST API -- entities, controllers, DAOs, repositories -- all of it. You feed it SQL. It feeds you a backend.

No boilerplate. No hand-wiring. Just schema in, API out.

---

## `> CAPABILITIES`

```
[x] Parse MySQL CREATE TABLE statements
[x] Auto-detect primary keys, foreign keys, column types
[x] Generate JPA entities with @Id, @ManyToOne, @JoinColumn
[x] Generate REST controllers (GET / POST / PUT / DELETE)
[x] Generate DAO service layer with GenericDao pattern
[x] Generate Spring Data JPA repositories
[x] Map MySQL types --> Java types (varchar->String, bigint->Long, datetime->LocalDateTime, ...)
[x] Lombok-powered: @Data, @Builder, @AllArgsConstructor, @NoArgsConstructor
[x] snake_case tables --> PascalCase entities, camelCase fields
```

---

## `> TECH STACK`

| Layer          | Tech                          |
|----------------|-------------------------------|
| Runtime        | Java 8                        |
| Framework      | Spring Boot 2.1.4             |
| ORM            | Spring Data JPA               |
| Build          | Maven                         |
| Boilerplate    | Lombok 1.18.8                 |
| Tests          | JUnit 5                       |
| DB Support     | MySQL DDL                     |

---

## `> QUICKSTART`

### 1. Configure

Edit `src/main/resources/config.properties`:

```properties
target.folder=/absolute/path/to/your/project/src/main/java/
target.package=com/your/package
file.name=dump.sql
```

### 2. Drop your SQL

Place your MySQL DDL dump file in `src/main/resources/`.

### 3. Execute

Run `Main.main()`. Watch the grid light up.

---

## `> OUTPUT MATRIX`

```
target.folder/
 |-- entity/
 |    |-- User.java
 |    |-- Module.java
 |    \-- ...
 |-- rest/
 |    |-- UserResource.java
 |    |-- ModuleResource.java
 |    \-- ...
 |-- dao/
 |    |-- UserDao.java
 |    |-- GenericDao.java
 |    \-- ...
 |-- repository/
 |    |-- UserRepository.java
 |    \-- ...
 \-- utils/
      \-- GlobalConstants.java
```

---

## `> GENERATED ENDPOINTS`

Every entity gets a full CRUD interface wired to `/api/v1`:

```
GET     /api/v1/{entity}        // pull all records
GET     /api/v1/{entity}/{id}   // pull one by id
POST    /api/v1/{entity}        // create
PUT     /api/v1/{entity}        // update
DELETE  /api/v1/{entity}/{id}   // flatline one
DELETE  /api/v1/{entity}        // flatline all
```

---

## `> TYPE MAPPING`

```
MySQL              -->   Java
-----------------------------------------
int, tinyint       -->   Integer
bigint             -->   Long
varchar            -->   String
float              -->   Float
double             -->   Double
datetime           -->   LocalDateTime
timestamp          -->   LocalDate
time               -->   LocalTime
bit                -->   String
PRIMARY KEY        -->   Long (default)
FOREIGN KEY        -->   Integer (default)
```

---

## `> ARCHITECTURE`

```
 dump.sql
    |
    v
 [ TOKENIZER ] -- strips comments, splits words
    |
    v
 [ PARSER ] -- detects CREATE TABLE, columns, keys
    |
    v
 [ TEMPLATE ENGINE ] -- fills {{entityName}}, {{tableName}}, {{fields}}
    |
    v
 [ FILE WRITER ] -- outputs layered Spring Boot project
```

---

<p align="center">
<code>// CREATED BY MENKETECHNOLOGIES</code>
</p>
