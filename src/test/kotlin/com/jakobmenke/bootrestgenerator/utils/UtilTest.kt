package com.jakobmenke.bootrestgenerator.utils

import com.jakobmenke.bootrestgenerator.dto.ColumnToField
import com.jakobmenke.bootrestgenerator.dto.Entity
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UtilTest {

    // ── firstLetterToCaps ──────────────────────────────────────────────

    @Nested
    inner class FirstLetterToCaps {
        @Test
        fun lowercaseWord() = assertEquals("Hello", Util.firstLetterToCaps("hello"))

        @Test
        fun alreadyCapitalized() = assertEquals("Hello", Util.firstLetterToCaps("Hello"))

        @Test
        fun allUppercase() = assertEquals("HELLO", Util.firstLetterToCaps("HELLO"))

        @Test
        fun emptyString() = assertEquals("", Util.firstLetterToCaps(""))

        @Test
        fun singleChar() = assertEquals("A", Util.firstLetterToCaps("a"))

        @Test
        fun singleCharAlreadyUpper() = assertEquals("Z", Util.firstLetterToCaps("Z"))

        @Test
        fun startsWithDigit() = assertEquals("1abc", Util.firstLetterToCaps("1abc"))

        @Test
        fun sentenceCapitalized() = assertEquals("This will be capitalized", Util.firstLetterToCaps("this will be capitalized"))

        @Test
        fun multipleWords() = assertEquals("One two three", Util.firstLetterToCaps("one two three"))
    }

    // ── camelName ──────────────────────────────────────────────────────

    @Nested
    inner class CamelName {
        @Test
        fun snakeCaseToCamel() = assertEquals("helloWorld", Util.camelName("HELLO_WORLD"))

        @Test
        fun singleWord() = assertEquals("hello", Util.camelName("HELLO"))

        @Test
        fun tripleUnderscores() = assertEquals("helloWorldFoo", Util.camelName("HELLO_WORLD_FOO"))

        @Test
        fun singleCharAfterUnderscore() = assertEquals("helloW", Util.camelName("HELLO_W"))

        @Test
        fun mixedCase() = assertEquals("helloWorld", Util.camelName("HELLO_WorLD"))

        @Test
        fun noUnderscore() = assertEquals("test", Util.camelName("TEST"))

        @Test
        fun trailingId() = assertEquals("userId", Util.camelName("USER_ID"))

        @Test
        fun multiWordId() = assertEquals("languageLevelId", Util.camelName("LANGUAGE_LEVEL_ID"))
    }

    // ── getId ──────────────────────────────────────────────────────────

    @Nested
    inner class GetId {
        @Test
        fun primaryKeySimple() {
            val result = Util.getId("PRIMARY KEY (`ACTIVITY_ID`)")
            assertEquals("@Id", result.databaseIdType)
            assertEquals("ACTIVITY_ID", result.databaseColumnName)
            assertEquals("Long", result.javaType)
            assertNull(result.camelCaseFieldName)
        }

        @Test
        fun primaryKeyNoBackticks() {
            val result = Util.getId("PRIMARY KEY (USER_ID)")
            assertEquals("@Id", result.databaseIdType)
            assertEquals("USER_ID", result.databaseColumnName)
            assertEquals("Long", result.javaType)
        }

        @Test
        fun foreignKeySimple() {
            val result = Util.getId("FOREIGN KEY (`TRANSCRIPT_ID`) REFERENCES `TRANSCRIPT` (`TRANSCRIPT_ID`)")
            assertEquals("@ManyToOne", result.databaseIdType)
            assertEquals("TRANSCRIPT_ID", result.databaseColumnName)
            assertEquals("transcriptId", result.camelCaseFieldName)
            assertEquals("Integer", result.javaType)
        }

        @Test
        fun foreignKeyMultiWordTable() {
            val result = Util.getId("FOREIGN KEY (`LANGUAGE_LEVEL_ID`) REFERENCES `LANGUAGE_LEVEL` (`LANGUAGE_LEVEL_ID`)")
            assertEquals("@ManyToOne", result.databaseIdType)
            assertEquals("LANGUAGE_LEVEL_ID", result.databaseColumnName)
            assertEquals("languageLevelId", result.camelCaseFieldName)
            assertEquals("Integer", result.javaType)
        }

        @Test
        fun foreignKeyThreeWordTable() {
            val result = Util.getId("FOREIGN KEY (`TESTING_NO_BAD_ID`) REFERENCES `TESTING_NO_BAD` (`TESTING_NO_BAD_ID`)")
            assertEquals("@ManyToOne", result.databaseIdType)
            assertEquals("TESTING_NO_BAD_ID", result.databaseColumnName)
            assertEquals("testingNoBadId", result.camelCaseFieldName)
            assertEquals("Integer", result.javaType)
        }

        @Test
        fun foreignKeyWithTrailingSpaces() {
            val result = Util.getId("FOREIGN KEY (`MODULE_ID`) REFERENCES `MODULE` (`MODULE_ID`) ")
            assertEquals("@ManyToOne", result.databaseIdType)
            assertEquals("MODULE_ID", result.databaseColumnName)
            assertEquals("moduleId", result.camelCaseFieldName)
        }

        @Test
        fun primaryKeyDataTypeIsAlwaysLong() {
            val result = Util.getId("PRIMARY KEY (`SETTINGS_ID`)")
            assertEquals("Long", result.javaType)
        }

        @Test
        fun foreignKeyDataTypeIsAlwaysInteger() {
            val result = Util.getId("FOREIGN KEY (`USER_ID`) REFERENCES `USER` (`USER_ID`)")
            assertEquals("Integer", result.javaType)
        }

        @Test
        fun primaryKeyDatabaseTypeIsNull() {
            val result = Util.getId("PRIMARY KEY (`FOO_ID`)")
            assertNull(result.databaseType)
        }
    }

    // ── getWords ───────────────────────────────────────────────────────

    @Nested
    inner class GetWords {
        private fun wordsFrom(text: String): MutableList<String> {
            val words = mutableListOf<String>()
            Util.getWords(words, ByteArrayInputStream(text.toByteArray()))
            return words
        }

        @Test
        fun simpleLineTokenized() {
            val words = wordsFrom("CREATE TABLE `USER`")
            assertEquals(listOf("CREATE", "TABLE", "`USER`"), words)
        }

        @Test
        fun skipsCommentLines() {
            val words = wordsFrom("-- this is a comment\nCREATE TABLE `USER`")
            assertEquals(listOf("CREATE", "TABLE", "`USER`"), words)
        }

        @Test
        fun skipsHashComments() {
            val words = wordsFrom("# hash comment\nCREATE TABLE `FOO`")
            assertEquals(listOf("CREATE", "TABLE", "`FOO`"), words)
        }

        @Test
        fun emptyLinesProduceNoWords() {
            val words = wordsFrom("\n\n\n")
            assertTrue(words.isEmpty())
        }

        @Test
        fun multipleSpacesCollapsed() {
            val words = wordsFrom("CREATE  TABLE   `X`")
            assertEquals(listOf("CREATE", "TABLE", "`X`"), words)
        }

        @Test
        fun leadingWhitespaceCommentDetected() {
            val words = wordsFrom("   -- indented comment\nFOO BAR")
            assertEquals(listOf("FOO", "BAR"), words)
        }

        @Test
        fun mixedContentAndComments() {
            val text = """
                -- comment 1
                CREATE TABLE `A`
                # comment 2
                `COL1` int(11)
            """.trimIndent()
            val words = wordsFrom(text)
            assertTrue(words.contains("CREATE"))
            assertTrue(words.contains("`COL1`"))
            assertTrue(words.none { it.startsWith("--") || it.startsWith("#") })
        }

        @Test
        fun mysqlConditionalCommentsNotFiltered() {
            // /*!40101 ... */ lines don't start with -- or #, so they go through
            val words = wordsFrom("/*!40101 SET @OLD = @@VAR */;")
            assertTrue(words.isNotEmpty())
        }
    }

    // ── parseWords ─────────────────────────────────────────────────────

    @Nested
    inner class ParseWords {

        private fun parseFromSql(sql: String): List<Entity> {
            val words = mutableListOf<String>()
            Util.getWords(words, ByteArrayInputStream(sql.toByteArray()))
            val entities = mutableListOf<Entity>()
            Util.parseWords(entities, words)
            return entities
        }

        @Test
        fun singleTableNoKeys() {
            val sql = """
                CREATE TABLE `USER`
                (
                    `USER_ID` int(11) NOT NULL,
                    `NAME` varchar(150) NOT NULL
                )
            """.trimIndent()
            val entities = parseFromSql(sql)
            assertEquals(1, entities.size)
            assertEquals("User", entities[0].entityName)
            assertEquals("USER", entities[0].tableName)
            assertEquals(2, entities[0].columns.size)

            val col0 = entities[0].columns[0]
            assertEquals("USER_ID", col0.databaseColumnName)
            assertEquals("userId", col0.camelCaseFieldName)
            assertEquals("Integer", col0.javaType)

            val col1 = entities[0].columns[1]
            assertEquals("NAME", col1.databaseColumnName)
            assertEquals("name", col1.camelCaseFieldName)
            assertEquals("String", col1.javaType)
        }

        @Test
        fun entityNameFromSnakeCase() {
            val sql = "CREATE TABLE `LANGUAGE_LEVEL`\n(\n`LANGUAGE_LEVEL_ID` int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("LanguageLevel", entities[0].entityName)
            assertEquals("LANGUAGE_LEVEL", entities[0].tableName)
        }

        @Test
        fun entityNameFromThreeWordSnakeCase() {
            val sql = "CREATE TABLE `PAYMENT_METHOD_INFO`\n(\n`ID` int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("PaymentMethodInfo", entities[0].entityName)
        }

        @Test
        fun varcharMapsToString() {
            val sql = "CREATE TABLE `T`\n(\n`COL` varchar(255) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("String", entities[0].columns[0].javaType)
        }

        @Test
        fun intMapsToInteger() {
            val sql = "CREATE TABLE `T`\n(\n`COL` int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("Integer", entities[0].columns[0].javaType)
        }

        @Test
        fun tinyintMapsToInteger() {
            val sql = "CREATE TABLE `T`\n(\n`COL` tinyint(3) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("Integer", entities[0].columns[0].javaType)
        }

        @Test
        fun bigintMapsToLong() {
            val sql = "CREATE TABLE `T`\n(\n`COL` bigint(20) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("Long", entities[0].columns[0].javaType)
        }

        @Test
        fun datetimeMapsToLocalDate() {
            val sql = "CREATE TABLE `T`\n(\n`COL` datetime NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("LocalDate", entities[0].columns[0].javaType)
        }

        @Test
        fun timestampMapsToLocalDateTime() {
            val sql = "CREATE TABLE `T`\n(\n`COL` timestamp NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("LocalDateTime", entities[0].columns[0].javaType)
        }

        @Test
        fun floatMapsToFloat() {
            val sql = "CREATE TABLE `T`\n(\n`COL` float NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("Float", entities[0].columns[0].javaType)
        }

        @Test
        fun doubleMapsToDouble() {
            val sql = "CREATE TABLE `T`\n(\n`COL` double NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("Double", entities[0].columns[0].javaType)
        }

        @Test
        fun bitMapsToString() {
            val sql = "CREATE TABLE `T`\n(\n`COL` bit(1) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("String", entities[0].columns[0].javaType)
        }

        @Test
        fun primaryKeyMarksColumn() {
            val sql = """
                CREATE TABLE `USER`
                (
                    `USER_ID` int(11) NOT NULL,
                    `NAME` varchar(150) NOT NULL,
                    PRIMARY KEY (`USER_ID`) extra words pad here to ten
                )
            """.trimIndent()
            val entities = parseFromSql(sql)
            val pkCol = entities[0].columns.find { it.databaseIdType == "@Id" }
            assertEquals("@Id", pkCol?.databaseIdType)
            assertEquals("Long", pkCol?.javaType)
        }

        @Test
        fun foreignKeyMarksColumn() {
            val sql = """
                CREATE TABLE `ACTIVITY`
                (
                    `ACTIVITY_ID` int(11) NOT NULL,
                    `MODULE_ID` int(11) NOT NULL,
                    PRIMARY KEY (`ACTIVITY_ID`) extra words pad here to ten,
                    FOREIGN KEY (`MODULE_ID`) REFERENCES `MODULE` (`MODULE_ID`) ON DELETE CASCADE
                )
            """.trimIndent()
            val entities = parseFromSql(sql)
            val fkCol = entities[0].columns.find { it.databaseIdType == "@ManyToOne" }
            assertEquals("@ManyToOne", fkCol?.databaseIdType)
            assertEquals("Integer", fkCol?.javaType)
        }

        @Test
        fun multipleTables() {
            val sql = """
                CREATE TABLE `USER`
                (
                    `USER_ID` int(11) NOT NULL,
                    `NAME` varchar(150) NOT NULL
                )
                CREATE TABLE `LANGUAGE`
                (
                    `LANGUAGE_ID` int(11) NOT NULL,
                    `TITLE` varchar(500) NOT NULL
                )
            """.trimIndent()
            val entities = parseFromSql(sql)
            assertEquals(2, entities.size)
            assertEquals("User", entities[0].entityName)
            assertEquals("Language", entities[1].entityName)
        }

        @Test
        fun camelCaseFieldNamesGenerated() {
            val sql = "CREATE TABLE `T`\n(\n`SOME_LONG_COLUMN` varchar(50) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("someLongColumn", entities[0].columns[0].camelCaseFieldName)
        }

        @Test
        fun singleWordColumnName() {
            val sql = "CREATE TABLE `T`\n(\n`NAME` varchar(150) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("name", entities[0].columns[0].camelCaseFieldName)
        }

        @Test
        fun primaryKeyStripsIdSuffix() {
            val sql = """
                CREATE TABLE `USER`
                (
                    `USER_ID` int(11) NOT NULL,
                    PRIMARY KEY (`USER_ID`) extra words pad here to ten
                )
            """.trimIndent()
            val entities = parseFromSql(sql)
            val pkCol = entities[0].columns.find { it.databaseIdType == "@Id" }
            // camelCaseFieldName should have Id stripped
            assertEquals("user", pkCol?.camelCaseFieldName)
        }

        @Test
        fun createTableCaseInsensitive() {
            val sql = "create table `FOO`\n(\n`BAR` varchar(50) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals(1, entities.size)
            assertEquals("Foo", entities[0].entityName)
        }

        @Test
        fun backtickStrippedFromTableName() {
            val sql = "CREATE TABLE `MY_TABLE`\n(\n`COL` int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("MY_TABLE", entities[0].tableName)
        }

        @Test
        fun columnDatabaseTypePreserved() {
            val sql = "CREATE TABLE `T`\n(\n`COL` varchar(255) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("varchar(255)", entities[0].columns[0].databaseType)
        }

        @Test
        fun tableWithAllDataTypes() {
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
}
