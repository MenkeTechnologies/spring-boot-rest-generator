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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinContentValidationTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var entities: List<Entity>

    @BeforeEach
    fun setUp() {
        Globals.LANGUAGE = "kotlin"
        Globals.PACKAGE = "com/test/app"
        Globals.SRC_FOLDER = tempDir.toString()

        val words = mutableListOf<String>()
        val inputStream = javaClass.classLoader.getResourceAsStream("dump.sql")!!
        Util.getWords(words, inputStream)
        val entityList = mutableListOf<Entity>()
        Util.parseWords(entityList, words)
        entities = entityList

        Main().writeTemplates(entities)
    }

    @AfterEach
    fun tearDown() {
        Globals.LANGUAGE = "java"
    }

    private fun readFile(subDir: String, fileName: String): String {
        return File(tempDir.toFile(), "com/test/app/$subDir/$fileName").readText()
    }

    // ── No Java artifacts in Kotlin output ──────────────────────────────

    @Test
    fun noJavaFilesGenerated() {
        val base = File(tempDir.toFile(), "com/test/app")
        val allFiles = base.walkTopDown().filter { it.isFile }.toList()
        assertFalse(allFiles.any { it.name.endsWith(".java") }, "No .java files should exist")
        assertTrue(allFiles.all { it.name.endsWith(".kt") }, "All files should be .kt")
    }

    @Test
    fun noLombokInAnyFile() {
        val base = File(tempDir.toFile(), "com/test/app")
        val allFiles = base.walkTopDown().filter { it.isFile }.toList()
        for (file in allFiles) {
            val content = file.readText()
            assertFalse(content.contains("lombok"), "File ${file.name} should not contain lombok")
            assertFalse(content.contains("@Data"), "File ${file.name} should not contain @Data")
        }
    }

    @Test
    fun noPlaceholdersInAnyEntityFile() {
        val entityDir = File(tempDir.toFile(), "com/test/app/entity")
        for (file in entityDir.listFiles()!!) {
            val content = file.readText()
            assertFalse(content.contains("{{"), "File ${file.name} should not contain {{")
            assertFalse(content.contains("}}"), "File ${file.name} should not contain }}")
        }
    }

    @Test
    fun noPlaceholdersInAnyRestFile() {
        val restDir = File(tempDir.toFile(), "com/test/app/rest")
        for (file in restDir.listFiles()!!) {
            val content = file.readText()
            assertFalse(content.contains("{{"), "File ${file.name} should not contain {{")
        }
    }

    @Test
    fun noPlaceholdersInAnyDaoFile() {
        val daoDir = File(tempDir.toFile(), "com/test/app/dao")
        for (file in daoDir.listFiles()!!) {
            val content = file.readText()
            assertFalse(content.contains("{{"), "File ${file.name} should not contain {{")
        }
    }

    @Test
    fun noPlaceholdersInAnyRepositoryFile() {
        val repoDir = File(tempDir.toFile(), "com/test/app/repository")
        for (file in repoDir.listFiles()!!) {
            val content = file.readText()
            assertFalse(content.contains("{{"), "File ${file.name} should not contain {{")
        }
    }

    // ── Kotlin-specific content checks on generated files ───────────────

    @Test
    fun allEntityFilesUseKotlinClassSyntax() {
        val entityDir = File(tempDir.toFile(), "com/test/app/entity")
        for (file in entityDir.listFiles()!!) {
            val content = file.readText()
            assertFalse(content.contains("public class"), "File ${file.name} should not contain 'public class'")
            assertContains(content, ": Serializable")
        }
    }

    @Test
    fun allEntityFilesUseVarNotPrivate() {
        val entityDir = File(tempDir.toFile(), "com/test/app/entity")
        for (file in entityDir.listFiles()!!) {
            val content = file.readText()
            assertFalse(content.contains("private String"), "File ${file.name} should not have Java field syntax")
            assertFalse(content.contains("private Integer"), "File ${file.name} should not have Java field syntax")
            assertFalse(content.contains("private Long"), "File ${file.name} should not have Java field syntax")
        }
    }

    @Test
    fun allRestFilesUseConstructorInjection() {
        val restDir = File(tempDir.toFile(), "com/test/app/rest")
        for (file in restDir.listFiles()!!) {
            val content = file.readText()
            assertFalse(content.contains("@Autowired"), "File ${file.name} should not use @Autowired")
            assertContains(content, "private val dao:")
        }
    }

    @Test
    fun allDaoFilesUseConstructorInjection() {
        val daoDir = File(tempDir.toFile(), "com/test/app/dao")
        for (file in daoDir.listFiles()!!) {
            if (file.name == "GenericDao.kt") continue
            val content = file.readText()
            assertFalse(content.contains("@Autowired"), "File ${file.name} should not use @Autowired")
            assertContains(content, "private val")
        }
    }

    @Test
    fun allDaoFilesUseOverride() {
        val daoDir = File(tempDir.toFile(), "com/test/app/dao")
        for (file in daoDir.listFiles()!!) {
            if (file.name == "GenericDao.kt") continue
            val content = file.readText()
            assertContains(content, "override fun findAll()")
            assertContains(content, "override fun save(")
        }
    }

    @Test
    fun allRepositoryFilesUseColonNotExtends() {
        val repoDir = File(tempDir.toFile(), "com/test/app/repository")
        for (file in repoDir.listFiles()!!) {
            val content = file.readText()
            assertFalse(content.contains("extends"), "File ${file.name} should not use 'extends'")
            assertContains(content, ": JpaRepository<")
        }
    }

    // ── Specific entity content deep checks ─────────────────────────────

    @Test
    fun activityEntityHasCorrectKotlinContent() {
        val content = readFile("entity", "Activity.kt")
        assertContains(content, "package com.test.app.entity")
        assertContains(content, "@Entity")
        assertContains(content, "@Table(name = \"ACTIVITY\")")
        assertContains(content, "@Id")
        assertContains(content, "@ManyToOne")
        assertContains(content, "@JoinColumn")
        assertContains(content, "var name: String = \"\"")
    }

    @Test
    fun activityResourceHasCorrectKotlinContent() {
        val content = readFile("rest", "ActivityResource.kt")
        assertContains(content, "class ActivityResource(private val dao: ActivityDao)")
        assertContains(content, "fun readAll(): List<Activity>")
        assertContains(content, "fun read(@PathVariable(\"id\") id: Long): Activity?")
        assertContains(content, "fun create(@RequestBody entity: Activity): Activity")
        assertContains(content, "fun update(@RequestBody entity: Activity): Activity")
    }

    @Test
    fun activityDaoHasCorrectKotlinContent() {
        val content = readFile("dao", "ActivityDao.kt")
        assertContains(content, "class ActivityDao(private val activityRepository: ActivityRepository) : GenericDao<Activity>")
    }

    @Test
    fun activityRepositoryHasCorrectKotlinContent() {
        val content = readFile("repository", "ActivityRepository.kt")
        assertContains(content, "interface ActivityRepository : JpaRepository<Activity, Long>")
    }

    @Test
    fun completedEntityHasMultipleForeignKeys() {
        val content = readFile("entity", "Completed.kt")
        val manyToOneCount = content.split("@ManyToOne").size - 1
        assertTrue(manyToOneCount >= 3, "Completed should have at least 3 @ManyToOne annotations, found $manyToOneCount")
    }

    @Test
    fun userEntityHasNoSemicolons() {
        val content = readFile("entity", "User.kt")
        assertFalse(content.contains(";"), "Kotlin entity should not have semicolons")
    }

    @Test
    fun languageLevelEntityHasCorrectName() {
        val content = readFile("entity", "LanguageLevel.kt")
        assertContains(content, "class LanguageLevel : Serializable")
        assertContains(content, "@Table(name = \"LANGUAGE_LEVEL\")")
    }

    @Test
    fun paymentMethodInfoResourceHasCorrectPath() {
        val content = readFile("rest", "PaymentMethodInfoResource.kt")
        assertContains(content, "\"/paymentmethodinfo\"")
    }
}
