package com.jakobmenke.bootrestgenerator.integration

import com.jakobmenke.bootrestgenerator.Main
import com.jakobmenke.bootrestgenerator.dto.Entity
import com.jakobmenke.bootrestgenerator.utils.Globals
import com.jakobmenke.bootrestgenerator.utils.Util
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MssqlPipelineTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var entities: List<Entity>
    private var originalDbType = "mysql"
    private var originalLanguage = "java"

    @BeforeEach
    fun setUp() {
        originalDbType = Globals.DB_TYPE
        originalLanguage = Globals.LANGUAGE
        Globals.DB_TYPE = "mssql"
        Globals.LANGUAGE = "java"
        val words = mutableListOf<String>()
        val inputStream = javaClass.classLoader.getResourceAsStream("mssql_dump.sql")!!
        Util.getWords(words, inputStream)
        Util.normalizeMssqlWords(words)
        val entityList = mutableListOf<Entity>()
        Util.parseWords(entityList, words)
        entities = entityList
    }

    @AfterEach
    fun tearDown() {
        Globals.DB_TYPE = originalDbType
        Globals.LANGUAGE = originalLanguage
    }

    // ── Table parsing ───────────────────────────────────────────────────

    @Test
    fun parsesAllTablesFromMssqlDump() {
        val names = entities.map { it.entityName }
        assertTrue(names.contains("User"))
        assertTrue(names.contains("Language"))
        assertTrue(names.contains("LanguageLevel"))
        assertTrue(names.contains("Module"))
        assertTrue(names.contains("Activity"))
        assertTrue(names.contains("VideoContent"))
        assertTrue(names.contains("Settings"))
        assertTrue(names.contains("WordBank"))
        assertTrue(names.contains("TypeTest"))
    }

    @Test
    fun totalEntityCount() {
        assertEquals(9, entities.size)
    }

    @Test
    fun squareBracketsAndSchemaStripped() {
        val user = entities.first { it.entityName == "User" }
        assertEquals("USER", user.tableName)
    }

    @Test
    fun snakeCaseTableNamesConvertedCorrectly() {
        assertEquals("LanguageLevel", entities.first { it.tableName == "LANGUAGE_LEVEL" }.entityName)
        assertEquals("VideoContent", entities.first { it.tableName == "VIDEO_CONTENT" }.entityName)
        assertEquals("WordBank", entities.first { it.tableName == "WORD_BANK" }.entityName)
        assertEquals("TypeTest", entities.first { it.tableName == "TYPE_TEST" }.entityName)
    }

    // ── Column parsing ──────────────────────────────────────────────────

    @Test
    fun userTableHasCorrectColumns() {
        val user = entities.first { it.entityName == "User" }
        val colNames = user.columns.map { it.databaseColumnName }
        assertTrue(colNames.contains("USER_ID") || user.columns.any { it.databaseIdType == "@Id" })
        assertTrue(colNames.contains("NAME"))
        assertTrue(colNames.contains("EMAIL"))
        assertTrue(colNames.contains("PW_HASH"))
        assertTrue(colNames.contains("DISCOUNT"))
        assertTrue(colNames.contains("CREATE_DATE"))
        assertTrue(colNames.contains("UPDATE_DATE"))
    }

    @Test
    fun activityTableHasCorrectColumns() {
        val activity = entities.first { it.entityName == "Activity" }
        val colNames = activity.columns.map { it.databaseColumnName }
        assertTrue(colNames.contains("ACTIVITY_TABLE"))
        assertTrue(colNames.contains("NAME"))
        assertTrue(colNames.contains("DESCRIPTION"))
        assertTrue(colNames.contains("ACTIVE"))
    }

    @Test
    fun columnNamesCamelCased() {
        val activity = entities.first { it.entityName == "Activity" }
        val col = activity.columns.find { it.databaseColumnName == "ACTIVITY_TABLE" }
        assertEquals("activityTable", col?.camelCaseFieldName)
    }

    // ── MSSQL type mapping ──────────────────────────────────────────────

    @Test
    fun nvarcharMapsToString() {
        val user = entities.first { it.entityName == "User" }
        val nameCol = user.columns.find { it.databaseColumnName == "NAME" }
        assertEquals("String", nameCol?.javaType)
    }

    @Test
    fun nvarcharMaxMapsToString() {
        val activity = entities.first { it.entityName == "Activity" }
        val descCol = activity.columns.find { it.databaseColumnName == "DESCRIPTION" }
        assertEquals("String", descCol?.javaType)
    }

    @Test
    fun varcharMapsToString() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val labelCol = typeTest.columns.find { it.databaseColumnName == "LABEL" }
        assertEquals("String", labelCol?.javaType)
    }

    @Test
    fun ncharMapsToString() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val col = typeTest.columns.find { it.databaseColumnName == "UNICODE_LABEL" }
        assertEquals("String", col?.javaType)
    }

    @Test
    fun uniqueidentifierMapsToString() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val guidCol = typeTest.columns.find { it.databaseColumnName == "GUID" }
        assertEquals("String", guidCol?.javaType)
    }

    @Test
    fun moneyMapsToDouble() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val priceCol = typeTest.columns.find { it.databaseColumnName == "PRICE" }
        assertEquals("Double", priceCol?.javaType)
    }

    @Test
    fun smallmoneyMapsToDouble() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val col = typeTest.columns.find { it.databaseColumnName == "SMALL_PRICE" }
        assertEquals("Double", col?.javaType)
    }

    @Test
    fun floatMapsToFloat() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val scoreCol = typeTest.columns.find { it.databaseColumnName == "SCORE" }
        assertEquals("Float", scoreCol?.javaType)
    }

    @Test
    fun realMapsToFloat() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val ratingCol = typeTest.columns.find { it.databaseColumnName == "RATING" }
        assertEquals("Float", ratingCol?.javaType)
    }

    @Test
    fun decimalMapsToDouble() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val amountCol = typeTest.columns.find { it.databaseColumnName == "AMOUNT" }
        assertEquals("Double", amountCol?.javaType)
    }

    @Test
    fun bitMapsToString() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val activeCol = typeTest.columns.find { it.databaseColumnName == "IS_ACTIVE" }
        assertEquals("String", activeCol?.javaType)
    }

    @Test
    fun dateMapsToLocalDate() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val dateCol = typeTest.columns.find { it.databaseColumnName == "BIRTH_DATE" }
        assertEquals("LocalDate", dateCol?.javaType)
    }

    @Test
    fun timeMapsToLocalTime() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val timeCol = typeTest.columns.find { it.databaseColumnName == "LOGIN_TIME" }
        assertEquals("LocalTime", timeCol?.javaType)
    }

    @Test
    fun datetime2MapsToLocalDateTime() {
        val user = entities.first { it.entityName == "User" }
        val col = user.columns.find { it.databaseColumnName == "CREATE_DATE" }
        assertEquals("LocalDateTime", col?.javaType)
    }

    @Test
    fun datetimeMapsToLocalDate() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val col = typeTest.columns.find { it.databaseColumnName == "CREATED_AT" }
        assertEquals("LocalDate", col?.javaType)
    }

    @Test
    fun datetimeoffsetMapsToLocalDateTime() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val col = typeTest.columns.find { it.databaseColumnName == "MODIFIED_AT" }
        assertEquals("LocalDateTime", col?.javaType)
    }

    @Test
    fun smalldatetimeMapsToLocalDateTime() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val col = typeTest.columns.find { it.databaseColumnName == "SMALL_DT" }
        assertEquals("LocalDateTime", col?.javaType)
    }

    @Test
    fun ntextMapsToString() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val col = typeTest.columns.find { it.databaseColumnName == "NOTES" }
        assertEquals("String", col?.javaType)
    }

    @Test
    fun imageMapsToString() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val col = typeTest.columns.find { it.databaseColumnName == "PHOTO" }
        assertEquals("String", col?.javaType)
    }

    @Test
    fun xmlMapsToString() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val col = typeTest.columns.find { it.databaseColumnName == "DATA" }
        assertEquals("String", col?.javaType)
    }

    @Test
    fun bigintMapsToLong() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        // bigint column 'ID' becomes PK with Long type
        val pkCol = typeTest.columns.find { it.databaseIdType == "@Id" }
        assertEquals("Long", pkCol?.javaType)
    }

    @Test
    fun intMapsToInteger() {
        val module = entities.first { it.entityName == "Module" }
        val col = module.columns.find { it.databaseColumnName == "MODULE_FLOW_INDEX" }
        assertEquals("Integer", col?.javaType)
    }

    @Test
    fun tinyintMapsToInteger() {
        val user = entities.first { it.entityName == "User" }
        val col = user.columns.find { it.databaseColumnName == "DISCOUNT" }
        assertEquals("Integer", col?.javaType)
    }

    // ── Primary keys ────────────────────────────────────────────────────

    @Test
    fun allEntitiesHaveAPrimaryKey() {
        for (entity in entities) {
            val pk = entity.columns.find { it.databaseIdType == "@Id" }
            assertTrue(pk != null, "${entity.entityName} should have a @Id primary key")
        }
    }

    @Test
    fun primaryKeyStripsIdSuffix() {
        val user = entities.first { it.entityName == "User" }
        val pk = user.columns.find { it.databaseIdType == "@Id" }
        assertEquals("user", pk?.camelCaseFieldName)
    }

    // ── Foreign keys ────────────────────────────────────────────────────

    @Test
    fun activityHasForeignKey() {
        val activity = entities.first { it.entityName == "Activity" }
        val fkColumn = activity.columns.find { it.databaseIdType == "@ManyToOne" }
        assertTrue(fkColumn != null, "Activity should have a foreign key to MODULE")
    }

    @Test
    fun moduleHasTwoForeignKeys() {
        val module = entities.first { it.entityName == "Module" }
        val fkColumns = module.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertEquals(2, fkColumns.size, "Module has FKs to LANGUAGE_LEVEL and VIDEO_CONTENT")
    }

    @Test
    fun languageLevelHasForeignKey() {
        val ll = entities.first { it.entityName == "LanguageLevel" }
        val fkColumns = ll.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertEquals(1, fkColumns.size, "LanguageLevel has FK to LANGUAGE")
    }

    @Test
    fun wordBankHasForeignKey() {
        val wb = entities.first { it.entityName == "WordBank" }
        val fkColumns = wb.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertEquals(1, fkColumns.size, "WordBank has FK to MODULE")
    }

    @Test
    fun userTableHasNoForeignKeys() {
        val user = entities.first { it.entityName == "User" }
        val fkColumns = user.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertTrue(fkColumns.isEmpty(), "USER table has no foreign keys")
    }

    @Test
    fun settingsTableHasNoForeignKeys() {
        val settings = entities.first { it.entityName == "Settings" }
        val fkColumns = settings.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertTrue(fkColumns.isEmpty(), "SETTINGS table has no foreign keys")
    }

    // ── Noise filtering ─────────────────────────────────────────────────

    @Test
    fun setStatementsDoNotCreateEntities() {
        val names = entities.map { it.entityName }
        assertTrue(!names.contains("AnsiNulls"))
        assertTrue(!names.contains("QuotedIdentifier"))
    }

    @Test
    fun goStatementsDoNotCreateEntities() {
        // GO is a batch separator, not a table
        assertTrue(entities.size == 9)
    }

    // ── File generation ─────────────────────────────────────────────────

    @Test
    fun writesEntityFiles() {
        Globals.PACKAGE = "com/test/mssql"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val entityDir = File(tempDir.toFile(), "com/test/mssql/entity")
        assertTrue(entityDir.exists())
        val entityFiles = entityDir.listFiles()?.map { it.name } ?: emptyList()
        assertTrue(entityFiles.contains("User.java"))
        assertTrue(entityFiles.contains("Activity.java"))
        assertTrue(entityFiles.contains("Language.java"))
        assertTrue(entityFiles.contains("TypeTest.java"))
    }

    @Test
    fun generatedEntityFileHasCorrectContent() {
        Globals.PACKAGE = "com/test/mssql"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val userFile = File(tempDir.toFile(), "com/test/mssql/entity/User.java")
        val content = userFile.readText()
        assertContains(content, "package com.test.mssql.entity")
        assertContains(content, "@Entity")
        assertContains(content, "@Id")
        assertContains(content, "private String name;")
        assertContains(content, "private String email;")
    }

    @Test
    fun generatedEntityWithForeignKeyHasJoinColumn() {
        Globals.PACKAGE = "com/test/mssql"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val activityFile = File(tempDir.toFile(), "com/test/mssql/entity/Activity.java")
        val content = activityFile.readText()
        assertContains(content, "@ManyToOne")
        assertContains(content, "@JoinColumn")
    }

    // ── Kotlin mode ─────────────────────────────────────────────────────

    @Test
    fun kotlinModeWithMssql() {
        Globals.DB_TYPE = "mssql"
        Globals.LANGUAGE = "kotlin"
        val words = mutableListOf<String>()
        val inputStream = javaClass.classLoader.getResourceAsStream("mssql_dump.sql")!!
        Util.getWords(words, inputStream)
        Util.normalizeMssqlWords(words)
        val entityList = mutableListOf<Entity>()
        Util.parseWords(entityList, words)

        val typeTest = entityList.first { it.entityName == "TypeTest" }
        val bitCol = typeTest.columns.find { it.databaseColumnName == "IS_ACTIVE" }
        assertEquals("Boolean", bitCol?.javaType)

        val module = entityList.first { it.entityName == "Module" }
        val intCol = module.columns.find { it.databaseColumnName == "MODULE_FLOW_INDEX" }
        assertEquals("Int", intCol?.javaType)

        Globals.LANGUAGE = "java"
    }
}
