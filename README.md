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

**Spring Boot REST Generator** is a zero-config code generation engine that parses raw MySQL, PostgreSQL, SQLite, or Microsoft SQL Server DDL dumps and outputs a fully wired Spring Boot REST API -- entities, controllers, DAOs, repositories -- all of it. You feed it SQL. It feeds you a backend.

No boilerplate. No hand-wiring. Just schema in, API out.

---

## `> CAPABILITIES`

```
[x] Parse MySQL CREATE TABLE statements
[x] Parse PostgreSQL CREATE TABLE + ALTER TABLE statements (pg_dump compatible)
[x] Parse SQLite CREATE TABLE statements (.dump compatible)
[x] Parse MSSQL CREATE TABLE statements (SSMS Generate Scripts compatible)
[x] Auto-detect primary keys, foreign keys, column types
[x] Generate JPA entities with @Id, @ManyToOne, @JoinColumn
[x] Generate REST controllers (GET / POST / PUT / DELETE)
[x] Generate DAO service layer with GenericDao pattern
[x] Generate Spring Data JPA repositories
[x] Map MySQL types --> Java/Kotlin types (varchar->String, bigint->Long, datetime->LocalDateTime, ...)
[x] Map PostgreSQL types --> Java/Kotlin types (integer, text, boolean, serial, numeric, real, ...)
[x] Map SQLite types --> Java/Kotlin types (INTEGER, TEXT, REAL, NUMERIC, BLOB, ...)
[x] Map MSSQL types --> Java/Kotlin types (nvarchar, uniqueidentifier, money, datetime2, ...)
[x] Lombok-powered: @Data, @Builder, @AllArgsConstructor, @NoArgsConstructor
[x] snake_case tables --> PascalCase entities, camelCase fields
[x] Template-driven codegen with {{placeholder}} substitution
```

---

## `> TECH STACK`

| Layer          | Tech                          |
|----------------|-------------------------------|
| Language       | Kotlin 2.1.10                 |
| Runtime        | Java 23                       |
| Framework      | Spring Boot 3.4.4             |
| ORM            | Spring Data JPA (Jakarta)     |
| Build          | Gradle 9.4.1 (Kotlin DSL)    |
| Boilerplate    | Lombok                        |
| Tests          | JUnit 5                       |
| DB Support     | MySQL, PostgreSQL, SQLite, MSSQL |

---

## `> QUICKSTART`

### 1. Configure

Edit `src/main/resources/config.properties`:

```properties
target.folder=/absolute/path/to/your/project/src/main/java/
target.package=com/your/package
file.name=dump.sql
target.language=kotlin
database.type=mysql
```

Set `target.language` to `java` or `kotlin` to control the generated output language. Default is `java`.

Set `database.type` to `mysql`, `postgresql`, `sqlite`, or `mssql` to match your dump file format. Default is `mysql`.

### 2. Drop your SQL

Place your DDL dump file in `src/main/resources/`.

For PostgreSQL, use `pg_dump --schema-only` to generate the dump file.
For SQLite, use `sqlite3 database.db .dump` to generate the dump file.
For MSSQL, use SSMS "Generate Scripts" or `sqlcmd` to export the schema.

### 3. Execute

```bash
./gradlew run
```

Or run `Main.kt` from your IDE. Watch the grid light up.

---

## `> OUTPUT MATRIX`

```
{target.folder}/{target.package}/
 |-- entity/
 |    |-- User.java           @Entity @Data @Table @Column @Id @ManyToOne
 |    |-- Module.java
 |    \-- ...
 |-- rest/
 |    |-- UserResource.java   @RestController with full CRUD
 |    |-- ModuleResource.java
 |    \-- ...
 |-- dao/
 |    |-- UserDao.java        @Service implementing GenericDao<T>
 |    |-- GenericDao.java     Generic interface for all DAOs
 |    \-- ...
 |-- repository/
 |    |-- UserRepository.java extends JpaRepository<T, Long>
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

### MySQL

```
MySQL              -->   Java/Kotlin
-----------------------------------------
int, tinyint       -->   Integer
bigint             -->   Long
varchar            -->   String
float              -->   Float
double             -->   Double
datetime           -->   LocalDate
timestamp          -->   LocalDateTime
time               -->   LocalTime
bit                -->   String
PRIMARY KEY        -->   Long (default)
FOREIGN KEY        -->   Integer (default)
```

### PostgreSQL

```
PostgreSQL                    -->   Java/Kotlin
-------------------------------------------------
integer, smallint, serial     -->   Integer
bigint, bigserial             -->   Long
character varying, varchar    -->   String
text                          -->   String
real                          -->   Float
double precision              -->   Double
numeric                       -->   Double
boolean, bool                 -->   String (Java) / Boolean (Kotlin)
date                          -->   LocalDate
timestamp (with/without tz)   -->   LocalDateTime
time (with/without tz)        -->   LocalTime
PRIMARY KEY                   -->   Long (default)
FOREIGN KEY                   -->   Integer (default)
```

### SQLite

```
SQLite               -->   Java/Kotlin
-----------------------------------------
INTEGER              -->   Integer
TEXT                 -->   String
VARCHAR(n)           -->   String
REAL                 -->   Float
NUMERIC              -->   Double
BLOB                 -->   String
INTEGER PRIMARY KEY  -->   Long (inline PK detected)
FOREIGN KEY          -->   Integer (default)
```

### MSSQL (SQL Server)

```
MSSQL                         -->   Java/Kotlin
-------------------------------------------------
int, tinyint, smallint        -->   Integer
bigint                        -->   Long
nvarchar, nchar, varchar      -->   String
ntext, text, char             -->   String
uniqueidentifier              -->   String
image, xml                    -->   String
float                         -->   Float
real                          -->   Float
money, smallmoney             -->   Double
decimal, numeric              -->   Double
bit                           -->   String (Java) / Boolean (Kotlin)
date                          -->   LocalDate
datetime                      -->   LocalDate
datetime2, datetimeoffset     -->   LocalDateTime
smalldatetime                 -->   LocalDateTime
time                          -->   LocalTime
PRIMARY KEY                   -->   Long (default)
FOREIGN KEY                   -->   Integer (default)
```

---

## `> ARCHITECTURE`

```
 dump.sql
    |
    v
 [ TOKENIZER ] -- strips comments (#, --), splits tokens     (Util.getWords)
    |
    v
 [ NORMALIZER ] -- combines multi-word types, strips          (Util.normalizePostgresqlWords)
    |               punctuation, brackets, filters noise       (Util.normalizeSqliteWords)
    |               (database-specific normalizer per type)    (Util.normalizeMssqlWords)
    v
 [ PARSER ] -- detects CREATE TABLE, columns, keys           (Util.parseWords)
    |           supports ALTER TABLE for PG primary/foreign keys
    |
    v
 [ TEMPLATE ENGINE ] -- fills {{entityName}}, {{fields}}, .. (Templates.kt)
    |
    v
 [ FILE WRITER ] -- outputs layered Spring Boot project      (Main.kt)
```

---

## `> PROJECT STRUCTURE`

```
src/main/kotlin/com/jakobmenke/bootrestgenerator/
 |-- Main.kt                          Entry point & file writer
 |-- dto/
 |    |-- Entity.kt                   Entity data model
 |    \-- ColumnToField.kt            Column-to-field mapping
 |-- templates/
 |    \-- Templates.kt                Template engine & replacements
 \-- utils/
      |-- Configuration.kt            Config reader
      |-- Util.kt                     Parser, type mapper, key detector
      |-- EntityToRESTConstants.kt    Regex patterns & constants
      \-- Globals.kt                  Global state holder

src/main/resources/templates/
 |-- entity.tmpl                      JPA Entity template
 |-- rest.resource.tmpl               REST Controller template
 |-- dao.tmpl                         Service/DAO template
 |-- repository.tmpl                  Spring Data JPA interface
 |-- genericdao.tmpl                  Generic DAO interface
 |-- constants.tmpl                   Global constants
 \-- restrepository.tmpl              Alternative REST template
```

---

## `> RUNNING TESTS`

```bash
./gradlew test
```

---

<p align="center">
<code>// CREATED BY MENKETECHNOLOGIES</code>
</p>
