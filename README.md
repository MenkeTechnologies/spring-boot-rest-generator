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
[x] Output in Java, Kotlin, or Groovy
[x] Map MySQL types --> Java/Kotlin/Groovy/Groovy types (varchar->String, bigint->Long, datetime->LocalDateTime, ...)
[x] Map PostgreSQL types --> Java/Kotlin/Groovy/Groovy types (integer, text, boolean, serial, numeric, real, ...)
[x] Map SQLite types --> Java/Kotlin/Groovy/Groovy types (INTEGER, TEXT, REAL, NUMERIC, BLOB, ...)
[x] Map MSSQL types --> Java/Kotlin/Groovy/Groovy types (nvarchar, uniqueidentifier, money, datetime2, ...)
[x] Java: Lombok-powered (@Data, @AllArgsConstructor, @NoArgsConstructor)
[x] Kotlin: Constructor injection, var properties with defaults, no Lombok
[x] Groovy: @Canonical annotation, field injection, no Lombok
[x] snake_case tables --> PascalCase entities, camelCase fields
[x] Template-driven codegen with {{placeholder}} substitution
```

---

## `> TECH STACK`

| Layer          | Tech                          |
|----------------|-------------------------------|
| Language       | Kotlin 2.3.20                 |
| Runtime        | Java 25                       |
| Framework      | Spring Boot 4.0.4             |
| ORM            | Spring Data JPA (Jakarta)     |
| Build          | Gradle 9.4.1 (Kotlin DSL)    |
| Boilerplate    | Lombok (Java), @Canonical (Groovy) |
| Tests          | JUnit 5                       |
| DB Support     | MySQL, PostgreSQL, SQLite, MSSQL |
| Output Languages | Java, Kotlin, Groovy          |

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

Set `target.language` to `java`, `kotlin`, or `groovy` to control the generated output language. Default is `java`.

Set `database.type` to `mysql`, `postgresql`, `sqlite`, or `mssql` to match your dump file format. Default is `mysql`.

### 2. Drop your SQL

Place your DDL dump file in `src/main/resources/`.

For MySQL, use `mysqldump --no-data` to generate the dump file.
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

File extensions depend on `target.language`: `.java`, `.kt`, or `.groovy`.

```
{target.folder}/{target.package}/
 |-- entity/
 |    |-- User.{ext}           @Entity @Table @Column @Id @ManyToOne
 |    |-- Module.{ext}
 |    \-- ...
 |-- rest/
 |    |-- UserResource.{ext}   @RestController with full CRUD
 |    |-- ModuleResource.{ext}
 |    \-- ...
 |-- dao/
 |    |-- UserDao.{ext}        @Service implementing GenericDao<T>
 |    |-- GenericDao.{ext}     Generic interface for all DAOs
 |    \-- ...
 |-- repository/
 |    |-- UserRepository.{ext} extends JpaRepository<T, Long>
 |    \-- ...
 \-- utils/
      \-- GlobalConstants.{ext}
```

### Language Differences

| Feature              | Java                          | Kotlin                         | Groovy                         |
|----------------------|-------------------------------|--------------------------------|--------------------------------|
| Entity boilerplate   | Lombok (@Data, @NoArgsConstructor) | var properties with defaults | @Canonical                     |
| DI style             | @Autowired field injection    | Constructor injection          | @Autowired field injection     |
| Inheritance syntax   | `extends` / `implements`      | `:` (colon)                    | `extends` / `implements`       |
| File extension       | `.java`                       | `.kt`                          | `.groovy`                      |
| Semicolons           | Yes                           | No                             | No                             |
| int type             | Integer                       | Int                            | Integer                        |
| bit/boolean type     | String                        | Boolean                        | String                         |
| FK type              | Integer                       | Int                            | Integer                        |

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
MySQL              -->   Java/Kotlin/Groovy
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
PostgreSQL                    -->   Java/Kotlin/Groovy
-------------------------------------------------
integer, smallint, serial     -->   Integer
bigint, bigserial             -->   Long
character varying, varchar    -->   String
text                          -->   String
real                          -->   Float
double precision              -->   Double
numeric                       -->   Double
boolean, bool                 -->   String (Java/Groovy) / Boolean (Kotlin)
date                          -->   LocalDate
timestamp (with/without tz)   -->   LocalDateTime
time (with/without tz)        -->   LocalTime
PRIMARY KEY                   -->   Long (default)
FOREIGN KEY                   -->   Integer (default)
```

### SQLite

```
SQLite               -->   Java/Kotlin/Groovy
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
MSSQL                         -->   Java/Kotlin/Groovy
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
bit                           -->   String (Java/Groovy) / Boolean (Kotlin)
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
 |-- *.tmpl                              Java templates (default)
 |-- kotlin/*.tmpl                       Kotlin templates
 \-- groovy/*.tmpl                       Groovy templates
```

---

## `> RUNNING TESTS`

```bash
./gradlew test
```

### Test Suite Overview

**500+ tests** across **28 test classes** covering unit, template, and integration layers.

| Category | Test Class | Tests | What It Covers |
|----------|-----------|-------|----------------|
| **Unit** | `MainTest` | 6 | PK/FK identification, string capitalization, camelCase conversion |
| **Unit** | `UtilTest` | 36+ | `firstLetterToCaps`, `camelName`, `getId`, `getWords`, `parseWords` |
| **Unit** | `KotlinUtilTest` | 15+ | Kotlin type mapping (`Int` vs `Integer`, `Boolean` vs `String`), PK/FK types |
| **Unit** | `GroovyUtilTest` | 30+ | Groovy type mapping (matches Java types), cross-database type handling |
| **Unit** | `ColumnToFieldTest` | 8 | `ColumnToField` data class constructors, equality, copy, mutability |
| **Unit** | `EntityTest` | 8 | `Entity` data class constructors, properties, column management |
| **Unit** | `EntityToRESTConstantsTest` | 50+ | Regex patterns for all SQL types, PK/FK parsing, MySQL/PG/SQLite/MSSQL type regexes |
| **Unit** | `EdgeCaseParsingTest` | 40+ | Empty input, special characters, self-referencing FKs, large names, multi-table edge cases |
| **Unit** | `ConfigurationTest` | 15 | Config file loading, language-specific default folders, fallback behavior |
| **Unit** | `KotlinConfigurationTest` | 9 | Kotlin language property, global flags, case-insensitive matching |
| **Unit** | `GroovyConfigurationTest` | 8 | Groovy language property, global flags, mutual exclusion with Kotlin |
| **Template** | `TemplatesTest` | 50+ | Java templates: entities, DAOs, repositories, REST resources, package declarations |
| **Template** | `KotlinTemplatesTest` | 50+ | Kotlin templates: colon inheritance, constructor injection, `val`/`var`, no Lombok |
| **Template** | `GroovyTemplatesTest` | 60+ | Groovy templates: `@Canonical`, `@Autowired`, `implements`, no Lombok |
| **Template** | `KotlinEntityFieldDefaultsTest` | 20+ | Kotlin field defaults (`""`, `0`, `0L`, `false`), nullable types, Java mode guard |
| **Template** | `GroovyEntityFieldsTest` | 15+ | Groovy field declarations without defaults or nullable types, language comparison |
| **Template** | `RestRepositoryTemplateTest` | 7 | `@RepositoryRestResource` annotation, Spring Data REST imports |
| **Integration** | `FullPipelineTest` | 25+ | End-to-end MySQL: parse 18 entities, validate columns/keys/types, write all files |
| **Integration** | `PostgresqlPipelineTest` | 30+ | PostgreSQL pipeline: `ALTER TABLE` constraints, PG type mappings, cross-language |
| **Integration** | `SqlitePipelineTest` | 25+ | SQLite pipeline: inline PKs, `.dump` noise filtering (`INSERT`, `PRAGMA`, `BEGIN`) |
| **Integration** | `MssqlPipelineTest` | 30+ | MSSQL pipeline: `[bracket]` stripping, `SET`/`GO` filtering, MSSQL type mappings |
| **Integration** | `KotlinPipelineTest` | 20+ | Kotlin pipeline: type mapping, `.kt` file generation, template content |
| **Integration** | `GroovyPipelineTest` | 20+ | Groovy pipeline: type mapping, `.groovy` file generation, `@Canonical` templates |
| **Integration** | `CustomSqlParsingTest` | 14 | Complex SQL: mixed comments, multi-FK tables, every data type, end-to-end template gen |
| **Integration** | `EmptyEntityListTest` | 15+ | Empty entity lists, single entity generation, deep package paths |
| **Integration** | `KotlinContentValidationTest` | 16 | Generated Kotlin files: no Java artifacts, no placeholders, correct idioms |
| **Integration** | `GroovyContentValidationTest` | 20+ | Generated Groovy files: no Java/Kotlin artifacts, pure Groovy syntax |
| **Integration** | `CrossDatabaseLanguageTest` | 20+ | All 12 db/language combos (4 databases x 3 languages), parameterized matrix |

### Coverage Matrix

```
              MySQL   PostgreSQL   SQLite   MSSQL
           +--------+------------+--------+--------+
Java       |   ✓    |     ✓      |   ✓    |   ✓    |
Kotlin     |   ✓    |     ✓      |   ✓    |   ✓    |
Groovy     |   ✓    |     ✓      |   ✓    |   ✓    |
           +--------+------------+--------+--------+
```

---

<p align="center">
<code>// CREATED BY MENKETECHNOLOGIES</code>
</p>
