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

class KotlinUtilTest {

    @BeforeEach
    fun setUp() {
        Globals.LANGUAGE = "kotlin"
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

    // ── Kotlin type mapping (unit-level) ────────────────────────────────

    @Nested
    inner class KotlinTypeMapping {
        @Test
        fun intMapsToInt() {
            val entities = parseFromSql("CREATE TABLE `T`\n(\n`C` int(11) NOT NULL\n)")
            assertEquals("Int", entities[0].columns[0].javaType)
        }

        @Test
        fun tinyintMapsToInt() {
            val entities = parseFromSql("CREATE TABLE `T`\n(\n`C` tinyint(1) NOT NULL\n)")
            assertEquals("Int", entities[0].columns[0].javaType)
        }

        @Test
        fun bitMapsToBoolean() {
            val entities = parseFromSql("CREATE TABLE `T`\n(\n`C` bit(1) NOT NULL\n)")
            assertEquals("Boolean", entities[0].columns[0].javaType)
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
    }

    // ── Kotlin getId FK type ────────────────────────────────────────────

    @Nested
    inner class KotlinGetId {
        @Test
        fun foreignKeyReturnsIntInKotlin() {
            val result = Util.getId("FOREIGN KEY (`MODULE_ID`) REFERENCES `MODULE` (`MODULE_ID`)")
            assertEquals("Int", result.javaType)
        }

        @Test
        fun primaryKeyReturnsLongInKotlin() {
            val result = Util.getId("PRIMARY KEY (`USER_ID`)")
            assertEquals("Long", result.javaType)
        }

        @Test
        fun foreignKeyMultiWordTableReturnsIntInKotlin() {
            val result = Util.getId("FOREIGN KEY (`LANGUAGE_LEVEL_ID`) REFERENCES `LANGUAGE_LEVEL` (`LANGUAGE_LEVEL_ID`)")
            assertEquals("Int", result.javaType)
        }
    }

    // ── Kotlin vs Java bidirectional ────────────────────────────────────

    @Nested
    inner class KotlinVsJavaBidirectional {
        @Test
        fun intColumnDiffersBetweenLanguages() {
            val sql = "CREATE TABLE `T`\n(\n`C` int(11) NOT NULL\n)"

            Globals.LANGUAGE = "kotlin"
            val kotlinEntities = parseFromSql(sql)
            assertEquals("Int", kotlinEntities[0].columns[0].javaType)

            Globals.LANGUAGE = "java"
            val javaEntities = parseFromSql(sql)
            assertEquals("Integer", javaEntities[0].columns[0].javaType)
        }

        @Test
        fun bitColumnDiffersBetweenLanguages() {
            val sql = "CREATE TABLE `T`\n(\n`C` bit(1) NOT NULL\n)"

            Globals.LANGUAGE = "kotlin"
            val kotlinEntities = parseFromSql(sql)
            assertEquals("Boolean", kotlinEntities[0].columns[0].javaType)

            Globals.LANGUAGE = "java"
            val javaEntities = parseFromSql(sql)
            assertEquals("String", javaEntities[0].columns[0].javaType)
        }

        @Test
        fun foreignKeyDiffersBetweenLanguages() {
            Globals.LANGUAGE = "kotlin"
            val kotlinResult = Util.getId("FOREIGN KEY (`FK_COL`) REFERENCES `OTHER` (`FK_COL`)")
            assertEquals("Int", kotlinResult.javaType)

            Globals.LANGUAGE = "java"
            val javaResult = Util.getId("FOREIGN KEY (`FK_COL`) REFERENCES `OTHER` (`FK_COL`)")
            assertEquals("Integer", javaResult.javaType)
        }

        @Test
        fun sharedTypesRemainSame() {
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

            Globals.LANGUAGE = "kotlin"
            val kotlinEntities = parseFromSql(sql)
            val kotlinTypes = kotlinEntities[0].columns.map { it.javaType }

            Globals.LANGUAGE = "java"
            val javaEntities = parseFromSql(sql)
            val javaTypes = javaEntities[0].columns.map { it.javaType }

            assertEquals(kotlinTypes, javaTypes)
        }
    }

    // ── Self-referencing FK ─────────────────────────────────────────────

    @Nested
    inner class SelfReferencingFK {
        @Test
        fun selfReferencingForeignKeyPkStillMarked() {
            // Self-referencing FK: MANAGER_ID references EMPLOYEE.EMPLOYEE_ID
            // The parser marks EMPLOYEE_ID as @Id first. When the FK is processed,
            // getId returns dbName=EMPLOYEE_ID (the referenced col), but setPKorFKColumns
            // skips it because EMPLOYEE_ID already has databaseIdType=@Id.
            // This is a known parser limitation for self-referencing tables.
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

            // MANAGER_ID stays as a regular Int column (not marked as @ManyToOne)
            val managerCol = entities[0].columns.find { it.databaseColumnName == "MANAGER_ID" }
            assertNotNull(managerCol)
            assertEquals("Int", managerCol.javaType)
            assertNull(managerCol.databaseIdType)
        }
    }
}
