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

class SqlitePipelineTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var entities: List<Entity>
    private var originalDbType = "mysql"
    private var originalLanguage = "java"

    @BeforeEach
    fun setUp() {
        originalDbType = Globals.DB_TYPE
        originalLanguage = Globals.LANGUAGE
        Globals.DB_TYPE = "sqlite"
        Globals.LANGUAGE = "java"
        val words = mutableListOf<String>()
        val inputStream = javaClass.classLoader.getResourceAsStream("sqlite_dump.sql")!!
        Util.getWords(words, inputStream)
        Util.normalizeSqliteWords(words)
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
    fun parsesAllTablesFromSqliteDump() {
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
    fun quotedTableNameStripped() {
        val user = entities.first { it.entityName == "User" }
        assertEquals("user", user.tableName)
    }

    @Test
    fun snakeCaseTableNamesConvertedCorrectly() {
        assertEquals("LanguageLevel", entities.first { it.tableName == "language_level" }.entityName)
        assertEquals("VideoContent", entities.first { it.tableName == "video_content" }.entityName)
        assertEquals("WordBank", entities.first { it.tableName == "word_bank" }.entityName)
        assertEquals("TypeTest", entities.first { it.tableName == "type_test" }.entityName)
    }

    // ── Column parsing ──────────────────────────────────────────────────

    @Test
    fun userTableHasCorrectColumns() {
        val user = entities.first { it.entityName == "User" }
        val colNames = user.columns.map { it.databaseColumnName }
        assertTrue(colNames.contains("user_id") || user.columns.any { it.databaseIdType == "@Id" })
        assertTrue(colNames.contains("name"))
        assertTrue(colNames.contains("email"))
        assertTrue(colNames.contains("pw_hash"))
        assertTrue(colNames.contains("discount"))
    }

    @Test
    fun activityTableHasCorrectColumns() {
        val activity = entities.first { it.entityName == "Activity" }
        val colNames = activity.columns.map { it.databaseColumnName }
        assertTrue(colNames.contains("activity_id") || activity.columns.any { it.databaseIdType == "@Id" })
        assertTrue(colNames.contains("module_id") || activity.columns.any { it.databaseIdType == "@ManyToOne" })
        assertTrue(colNames.contains("activity_table"))
        assertTrue(colNames.contains("name"))
        assertTrue(colNames.contains("description"))
        assertTrue(colNames.contains("active"))
    }

    @Test
    fun columnNamesCamelCased() {
        val activity = entities.first { it.entityName == "Activity" }
        val col = activity.columns.find { it.databaseColumnName == "activity_table" }
        assertEquals("activityTable", col?.camelCaseFieldName)
    }

    // ── SQLite type mapping ─────────────────────────────────────────────

    @Test
    fun textMapsToString() {
        val user = entities.first { it.entityName == "User" }
        val nameCol = user.columns.find { it.databaseColumnName == "name" }
        assertEquals("String", nameCol?.javaType)
    }

    @Test
    fun integerColumnMapsToInteger() {
        val user = entities.first { it.entityName == "User" }
        val discountCol = user.columns.find { it.databaseColumnName == "discount" }
        assertEquals("Integer", discountCol?.javaType)
    }

    @Test
    fun varcharMapsToString() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val labelCol = typeTest.columns.find { it.databaseColumnName == "label" }
        assertEquals("String", labelCol?.javaType)
    }

    @Test
    fun realMapsToFloat() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val scoreCol = typeTest.columns.find { it.databaseColumnName == "score" }
        assertEquals("Float", scoreCol?.javaType)
    }

    @Test
    fun numericMapsToDouble() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val amountCol = typeTest.columns.find { it.databaseColumnName == "amount" }
        assertEquals("Double", amountCol?.javaType)
    }

    @Test
    fun blobMapsToString() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val blobCol = typeTest.columns.find { it.databaseColumnName == "raw_data" }
        assertEquals("String", blobCol?.javaType)
    }

    // ── Inline PRIMARY KEY ──────────────────────────────────────────────

    @Test
    fun inlinePrimaryKeyDetected() {
        val user = entities.first { it.entityName == "User" }
        val pkColumn = user.columns.find { it.databaseIdType == "@Id" }
        assertTrue(pkColumn != null, "User should have an inline primary key")
        assertEquals("Long", pkColumn.javaType)
    }

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

    // ── FOREIGN KEY constraints ─────────────────────────────────────────

    @Test
    fun activityHasForeignKey() {
        val activity = entities.first { it.entityName == "Activity" }
        val fkColumn = activity.columns.find { it.databaseIdType == "@ManyToOne" }
        assertTrue(fkColumn != null, "Activity should have a foreign key to module")
    }

    @Test
    fun moduleHasTwoForeignKeys() {
        val module = entities.first { it.entityName == "Module" }
        val fkColumns = module.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertEquals(2, fkColumns.size, "Module has FKs to language_level and video_content")
    }

    @Test
    fun languageLevelHasForeignKey() {
        val ll = entities.first { it.entityName == "LanguageLevel" }
        val fkColumns = ll.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertEquals(1, fkColumns.size, "LanguageLevel has FK to language")
    }

    @Test
    fun wordBankHasForeignKey() {
        val wb = entities.first { it.entityName == "WordBank" }
        val fkColumns = wb.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertEquals(1, fkColumns.size, "WordBank has FK to module")
    }

    @Test
    fun userTableHasNoForeignKeys() {
        val user = entities.first { it.entityName == "User" }
        val fkColumns = user.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertTrue(fkColumns.isEmpty(), "user table has no foreign keys")
    }

    @Test
    fun settingsTableHasNoForeignKeys() {
        val settings = entities.first { it.entityName == "Settings" }
        val fkColumns = settings.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertTrue(fkColumns.isEmpty(), "settings table has no foreign keys")
    }

    // ── INSERT/PRAGMA/BEGIN/COMMIT noise filtered ───────────────────────

    @Test
    fun insertStatementsDoNotCreateFalseEntities() {
        // INSERT, PRAGMA, BEGIN, COMMIT, DELETE should be filtered out
        val names = entities.map { it.entityName }
        // sqlite_sequence is referenced in INSERT/DELETE but should NOT become an entity
        assertTrue(!names.contains("SqliteSequence"))
    }

    // ── File generation ─────────────────────────────────────────────────

    @Test
    fun writesEntityFiles() {
        Globals.PACKAGE = "com/test/sqlite"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val entityDir = File(tempDir.toFile(), "com/test/sqlite/entity")
        assertTrue(entityDir.exists())
        val entityFiles = entityDir.listFiles()?.map { it.name } ?: emptyList()
        assertTrue(entityFiles.contains("User.java"))
        assertTrue(entityFiles.contains("Activity.java"))
        assertTrue(entityFiles.contains("Language.java"))
        assertTrue(entityFiles.contains("TypeTest.java"))
    }

    @Test
    fun generatedEntityFileHasCorrectContent() {
        Globals.PACKAGE = "com/test/sqlite"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val userFile = File(tempDir.toFile(), "com/test/sqlite/entity/User.java")
        val content = userFile.readText()
        assertContains(content, "package com.test.sqlite.entity")
        assertContains(content, "@Entity")
        assertContains(content, "@Id")
        assertContains(content, "private String name;")
        assertContains(content, "private String email;")
    }

    @Test
    fun generatedEntityWithForeignKeyHasJoinColumn() {
        Globals.PACKAGE = "com/test/sqlite"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val activityFile = File(tempDir.toFile(), "com/test/sqlite/entity/Activity.java")
        val content = activityFile.readText()
        assertContains(content, "@ManyToOne")
        assertContains(content, "@JoinColumn")
    }

    // ── Kotlin mode ─────────────────────────────────────────────────────

    @Test
    fun kotlinModeWithSqlite() {
        Globals.DB_TYPE = "sqlite"
        Globals.LANGUAGE = "kotlin"
        val words = mutableListOf<String>()
        val inputStream = javaClass.classLoader.getResourceAsStream("sqlite_dump.sql")!!
        Util.getWords(words, inputStream)
        Util.normalizeSqliteWords(words)
        val entityList = mutableListOf<Entity>()
        Util.parseWords(entityList, words)

        val user = entityList.first { it.entityName == "User" }
        val discountCol = user.columns.find { it.databaseColumnName == "discount" }
        assertEquals("Int", discountCol?.javaType)

        Globals.LANGUAGE = "java"
    }
}
