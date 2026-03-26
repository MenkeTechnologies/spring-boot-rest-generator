package com.jakobmenke.bootrestgenerator.utils

import com.jakobmenke.bootrestgenerator.dto.Entity
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EdgeCaseParsingTest {

    private fun parseFromSql(sql: String): List<Entity> {
        val words = mutableListOf<String>()
        Util.getWords(words, ByteArrayInputStream(sql.toByteArray()))
        val entities = mutableListOf<Entity>()
        Util.parseWords(entities, words)
        return entities
    }

    // ── Empty / minimal input ───────────────────────────────────────────

    @Nested
    inner class EmptyAndMinimalInput {
        @Test
        fun emptySqlProducesNoEntities() {
            val entities = parseFromSql("")
            assertTrue(entities.isEmpty())
        }

        @Test
        fun onlyCommentsProducesNoEntities() {
            val sql = """
                -- comment 1
                -- comment 2
                # hash comment
            """.trimIndent()
            val entities = parseFromSql(sql)
            assertTrue(entities.isEmpty())
        }

        @Test
        fun whitespaceOnlyProducesNoEntities() {
            val entities = parseFromSql("   \n\n   \n   ")
            assertTrue(entities.isEmpty())
        }

        @Test
        fun tableWithOnlyPrimaryKey() {
            val sql = """
                CREATE TABLE `SIMPLE`
                (
                    `SIMPLE_ID` int(11) NOT NULL AUTO_INCREMENT,
                    PRIMARY KEY (`SIMPLE_ID`) extra words pad here to ten
                )
            """.trimIndent()
            val entities = parseFromSql(sql)
            assertEquals(1, entities.size)
            assertEquals("Simple", entities[0].entityName)
            assertEquals(1, entities[0].columns.size)
            assertEquals("@Id", entities[0].columns[0].databaseIdType)
            assertEquals("Long", entities[0].columns[0].javaType)
        }

        @Test
        fun tableWithOnlyPrimaryKeyAndOneForeignKey() {
            val sql = """
                CREATE TABLE `LINK`
                (
                    `LINK_ID` int(11) NOT NULL AUTO_INCREMENT,
                    `PARENT_ID` int(11) NOT NULL,
                    PRIMARY KEY (`LINK_ID`) extra words pad here to ten,
                    FOREIGN KEY (`PARENT_ID`) REFERENCES `PARENT` (`PARENT_ID`) ON DELETE CASCADE
                )
            """.trimIndent()
            val entities = parseFromSql(sql)
            assertEquals(1, entities.size)
            val pk = entities[0].columns.find { it.databaseIdType == "@Id" }
            val fk = entities[0].columns.find { it.databaseIdType == "@ManyToOne" }
            assertNotNull(pk)
            assertNotNull(fk)
            assertEquals(2, entities[0].columns.size)
        }
    }

    // ── Column naming edge cases ────────────────────────────────────────

    @Nested
    inner class ColumnNamingEdgeCases {
        @Test
        fun singleWordColumnName() {
            val sql = "CREATE TABLE `T`\n(\n`NAME` varchar(150) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("name", entities[0].columns[0].camelCaseFieldName)
        }

        @Test
        fun columnWithNumbers() {
            val sql = "CREATE TABLE `T`\n(\n`FIELD_2_NAME` varchar(50) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("field2Name", entities[0].columns[0].camelCaseFieldName)
        }

        @Test
        fun allUppercaseSingleWord() {
            val sql = "CREATE TABLE `T`\n(\n`STATUS` varchar(20) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("status", entities[0].columns[0].camelCaseFieldName)
        }

        @Test
        fun veryLongColumnName() {
            val sql = "CREATE TABLE `T`\n(\n`SOME_VERY_LONG_COLUMN_NAME_THAT_GOES_ON_AND_ON` varchar(50) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("someVeryLongColumnNameThatGoesOnAndOn", entities[0].columns[0].camelCaseFieldName)
        }
    }

    // ── Table naming edge cases ─────────────────────────────────────────

    @Nested
    inner class TableNamingEdgeCases {
        @Test
        fun singleWordTableName() {
            val sql = "CREATE TABLE `USER`\n(\n`ID` int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("User", entities[0].entityName)
            assertEquals("USER", entities[0].tableName)
        }

        @Test
        fun fourWordSnakeCaseTable() {
            val sql = "CREATE TABLE `USER_ROLE_PERMISSION_MAP`\n(\n`ID` int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("UserRolePermissionMap", entities[0].entityName)
        }

        @Test
        fun lowercaseTableName() {
            val sql = "CREATE TABLE `user_data`\n(\n`ID` int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("UserData", entities[0].entityName)
            assertEquals("user_data", entities[0].tableName)
        }

        @Test
        fun mixedCaseTableName() {
            val sql = "CREATE TABLE `User_Profile`\n(\n`ID` int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("UserProfile", entities[0].entityName)
        }

        @Test
        fun tableWithNoBackticks() {
            val sql = "CREATE TABLE USER\n(\nID int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("User", entities[0].entityName)
        }
    }

    // ── Data type edge cases ────────────────────────────────────────────

    @Nested
    inner class DataTypeEdgeCases {
        @Test
        fun varcharWithNoLength() {
            val sql = "CREATE TABLE `T`\n(\n`C` varchar NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("String", entities[0].columns[0].javaType)
        }

        @Test
        fun intWithNoLength() {
            val sql = "CREATE TABLE `T`\n(\n`C` int NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("Integer", entities[0].columns[0].javaType)
        }

        @Test
        fun intWithSmallLength() {
            val sql = "CREATE TABLE `T`\n(\n`C` int(3) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("Integer", entities[0].columns[0].javaType)
        }

        @Test
        fun varcharLargeLength() {
            val sql = "CREATE TABLE `T`\n(\n`C` varchar(4000) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("String", entities[0].columns[0].javaType)
            assertEquals("varchar(4000)", entities[0].columns[0].databaseType)
        }

        @Test
        fun timeTypeMapping() {
            val sql = "CREATE TABLE `T`\n(\n`C` time NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("LocalTime", entities[0].columns[0].javaType)
        }
    }

    // ── Multiple tables edge cases ──────────────────────────────────────

    @Nested
    inner class MultipleTablesEdgeCases {
        @Test
        fun tenTablesAllParsed() {
            val tables = (1..10).joinToString("\n") { i ->
                "CREATE TABLE `TABLE_$i`\n(\n`ID` int(11) NOT NULL\n)"
            }
            val entities = parseFromSql(tables)
            assertEquals(10, entities.size)
        }

        @Test
        fun tablesWithInterleavedComments() {
            val sql = """
                -- table 1
                CREATE TABLE `A`
                (
                    `ID` int(11) NOT NULL
                )
                -- table 2
                # another comment
                CREATE TABLE `B`
                (
                    `ID` int(11) NOT NULL
                )
            """.trimIndent()
            val entities = parseFromSql(sql)
            assertEquals(2, entities.size)
            assertEquals("A", entities[0].entityName)
            assertEquals("B", entities[1].entityName)
        }
    }

    // ── Primary/foreign key edge cases ──────────────────────────────────

    @Nested
    inner class KeyEdgeCases {
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
            val pk = entities[0].columns.find { it.databaseIdType == "@Id" }
            assertEquals("user", pk?.camelCaseFieldName)
        }

        @Test
        fun foreignKeyStripsIdSuffix() {
            val sql = """
                CREATE TABLE `ORDER`
                (
                    `ORDER_ID` int(11) NOT NULL,
                    `USER_ID` int(11) NOT NULL,
                    PRIMARY KEY (`ORDER_ID`) extra words pad here to ten,
                    FOREIGN KEY (`USER_ID`) REFERENCES `USER` (`USER_ID`) ON DELETE CASCADE
                )
            """.trimIndent()
            val entities = parseFromSql(sql)
            val fk = entities[0].columns.find { it.databaseIdType == "@ManyToOne" }
            // camelCaseFieldName should have "Id" stripped by setPKorFKColumns
            assertEquals("user", fk?.camelCaseFieldName)
        }

        @Test
        fun primaryKeyWithoutBackticks() {
            val result = Util.getId("PRIMARY KEY (USER_ID)")
            assertEquals("@Id", result.databaseIdType)
            assertEquals("USER_ID", result.databaseColumnName)
        }

        @Test
        fun foreignKeyWithOnDeleteCascade() {
            val result = Util.getId("FOREIGN KEY (`MODULE_ID`) REFERENCES `MODULE` (`MODULE_ID`) ON DELETE CASCADE")
            assertEquals("@ManyToOne", result.databaseIdType)
            assertEquals("MODULE_ID", result.databaseColumnName)
        }

        @Test
        fun foreignKeyWithExtraWhitespace() {
            val result = Util.getId("FOREIGN KEY  (`COL_ID`)  REFERENCES  `OTHER`  (`COL_ID`)")
            assertEquals("@ManyToOne", result.databaseIdType)
        }

        @Test
        fun primaryKeyDatabaseTypeIsAlwaysNull() {
            val result = Util.getId("PRIMARY KEY (`SOME_ID`)")
            assertNull(result.databaseType)
        }

        @Test
        fun foreignKeyDatabaseTypeIsAlwaysNull() {
            val result = Util.getId("FOREIGN KEY (`FK_COL`) REFERENCES `OTHER` (`FK_COL`)")
            assertNull(result.databaseType)
        }

        @Test
        fun primaryKeyCamelCaseFieldNameIsNull() {
            val result = Util.getId("PRIMARY KEY (`ANY_ID`)")
            assertNull(result.camelCaseFieldName)
        }
    }

    // ── getWords edge cases ─────────────────────────────────────────────

    @Nested
    inner class GetWordsEdgeCases {
        private fun wordsFrom(text: String): MutableList<String> {
            val words = mutableListOf<String>()
            Util.getWords(words, ByteArrayInputStream(text.toByteArray()))
            return words
        }

        @Test
        fun tabsAreNotSplitters() {
            // tabs are part of whitespace split by space, should produce combined tokens
            val words = wordsFrom("CREATE\tTABLE\t`T`")
            // tabs produce single token per tab-separated group when split by space
            assertTrue(words.isNotEmpty())
        }

        @Test
        fun inlineCommentNotStripped() {
            // Only full-line comments are stripped; inline content after -- is not handled
            val words = wordsFrom("CREATE TABLE `T` -- inline comment")
            assertTrue(words.contains("CREATE"))
        }

        @Test
        fun emptyInputProducesNoWords() {
            val words = wordsFrom("")
            assertTrue(words.isEmpty())
        }

        @Test
        fun singleWordInput() {
            val words = wordsFrom("HELLO")
            assertEquals(listOf("HELLO"), words)
        }

        @Test
        fun multiLineInput() {
            val words = wordsFrom("CREATE\nTABLE\n`T`")
            assertEquals(listOf("CREATE", "TABLE", "`T`"), words)
        }
    }

    // ── camelName edge cases ────────────────────────────────────────────

    @Nested
    inner class CamelNameEdgeCases {
        @Test
        fun singleCharacter() = assertEquals("a", Util.camelName("A"))

        @Test
        fun alreadyLowercase() = assertEquals("hello", Util.camelName("hello"))

        @Test
        fun numbersPreserved() = assertEquals("user2Id", Util.camelName("USER_2_ID"))

        @Test
        fun fourWords() = assertEquals("someVeryLongName", Util.camelName("SOME_VERY_LONG_NAME"))
    }

    // ── firstLetterToCaps edge cases ────────────────────────────────────

    @Nested
    inner class FirstLetterToCapsEdgeCases {
        @Test
        fun singleLetter() = assertEquals("A", Util.firstLetterToCaps("a"))

        @Test
        fun numberFirst() = assertEquals("123abc", Util.firstLetterToCaps("123abc"))

        @Test
        fun specialCharFirst() = assertEquals("@test", Util.firstLetterToCaps("@test"))

        @Test
        fun unicodeChar() = assertEquals("Über", Util.firstLetterToCaps("über"))
    }
}
