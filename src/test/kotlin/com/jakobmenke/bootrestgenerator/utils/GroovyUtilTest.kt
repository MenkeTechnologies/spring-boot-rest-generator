package com.jakobmenke.bootrestgenerator.utils

import com.jakobmenke.bootrestgenerator.dto.Entity
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GroovyUtilTest {

    @BeforeEach
    fun setUp() {
        Globals.LANGUAGE = "groovy"
    }

    @AfterEach
    fun tearDown() {
        Globals.LANGUAGE = "java"
    }

    private fun parseFromSql(sql: String): List<Entity> {
        val words = mutableListOf<String>()
        Util.getWords(words, ByteArrayInputStream(sql.toByteArray()))
        val entities = mutableListOf<Entity>()
        Util.parseWords(entities, words)
        return entities
    }

    // ── Groovy type mapping (unit-level) ─────────────────────────────────

    @Nested
    inner class GroovyTypeMapping {
        @Test
        fun intMapsToInteger() {
            val entities = parseFromSql("CREATE TABLE `T`\n(\n`C` int(11) NOT NULL\n)")
            assertEquals("Integer", entities[0].columns[0].javaType)
        }

        @Test
        fun tinyintMapsToInteger() {
            val entities = parseFromSql("CREATE TABLE `T`\n(\n`C` tinyint(1) NOT NULL\n)")
            assertEquals("Integer", entities[0].columns[0].javaType)
        }

        @Test
        fun bitMapsToString() {
            val entities = parseFromSql("CREATE TABLE `T`\n(\n`C` bit(1) NOT NULL\n)")
            assertEquals("String", entities[0].columns[0].javaType)
        }

        @Test
        fun varcharMapsToString() {
            val entities = parseFromSql("CREATE TABLE `T`\n(\n`C` varchar(255) NOT NULL\n)")
            assertEquals("String", entities[0].columns[0].javaType)
        }

        @Test
        fun bigintMapsToLong() {
            val entities = parseFromSql("CREATE TABLE `T`\n(\n`C` bigint(20) NOT NULL\n)")
            assertEquals("Long", entities[0].columns[0].javaType)
        }

        @Test
        fun floatMapsToFloat() {
            val entities = parseFromSql("CREATE TABLE `T`\n(\n`C` float NOT NULL\n)")
            assertEquals("Float", entities[0].columns[0].javaType)
        }

        @Test
        fun doubleMapsToDouble() {
            val entities = parseFromSql("CREATE TABLE `T`\n(\n`C` double NOT NULL\n)")
            assertEquals("Double", entities[0].columns[0].javaType)
        }

        @Test
        fun datetimeMapsToLocalDate() {
            val entities = parseFromSql("CREATE TABLE `T`\n(\n`C` datetime NOT NULL\n)")
            assertEquals("LocalDate", entities[0].columns[0].javaType)
        }

        @Test
        fun timestampMapsToLocalDateTime() {
            val entities = parseFromSql("CREATE TABLE `T`\n(\n`C` timestamp NULL\n)")
            assertEquals("LocalDateTime", entities[0].columns[0].javaType)
        }

        @Test
        fun timeMapsToLocalTime() {
            val entities = parseFromSql("CREATE TABLE `T`\n(\n`C` time NOT NULL\n)")
            assertEquals("LocalTime", entities[0].columns[0].javaType)
        }

        @Test
        fun allTypesTogetherInGroovy() {
            val sql = """
                CREATE TABLE `ALL_TYPES`
                (
                    `C1` varchar(50) NOT NULL,
                    `C2` int(11) NOT NULL,
                    `C3` bigint(20) NOT NULL,
                    `C4` datetime NOT NULL,
                    `C5` timestamp NULL,
                    `C6` float NOT NULL,
                    `C7` double NOT NULL,
                    `C8` tinyint(1) NOT NULL,
                    `C9` bit(1) NOT NULL
                )
            """.trimIndent()
            val entities = parseFromSql(sql)
            val types = entities[0].columns.map { it.javaType }
            assertEquals(listOf("String", "Integer", "Long", "LocalDate", "LocalDateTime", "Float", "Double", "Integer", "String"), types)
        }
    }

    // ── Groovy getId FK type ─────────────────────────────────────────────

    @Nested
    inner class GroovyGetId {
        @Test
        fun foreignKeyReturnsIntegerInGroovy() {
            val result = Util.getId("FOREIGN KEY (`MODULE_ID`) REFERENCES `MODULE` (`MODULE_ID`)")
            assertEquals("Integer", result.javaType)
        }

        @Test
        fun primaryKeyReturnsLongInGroovy() {
            val result = Util.getId("PRIMARY KEY (`USER_ID`)")
            assertEquals("Long", result.javaType)
        }

        @Test
        fun foreignKeyMultiWordTableReturnsIntegerInGroovy() {
            val result = Util.getId("FOREIGN KEY (`LANGUAGE_LEVEL_ID`) REFERENCES `LANGUAGE_LEVEL` (`LANGUAGE_LEVEL_ID`)")
            assertEquals("Integer", result.javaType)
        }
    }

    // ── Groovy matches Java types (not Kotlin) ───────────────────────────

    @Nested
    inner class GroovyMatchesJavaTypes {
        @Test
        fun intColumnMatchesJavaNotKotlin() {
            val sql = "CREATE TABLE `T`\n(\n`C` int(11) NOT NULL\n)"

            Globals.LANGUAGE = "groovy"
            val groovyEntities = parseFromSql(sql)
            assertEquals("Integer", groovyEntities[0].columns[0].javaType)

            Globals.LANGUAGE = "java"
            val javaEntities = parseFromSql(sql)
            assertEquals("Integer", javaEntities[0].columns[0].javaType)

            Globals.LANGUAGE = "kotlin"
            val kotlinEntities = parseFromSql(sql)
            assertEquals("Int", kotlinEntities[0].columns[0].javaType)

            Globals.LANGUAGE = "groovy"
        }

        @Test
        fun bitColumnMatchesJavaNotKotlin() {
            val sql = "CREATE TABLE `T`\n(\n`C` bit(1) NOT NULL\n)"

            Globals.LANGUAGE = "groovy"
            val groovyEntities = parseFromSql(sql)
            assertEquals("String", groovyEntities[0].columns[0].javaType)

            Globals.LANGUAGE = "java"
            val javaEntities = parseFromSql(sql)
            assertEquals("String", javaEntities[0].columns[0].javaType)

            Globals.LANGUAGE = "kotlin"
            val kotlinEntities = parseFromSql(sql)
            assertEquals("Boolean", kotlinEntities[0].columns[0].javaType)

            Globals.LANGUAGE = "groovy"
        }

        @Test
        fun foreignKeyMatchesJavaNotKotlin() {
            Globals.LANGUAGE = "groovy"
            val groovyResult = Util.getId("FOREIGN KEY (`FK_COL`) REFERENCES `OTHER` (`FK_COL`)")
            assertEquals("Integer", groovyResult.javaType)

            Globals.LANGUAGE = "java"
            val javaResult = Util.getId("FOREIGN KEY (`FK_COL`) REFERENCES `OTHER` (`FK_COL`)")
            assertEquals("Integer", javaResult.javaType)

            Globals.LANGUAGE = "kotlin"
            val kotlinResult = Util.getId("FOREIGN KEY (`FK_COL`) REFERENCES `OTHER` (`FK_COL`)")
            assertEquals("Int", kotlinResult.javaType)

            Globals.LANGUAGE = "groovy"
        }

        @Test
        fun sharedTypesRemainSameAcrossAllLanguages() {
            val sql = """
                CREATE TABLE `T`
                (
                    `A` varchar(50) NOT NULL,
                    `B` bigint(20) NOT NULL,
                    `C` float NOT NULL,
                    `D` double NOT NULL,
                    `E` datetime NOT NULL,
                    `F` timestamp NULL,
                    `G` time NOT NULL
                )
            """.trimIndent()

            Globals.LANGUAGE = "groovy"
            val groovyTypes = parseFromSql(sql)[0].columns.map { it.javaType }

            Globals.LANGUAGE = "java"
            val javaTypes = parseFromSql(sql)[0].columns.map { it.javaType }

            Globals.LANGUAGE = "kotlin"
            val kotlinTypes = parseFromSql(sql)[0].columns.map { it.javaType }

            assertEquals(groovyTypes, javaTypes)
            assertEquals(groovyTypes, kotlinTypes)

            Globals.LANGUAGE = "groovy"
        }
    }

    // ── Self-referencing FK ──────────────────────────────────────────────

    @Nested
    inner class SelfReferencingFK {
        @Test
        fun selfReferencingForeignKeyPkStillMarked() {
            val sql = """
                CREATE TABLE `EMPLOYEE`
                (
                    `EMPLOYEE_ID` int(11) NOT NULL AUTO_INCREMENT,
                    `MANAGER_ID` int(11) NULL,
                    `NAME` varchar(100) NOT NULL,
                    PRIMARY KEY (`EMPLOYEE_ID`) extra words pad here to ten,
                    FOREIGN KEY (`MANAGER_ID`) REFERENCES `EMPLOYEE` (`EMPLOYEE_ID`) ON DELETE SET
                )
            """.trimIndent()
            val entities = parseFromSql(sql)
            assertEquals(1, entities.size)
            assertEquals("Employee", entities[0].entityName)

            val pk = entities[0].columns.find { it.databaseIdType == "@Id" }
            assertNotNull(pk)
            assertEquals("Long", pk.javaType)

            val managerCol = entities[0].columns.find { it.databaseColumnName == "MANAGER_ID" }
            assertNotNull(managerCol)
            assertEquals("Integer", managerCol.javaType)
            assertNull(managerCol.databaseIdType)
        }
    }

    // ── Groovy with PostgreSQL types ─────────────────────────────────────

    @Nested
    inner class GroovyPostgresqlTypes {
        private fun parsePostgresql(sql: String): List<Entity> {
            Globals.DB_TYPE = "postgresql"
            val words = mutableListOf<String>()
            Util.getWords(words, ByteArrayInputStream(sql.toByteArray()))
            Util.normalizePostgresqlWords(words)
            val entities = mutableListOf<Entity>()
            Util.parseWords(entities, words)
            Globals.DB_TYPE = "mysql"
            return entities
        }

        @Test
        fun pgIntegerMapsToInteger() {
            val sql = "CREATE TABLE \"T\"\n(\n\"C\" integer NOT NULL\n)"
            val entities = parsePostgresql(sql)
            assertEquals("Integer", entities[0].columns[0].javaType)
        }

        @Test
        fun pgBooleanMapsToString() {
            val sql = "CREATE TABLE \"T\"\n(\n\"C\" boolean NOT NULL\n)"
            val entities = parsePostgresql(sql)
            assertEquals("String", entities[0].columns[0].javaType)
        }

        @Test
        fun pgTextMapsToString() {
            val sql = "CREATE TABLE \"T\"\n(\n\"C\" text NOT NULL\n)"
            val entities = parsePostgresql(sql)
            assertEquals("String", entities[0].columns[0].javaType)
        }

        @Test
        fun pgNumericMapsToDouble() {
            val sql = "CREATE TABLE \"T\"\n(\n\"C\" numeric NOT NULL\n)"
            val entities = parsePostgresql(sql)
            assertEquals("Double", entities[0].columns[0].javaType)
        }

        @Test
        fun pgRealMapsToFloat() {
            val sql = "CREATE TABLE \"T\"\n(\n\"C\" real NOT NULL\n)"
            val entities = parsePostgresql(sql)
            assertEquals("Float", entities[0].columns[0].javaType)
        }
    }

    // ── Groovy with MSSQL types ──────────────────────────────────────────

    @Nested
    inner class GroovyMssqlTypes {
        private fun parseMssql(sql: String): List<Entity> {
            Globals.DB_TYPE = "mssql"
            val words = mutableListOf<String>()
            Util.getWords(words, ByteArrayInputStream(sql.toByteArray()))
            Util.normalizeMssqlWords(words)
            val entities = mutableListOf<Entity>()
            Util.parseWords(entities, words)
            Globals.DB_TYPE = "mysql"
            return entities
        }

        @Test
        fun mssqlNvarcharMapsToString() {
            val sql = "CREATE TABLE [T]\n(\n[C] nvarchar(100) NOT NULL\n)"
            val entities = parseMssql(sql)
            assertEquals("String", entities[0].columns[0].javaType)
        }

        @Test
        fun mssqlMoneyMapsToDouble() {
            val sql = "CREATE TABLE [T]\n(\n[C] money NOT NULL\n)"
            val entities = parseMssql(sql)
            assertEquals("Double", entities[0].columns[0].javaType)
        }

        @Test
        fun mssqlUniqueidentifierMapsToString() {
            val sql = "CREATE TABLE [T]\n(\n[C] uniqueidentifier NOT NULL\n)"
            val entities = parseMssql(sql)
            assertEquals("String", entities[0].columns[0].javaType)
        }

        @Test
        fun mssqlDatetime2MapsToLocalDateTime() {
            val sql = "CREATE TABLE [T]\n(\n[C] datetime2 NOT NULL\n)"
            val entities = parseMssql(sql)
            assertEquals("LocalDateTime", entities[0].columns[0].javaType)
        }

        @Test
        fun mssqlDecimalMapsToDouble() {
            val sql = "CREATE TABLE [T]\n(\n[C] decimal(18,2) NOT NULL\n)"
            val entities = parseMssql(sql)
            assertEquals("Double", entities[0].columns[0].javaType)
        }
    }

    // ── Groovy with SQLite types ─────────────────────────────────────────

    @Nested
    inner class GroovySqliteTypes {
        private fun parseSqlite(sql: String): List<Entity> {
            Globals.DB_TYPE = "sqlite"
            val words = mutableListOf<String>()
            Util.getWords(words, ByteArrayInputStream(sql.toByteArray()))
            Util.normalizeSqliteWords(words)
            val entities = mutableListOf<Entity>()
            Util.parseWords(entities, words)
            Globals.DB_TYPE = "mysql"
            return entities
        }

        @Test
        fun sqliteIntegerMapsToInteger() {
            val sql = "CREATE TABLE T\n(\nC INTEGER NOT NULL\n)"
            val entities = parseSqlite(sql)
            assertEquals("Integer", entities[0].columns[0].javaType)
        }

        @Test
        fun sqliteTextMapsToString() {
            val sql = "CREATE TABLE T\n(\nC TEXT NOT NULL\n)"
            val entities = parseSqlite(sql)
            assertEquals("String", entities[0].columns[0].javaType)
        }

        @Test
        fun sqliteRealMapsToFloat() {
            val sql = "CREATE TABLE T\n(\nC REAL NOT NULL\n)"
            val entities = parseSqlite(sql)
            assertEquals("Float", entities[0].columns[0].javaType)
        }
    }
}
