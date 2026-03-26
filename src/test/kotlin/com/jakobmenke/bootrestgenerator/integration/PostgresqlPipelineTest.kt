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

class PostgresqlPipelineTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var entities: List<Entity>
    private var originalDbType = "mysql"
    private var originalLanguage = "java"

    @BeforeEach
    fun setUp() {
        originalDbType = Globals.DB_TYPE
        originalLanguage = Globals.LANGUAGE
        Globals.DB_TYPE = "postgresql"
        Globals.LANGUAGE = "java"
        val words = mutableListOf<String>()
        val inputStream = javaClass.classLoader.getResourceAsStream("pg_dump.sql")!!
        Util.getWords(words, inputStream)
        Util.normalizePostgresqlWords(words)
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
    fun parsesAllTablesFromPgDump() {
        val names = entities.map { it.entityName }
        assertTrue(names.contains("Activity"))
        assertTrue(names.contains("Language"))
        assertTrue(names.contains("LanguageLevel"))
        assertTrue(names.contains("Module"))
        assertTrue(names.contains("User"))
        assertTrue(names.contains("VideoContent"))
        assertTrue(names.contains("Payment"))
        assertTrue(names.contains("Settings"))
        assertTrue(names.contains("WordBank"))
        assertTrue(names.contains("TypeTest"))
    }

    @Test
    fun totalEntityCount() {
        assertEquals(10, entities.size)
    }

    @Test
    fun schemaQualifiedTableNamesStripped() {
        val activity = entities.first { it.entityName == "Activity" }
        assertEquals("activity", activity.tableName)
    }

    @Test
    fun quotedTableNameHandled() {
        val user = entities.first { it.entityName == "User" }
        assertEquals("user", user.tableName)
    }

    // ── Column parsing ──────────────────────────────────────────────────

    @Test
    fun activityTableHasCorrectColumns() {
        val activity = entities.first { it.entityName == "Activity" }
        val colNames = activity.columns.map { it.databaseColumnName }
        assertTrue(colNames.contains("activity_id"))
        assertTrue(colNames.contains("module_id"))
        assertTrue(colNames.contains("activity_table"))
        assertTrue(colNames.contains("name"))
        assertTrue(colNames.contains("description"))
        assertTrue(colNames.contains("active"))
        assertTrue(colNames.contains("create_date"))
        assertTrue(colNames.contains("update_date"))
    }

    @Test
    fun userTableHasAllColumns() {
        val user = entities.first { it.entityName == "User" }
        val colNames = user.columns.map { it.databaseColumnName }
        assertTrue(colNames.contains("user_id"))
        assertTrue(colNames.contains("name"))
        assertTrue(colNames.contains("email"))
        assertTrue(colNames.contains("pw_hash"))
        assertTrue(colNames.contains("discount"))
        assertTrue(colNames.contains("create_date"))
        assertTrue(colNames.contains("update_date"))
    }

    // ── PostgreSQL type mapping ─────────────────────────────────────────

    @Test
    fun characterVaryingMapsToString() {
        val activity = entities.first { it.entityName == "Activity" }
        val nameCol = activity.columns.find { it.databaseColumnName == "name" }
        assertEquals("String", nameCol?.javaType)
    }

    @Test
    fun textMapsToString() {
        val activity = entities.first { it.entityName == "Activity" }
        val descCol = activity.columns.find { it.databaseColumnName == "description" }
        assertEquals("String", descCol?.javaType)
    }

    @Test
    fun integerMapsToInteger() {
        val activity = entities.first { it.entityName == "Activity" }
        val col = activity.columns.find { it.databaseColumnName == "activity_id" }
        // Before PK assignment, the original type should have been integer → Integer
        // After PK, it becomes Long. Check a non-PK integer column instead.
        val module = entities.first { it.entityName == "Module" }
        val flowCol = module.columns.find { it.databaseColumnName == "module_flow_index" }
        assertEquals("Integer", flowCol?.javaType)
    }

    @Test
    fun smallintMapsToInteger() {
        val user = entities.first { it.entityName == "User" }
        val discountCol = user.columns.find { it.databaseColumnName == "discount" }
        assertEquals("Integer", discountCol?.javaType)
    }

    @Test
    fun timestampWithoutTimeZoneMapsToLocalDateTime() {
        val activity = entities.first { it.entityName == "Activity" }
        val col = activity.columns.find { it.databaseColumnName == "create_date" }
        assertEquals("LocalDateTime", col?.javaType)
    }

    @Test
    fun typeTestBigserialMapsToLong() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        // bigserial column 'id' — before PK assignment it was Long, after PK it stays Long
        val idCol = typeTest.columns.find { it.databaseIdType == "@Id" }
        assertEquals("Long", idCol?.javaType)
    }

    @Test
    fun typeTestTextMapsToString() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val nameCol = typeTest.columns.find { it.databaseColumnName == "name" }
        assertEquals("String", nameCol?.javaType)
    }

    @Test
    fun typeTestDoublePrecisionMapsToDouble() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val scoreCol = typeTest.columns.find { it.databaseColumnName == "score" }
        assertEquals("Double", scoreCol?.javaType)
    }

    @Test
    fun typeTestRealMapsToFloat() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val ratingCol = typeTest.columns.find { it.databaseColumnName == "rating" }
        assertEquals("Float", ratingCol?.javaType)
    }

    @Test
    fun typeTestNumericMapsToDouble() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val amountCol = typeTest.columns.find { it.databaseColumnName == "amount" }
        assertEquals("Double", amountCol?.javaType)
    }

    @Test
    fun typeTestBooleanMapsToString() {
        // In Java mode, boolean maps to String (same as bit)
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val activeCol = typeTest.columns.find { it.databaseColumnName == "is_active" }
        assertEquals("String", activeCol?.javaType)
    }

    @Test
    fun typeTestDateMapsToLocalDate() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val dateCol = typeTest.columns.find { it.databaseColumnName == "birth_date" }
        assertEquals("LocalDate", dateCol?.javaType)
    }

    @Test
    fun typeTestTimeMapsToLocalTime() {
        val typeTest = entities.first { it.entityName == "TypeTest" }
        val timeCol = typeTest.columns.find { it.databaseColumnName == "login_time" }
        assertEquals("LocalTime", timeCol?.javaType)
    }

    // ── Primary keys via ALTER TABLE ────────────────────────────────────

    @Test
    fun activityHasPrimaryKey() {
        val activity = entities.first { it.entityName == "Activity" }
        val pkColumn = activity.columns.find { it.databaseIdType == "@Id" }
        assertTrue(pkColumn != null, "Activity should have a primary key")
        assertEquals("Long", pkColumn.javaType)
    }

    @Test
    fun allEntitiesHaveAPrimaryKey() {
        for (entity in entities) {
            val pk = entity.columns.find { it.databaseIdType == "@Id" }
            assertTrue(pk != null, "${entity.entityName} should have a @Id primary key")
        }
    }

    // ── Foreign keys via ALTER TABLE ────────────────────────────────────

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

    // ── Name conversion ─────────────────────────────────────────────────

    @Test
    fun snakeCaseTableNamesConvertedCorrectly() {
        assertEquals("LanguageLevel", entities.first { it.tableName == "language_level" }.entityName)
        assertEquals("VideoContent", entities.first { it.tableName == "video_content" }.entityName)
        assertEquals("WordBank", entities.first { it.tableName == "word_bank" }.entityName)
        assertEquals("TypeTest", entities.first { it.tableName == "type_test" }.entityName)
    }

    @Test
    fun columnNamesCamelCased() {
        val activity = entities.first { it.entityName == "Activity" }
        val col = activity.columns.find { it.databaseColumnName == "activity_table" }
        assertEquals("activityTable", col?.camelCaseFieldName)
    }

    // ── File generation ─────────────────────────────────────────────────

    @Test
    fun writesEntityFiles() {
        Globals.PACKAGE = "com/test/pg"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val entityDir = File(tempDir.toFile(), "com/test/pg/entity")
        assertTrue(entityDir.exists())
        val entityFiles = entityDir.listFiles()?.map { it.name } ?: emptyList()
        assertTrue(entityFiles.contains("User.java"))
        assertTrue(entityFiles.contains("Activity.java"))
        assertTrue(entityFiles.contains("Language.java"))
        assertTrue(entityFiles.contains("TypeTest.java"))
    }

    @Test
    fun generatedEntityFileHasCorrectContent() {
        Globals.PACKAGE = "com/test/pg"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val userFile = File(tempDir.toFile(), "com/test/pg/entity/User.java")
        val content = userFile.readText()
        assertContains(content, "package com.test.pg.entity")
        assertContains(content, "@Entity")
        assertContains(content, "@Id")
        assertContains(content, "private String name;")
        assertContains(content, "private String email;")
    }

    @Test
    fun generatedEntityWithForeignKeyHasJoinColumn() {
        Globals.PACKAGE = "com/test/pg"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val activityFile = File(tempDir.toFile(), "com/test/pg/entity/Activity.java")
        val content = activityFile.readText()
        assertContains(content, "@ManyToOne")
        assertContains(content, "@JoinColumn")
    }

    // ── Kotlin mode ─────────────────────────────────────────────────────

    @Test
    fun kotlinModeWithPostgresql() {
        // Re-parse in Kotlin mode
        Globals.DB_TYPE = "postgresql"
        Globals.LANGUAGE = "kotlin"
        val words = mutableListOf<String>()
        val inputStream = javaClass.classLoader.getResourceAsStream("pg_dump.sql")!!
        Util.getWords(words, inputStream)
        Util.normalizePostgresqlWords(words)
        val entityList = mutableListOf<Entity>()
        Util.parseWords(entityList, words)

        val typeTest = entityList.first { it.entityName == "TypeTest" }
        val activeCol = typeTest.columns.find { it.databaseColumnName == "is_active" }
        assertEquals("Boolean", activeCol?.javaType)

        val module = entityList.first { it.entityName == "Module" }
        val flowCol = module.columns.find { it.databaseColumnName == "module_flow_index" }
        assertEquals("Int", flowCol?.javaType)

        // Reset
        Globals.LANGUAGE = "java"
    }
}
