package com.jakobmenke.bootrestgenerator.integration

import com.jakobmenke.bootrestgenerator.Main
import com.jakobmenke.bootrestgenerator.dto.Entity
import com.jakobmenke.bootrestgenerator.utils.Configuration
import com.jakobmenke.bootrestgenerator.utils.Globals
import com.jakobmenke.bootrestgenerator.utils.Util
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FullPipelineTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var entities: List<Entity>

    @BeforeEach
    fun setUp() {
        val words = mutableListOf<String>()
        val inputStream = javaClass.classLoader.getResourceAsStream("mysql_dump.sql")!!
        Util.getWords(words, inputStream)
        val entityList = mutableListOf<Entity>()
        Util.parseWords(entityList, words)
        entities = entityList
    }

    // ── Full SQL parsing integration ───────────────────────────────────

    @Test
    fun parsesAllTablesFromDumpSql() {
        val names = entities.map { it.entityName }
        assertTrue(names.contains("Activity"))
        assertTrue(names.contains("AlphabetCharacter"))
        assertTrue(names.contains("AudioContent"))
        assertTrue(names.contains("Completed"))
        assertTrue(names.contains("EmailBuffer"))
        assertTrue(names.contains("Language"))
        assertTrue(names.contains("LanguageLevel"))
        assertTrue(names.contains("Module"))
        assertTrue(names.contains("Notifications"))
        assertTrue(names.contains("Payment"))
        assertTrue(names.contains("PaymentMethodInfo"))
        assertTrue(names.contains("Settings"))
        assertTrue(names.contains("Subscription"))
        assertTrue(names.contains("Transcript"))
        assertTrue(names.contains("Translation"))
        assertTrue(names.contains("User"))
        assertTrue(names.contains("VideoContent"))
        assertTrue(names.contains("WordBank"))
    }

    @Test
    fun totalEntityCount() {
        assertEquals(18, entities.size)
    }

    @Test
    fun activityTableHasCorrectColumns() {
        val activity = entities.first { it.entityName == "Activity" }
        assertEquals("ACTIVITY", activity.tableName)
        val colNames = activity.columns.map { it.databaseColumnName }
        assertTrue(colNames.contains("ACTIVITY_ID") || colNames.any { it == "ACTIVITY_ID" })
        assertTrue(colNames.contains("ACTIVITY_TABLE"))
        assertTrue(colNames.contains("NAME"))
        assertTrue(colNames.contains("DESCRIPTION"))
        assertTrue(colNames.contains("ACTIVE"))
        assertTrue(colNames.contains("CREATE_DATE"))
        assertTrue(colNames.contains("UPDATE_DATE"))
    }

    @Test
    fun activityHasPrimaryKey() {
        val activity = entities.first { it.entityName == "Activity" }
        val pkColumn = activity.columns.find { it.databaseIdType == "@Id" }
        assertTrue(pkColumn != null, "Activity should have a primary key")
        assertEquals("Long", pkColumn.javaType)
    }

    @Test
    fun activityHasForeignKey() {
        val activity = entities.first { it.entityName == "Activity" }
        val fkColumn = activity.columns.find { it.databaseIdType == "@ManyToOne" }
        assertTrue(fkColumn != null, "Activity should have a foreign key to MODULE")
    }

    @Test
    fun userTableHasAllColumns() {
        val user = entities.first { it.entityName == "User" }
        val colNames = user.columns.map { it.databaseColumnName }
        assertTrue(colNames.contains("USER_ID") || user.columns.any { it.databaseIdType == "@Id" })
        assertTrue(colNames.contains("NAME"))
        assertTrue(colNames.contains("EMAIL"))
        assertTrue(colNames.contains("PW_HASH"))
        assertTrue(colNames.contains("CREATE_DATE"))
        assertTrue(colNames.contains("UPDATE_DATE"))
    }

    @Test
    fun userTableHasNoForeignKeys() {
        val user = entities.first { it.entityName == "User" }
        val fkColumns = user.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertTrue(fkColumns.isEmpty(), "USER table has no foreign keys")
    }

    @Test
    fun completedTableHasMultipleForeignKeys() {
        val completed = entities.first { it.entityName == "Completed" }
        val fkColumns = completed.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertTrue(fkColumns.size >= 3, "COMPLETED has at least 3 foreign keys (MODULE, USER, ACTIVITY)")
    }

    @Test
    fun languageTableColumnsHaveCorrectTypes() {
        val language = entities.first { it.entityName == "Language" }
        val nameCol = language.columns.find { it.databaseColumnName == "NAME" }
        assertEquals("String", nameCol?.javaType)
        val createDateCol = language.columns.find { it.databaseColumnName == "CREATE_DATE" }
        assertEquals("LocalDate", createDateCol?.javaType)
        val updateDateCol = language.columns.find { it.databaseColumnName == "UPDATE_DATE" }
        assertEquals("LocalDateTime", updateDateCol?.javaType)
    }

    @Test
    fun settingsTableHasNoPrimaryKeyOrForeignKeyMisidentified() {
        val settings = entities.first { it.entityName == "Settings" }
        val pk = settings.columns.find { it.databaseIdType == "@Id" }
        assertTrue(pk != null, "SETTINGS should have a primary key")
        val fk = settings.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertTrue(fk.isEmpty(), "SETTINGS has no foreign keys")
    }

    @Test
    fun emailBufferTableHasNoForeignKeys() {
        val emailBuffer = entities.first { it.entityName == "EmailBuffer" }
        val fk = emailBuffer.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertTrue(fk.isEmpty(), "EMAIL_BUFFER has no FK constraints")
    }

    @Test
    fun moduleTableHasTwoForeignKeys() {
        val module = entities.first { it.entityName == "Module" }
        val fkColumns = module.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertEquals(2, fkColumns.size, "MODULE has FKs to LANGUAGE_LEVEL and VIDEO_CONTENT")
    }

    @Test
    fun wordBankTableHasOneForeignKey() {
        val wordBank = entities.first { it.entityName == "WordBank" }
        val fkColumns = wordBank.columns.filter { it.databaseIdType == "@ManyToOne" }
        assertEquals(1, fkColumns.size, "WORD_BANK has FK to MODULE")
    }

    @Test
    fun translationTableColumnsIncludeSegmentIndex() {
        val translation = entities.first { it.entityName == "Translation" }
        val segCol = translation.columns.find { it.databaseColumnName == "SEGMENT_INDEX" }
        assertEquals("Integer", segCol?.javaType)
        assertEquals("segmentIndex", segCol?.camelCaseFieldName)
    }

    @Test
    fun snakeCaseTableNamesConvertedCorrectly() {
        assertEquals("AudioContent", entities.first { it.tableName == "AUDIO_CONTENT" }.entityName)
        assertEquals("PaymentMethodInfo", entities.first { it.tableName == "PAYMENT_METHOD_INFO" }.entityName)
        assertEquals("LanguageLevel", entities.first { it.tableName == "LANGUAGE_LEVEL" }.entityName)
        assertEquals("VideoContent", entities.first { it.tableName == "VIDEO_CONTENT" }.entityName)
        assertEquals("WordBank", entities.first { it.tableName == "WORD_BANK" }.entityName)
        assertEquals("AlphabetCharacter", entities.first { it.tableName == "ALPHABET_CHARACTER" }.entityName)
    }

    @Test
    fun allEntitiesHaveAtLeastOneColumn() {
        for (entity in entities) {
            assertTrue(entity.columns.isNotEmpty(), "${entity.entityName} should have columns")
        }
    }

    @Test
    fun allEntitiesHaveAPrimaryKey() {
        for (entity in entities) {
            val pk = entity.columns.find { it.databaseIdType == "@Id" }
            assertTrue(pk != null, "${entity.entityName} should have a @Id primary key")
        }
    }

    // ── File generation integration ────────────────────────────────────

    @Test
    fun writesEntityFiles() {
        Globals.PACKAGE = "com/test/app"
        Globals.SRC_FOLDER = tempDir.toString()
        Globals.FILE_NAME = "mysql_dump.sql"

        val main = Main()
        main.writeTemplates(entities)

        val entityDir = File(tempDir.toFile(), "com/test/app/entity")
        assertTrue(entityDir.exists())
        val entityFiles = entityDir.listFiles()?.map { it.name } ?: emptyList()
        assertTrue(entityFiles.contains("User.java"))
        assertTrue(entityFiles.contains("Activity.java"))
        assertTrue(entityFiles.contains("Language.java"))
    }

    @Test
    fun writesRestFiles() {
        Globals.PACKAGE = "com/test/app"
        Globals.SRC_FOLDER = tempDir.toString()
        Globals.FILE_NAME = "mysql_dump.sql"

        val main = Main()
        main.writeTemplates(entities)

        val restDir = File(tempDir.toFile(), "com/test/app/rest")
        assertTrue(restDir.exists())
        val restFiles = restDir.listFiles()?.map { it.name } ?: emptyList()
        assertTrue(restFiles.contains("UserResource.java"))
        assertTrue(restFiles.contains("ActivityResource.java"))
    }

    @Test
    fun writesDaoFiles() {
        Globals.PACKAGE = "com/test/app"
        Globals.SRC_FOLDER = tempDir.toString()
        Globals.FILE_NAME = "mysql_dump.sql"

        val main = Main()
        main.writeTemplates(entities)

        val daoDir = File(tempDir.toFile(), "com/test/app/dao")
        assertTrue(daoDir.exists())
        val daoFiles = daoDir.listFiles()?.map { it.name } ?: emptyList()
        assertTrue(daoFiles.contains("UserDao.java"))
        assertTrue(daoFiles.contains("GenericDao.java"))
    }

    @Test
    fun writesRepositoryFiles() {
        Globals.PACKAGE = "com/test/app"
        Globals.SRC_FOLDER = tempDir.toString()
        Globals.FILE_NAME = "mysql_dump.sql"

        val main = Main()
        main.writeTemplates(entities)

        val repoDir = File(tempDir.toFile(), "com/test/app/repository")
        assertTrue(repoDir.exists())
        val repoFiles = repoDir.listFiles()?.map { it.name } ?: emptyList()
        assertTrue(repoFiles.contains("UserRepository.java"))
    }

    @Test
    fun writesUtilsFiles() {
        Globals.PACKAGE = "com/test/app"
        Globals.SRC_FOLDER = tempDir.toString()
        Globals.FILE_NAME = "mysql_dump.sql"

        val main = Main()
        main.writeTemplates(entities)

        val utilsDir = File(tempDir.toFile(), "com/test/app/utils")
        assertTrue(utilsDir.exists())
        val utilsFiles = utilsDir.listFiles()?.map { it.name } ?: emptyList()
        assertTrue(utilsFiles.contains("GlobalConstants.java"))
    }

    @Test
    fun generatedEntityFileHasCorrectContent() {
        Globals.PACKAGE = "com/test/app"
        Globals.SRC_FOLDER = tempDir.toString()
        Globals.FILE_NAME = "mysql_dump.sql"

        val main = Main()
        main.writeTemplates(entities)

        val userFile = File(tempDir.toFile(), "com/test/app/entity/User.java")
        val content = userFile.readText()
        assertContains(content, "package com.test.app.entity")
        assertContains(content, "@Entity")
        assertContains(content, "@Table(name = \"USER\")")
        assertContains(content, "public class User implements Serializable")
        assertContains(content, "@Id")
        assertContains(content, "private String name;")
        assertContains(content, "private String email;")
    }

    @Test
    fun generatedRestFileHasCorrectContent() {
        Globals.PACKAGE = "com/test/app"
        Globals.SRC_FOLDER = tempDir.toString()
        Globals.FILE_NAME = "mysql_dump.sql"

        val main = Main()
        main.writeTemplates(entities)

        val restFile = File(tempDir.toFile(), "com/test/app/rest/UserResource.java")
        val content = restFile.readText()
        assertContains(content, "package com.test.app.rest")
        assertContains(content, "@RestController")
        assertContains(content, "import com.test.app.entity.User")
        assertContains(content, "import com.test.app.dao.UserDao")
        assertContains(content, "UserDao dao")
    }

    @Test
    fun generatedDaoFileHasCorrectContent() {
        Globals.PACKAGE = "com/test/app"
        Globals.SRC_FOLDER = tempDir.toString()
        Globals.FILE_NAME = "mysql_dump.sql"

        val main = Main()
        main.writeTemplates(entities)

        val daoFile = File(tempDir.toFile(), "com/test/app/dao/UserDao.java")
        val content = daoFile.readText()
        assertContains(content, "@Service")
        assertContains(content, "UserRepository userRepository")
        assertContains(content, "implements GenericDao<User>")
    }

    @Test
    fun generatedRepositoryFileHasCorrectContent() {
        Globals.PACKAGE = "com/test/app"
        Globals.SRC_FOLDER = tempDir.toString()
        Globals.FILE_NAME = "mysql_dump.sql"

        val main = Main()
        main.writeTemplates(entities)

        val repoFile = File(tempDir.toFile(), "com/test/app/repository/UserRepository.java")
        val content = repoFile.readText()
        assertContains(content, "@Repository")
        assertContains(content, "extends JpaRepository<User, Long>")
    }

    @Test
    fun generatedConstantsFileHasCorrectContent() {
        Globals.PACKAGE = "com/test/app"
        Globals.SRC_FOLDER = tempDir.toString()
        Globals.FILE_NAME = "mysql_dump.sql"

        val main = Main()
        main.writeTemplates(entities)

        val constantsFile = File(tempDir.toFile(), "com/test/app/utils/GlobalConstants.java")
        val content = constantsFile.readText()
        assertContains(content, "CONTEXT_PATH = \"/api/v1\"")
    }

    @Test
    fun totalGeneratedFilesPerEntity() {
        Globals.PACKAGE = "com/test/app"
        Globals.SRC_FOLDER = tempDir.toString()
        Globals.FILE_NAME = "mysql_dump.sql"

        val main = Main()
        main.writeTemplates(entities)

        // 4 files per entity + 2 shared (GenericDao, GlobalConstants)
        val entityCount = entities.size
        val entityFiles = File(tempDir.toFile(), "com/test/app/entity").listFiles()?.size ?: 0
        val restFiles = File(tempDir.toFile(), "com/test/app/rest").listFiles()?.size ?: 0
        val daoFiles = File(tempDir.toFile(), "com/test/app/dao").listFiles()?.size ?: 0
        val repoFiles = File(tempDir.toFile(), "com/test/app/repository").listFiles()?.size ?: 0
        val utilsFiles = File(tempDir.toFile(), "com/test/app/utils").listFiles()?.size ?: 0

        assertEquals(entityCount, entityFiles)
        assertEquals(entityCount, restFiles)
        assertEquals(entityCount + 1, daoFiles) // +1 for GenericDao.java
        assertEquals(entityCount, repoFiles)
        assertEquals(1, utilsFiles) // GlobalConstants.java
    }

    @Test
    fun entityWithForeignKeyGeneratesJoinColumn() {
        Globals.PACKAGE = "com/test/app"
        Globals.SRC_FOLDER = tempDir.toString()
        Globals.FILE_NAME = "mysql_dump.sql"

        val main = Main()
        main.writeTemplates(entities)

        val activityFile = File(tempDir.toFile(), "com/test/app/entity/Activity.java")
        val content = activityFile.readText()
        assertContains(content, "@ManyToOne")
        assertContains(content, "@JoinColumn")
    }

    // ── Configuration integration ──────────────────────────────────────

    @Test
    fun configurationLoadsAndParsesCorrectly() {
        val props = Configuration.readConfig("config.properties")!!
        val config = Configuration(props)
        assertEquals("mysql_dump.sql", config.fileName)
        assertEquals("com/example/generated", config.targetPackage)
        assertTrue(config.srcFolder.isNotEmpty())
    }
}
