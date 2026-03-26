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

class GroovyContentValidationTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var entities: List<Entity>

    @BeforeEach
    fun setUp() {
        Globals.LANGUAGE = "groovy"
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

    // ── No Java or Kotlin artifacts in Groovy output ─────────────────────

    @Test
    fun noJavaOrKotlinFilesGenerated() {
        val base = File(tempDir.toFile(), "com/test/app")
        val allFiles = base.walkTopDown().filter { it.isFile }.toList()
        assertFalse(allFiles.any { it.name.endsWith(".java") }, "No .java files should exist")
        assertFalse(allFiles.any { it.name.endsWith(".kt") }, "No .kt files should exist")
        assertTrue(allFiles.all { it.name.endsWith(".groovy") }, "All files should be .groovy")
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

    // ── Groovy-specific content checks on generated files ────────────────

    @Test
    fun allEntityFilesUseGroovyClassSyntax() {
        val entityDir = File(tempDir.toFile(), "com/test/app/entity")
        for (file in entityDir.listFiles()!!) {
            val content = file.readText()
            assertFalse(content.contains("public class"), "File ${file.name} should not contain 'public class'")
            assertContains(content, "implements Serializable")
            assertContains(content, "@Canonical")
        }
    }

    @Test
    fun allEntityFilesUseGroovyFieldSyntax() {
        val entityDir = File(tempDir.toFile(), "com/test/app/entity")
        for (file in entityDir.listFiles()!!) {
            val content = file.readText()
            assertFalse(content.contains("private String"), "File ${file.name} should not have Java private field syntax")
            assertFalse(content.contains("private Integer"), "File ${file.name} should not have Java private field syntax")
            assertFalse(content.contains("private Long"), "File ${file.name} should not have Java private field syntax")
            assertFalse(content.contains("var "), "File ${file.name} should not have Kotlin var syntax")
        }
    }

    @Test
    fun noKotlinSyntaxInAnyFile() {
        val base = File(tempDir.toFile(), "com/test/app")
        val allFiles = base.walkTopDown().filter { it.isFile }.toList()
        for (file in allFiles) {
            val content = file.readText()
            assertFalse(content.contains("fun "), "File ${file.name} should not contain Kotlin 'fun' keyword")
            assertFalse(content.contains("override "), "File ${file.name} should not contain Kotlin 'override' keyword")
            assertFalse(content.contains("private val "), "File ${file.name} should not contain Kotlin 'private val'")
            assertFalse(content.contains("const val "), "File ${file.name} should not contain Kotlin 'const val'")
            assertFalse(content.contains("object "), "File ${file.name} should not contain Kotlin 'object' keyword")
        }
    }

    @Test
    fun allRestFilesUseAutowired() {
        val restDir = File(tempDir.toFile(), "com/test/app/rest")
        for (file in restDir.listFiles()!!) {
            val content = file.readText()
            assertContains(content, "@Autowired")
        }
    }

    @Test
    fun allDaoFilesUseAutowired() {
        val daoDir = File(tempDir.toFile(), "com/test/app/dao")
        for (file in daoDir.listFiles()!!) {
            if (file.name == "GenericDao.groovy") continue
            val content = file.readText()
            assertContains(content, "@Autowired")
        }
    }

    @Test
    fun allDaoFilesImplementGenericDao() {
        val daoDir = File(tempDir.toFile(), "com/test/app/dao")
        for (file in daoDir.listFiles()!!) {
            if (file.name == "GenericDao.groovy") continue
            val content = file.readText()
            assertContains(content, "implements GenericDao<")
        }
    }

    @Test
    fun allRepositoryFilesUseExtendsNotColon() {
        val repoDir = File(tempDir.toFile(), "com/test/app/repository")
        for (file in repoDir.listFiles()!!) {
            val content = file.readText()
            assertContains(content, "extends JpaRepository<")
            assertFalse(content.contains(": JpaRepository"), "File ${file.name} should use 'extends' not ':'")
        }
    }

    // ── Specific entity content deep checks ──────────────────────────────

    @Test
    fun activityEntityHasCorrectGroovyContent() {
        val content = readFile("entity", "Activity.groovy")
        assertContains(content, "package com.test.app.entity")
        assertContains(content, "@Entity")
        assertContains(content, "@Table(name = \"ACTIVITY\")")
        assertContains(content, "@Id")
        assertContains(content, "@ManyToOne")
        assertContains(content, "@JoinColumn")
        assertContains(content, "String name")
        assertContains(content, "@Canonical")
    }

    @Test
    fun activityResourceHasCorrectGroovyContent() {
        val content = readFile("rest", "ActivityResource.groovy")
        assertContains(content, "class ActivityResource")
        assertContains(content, "List<Activity> readAll()")
        assertContains(content, "Activity read(")
        assertContains(content, "Activity create(")
        assertContains(content, "Activity update(")
        assertContains(content, "@Autowired")
    }

    @Test
    fun activityDaoHasCorrectGroovyContent() {
        val content = readFile("dao", "ActivityDao.groovy")
        assertContains(content, "class ActivityDao implements GenericDao<Activity>")
        assertContains(content, "@Autowired")
    }

    @Test
    fun activityRepositoryHasCorrectGroovyContent() {
        val content = readFile("repository", "ActivityRepository.groovy")
        assertContains(content, "interface ActivityRepository extends JpaRepository<Activity, Long>")
    }

    @Test
    fun completedEntityHasMultipleForeignKeys() {
        val content = readFile("entity", "Completed.groovy")
        val manyToOneCount = content.split("@ManyToOne").size - 1
        assertTrue(manyToOneCount >= 3, "Completed should have at least 3 @ManyToOne annotations, found $manyToOneCount")
    }

    @Test
    fun languageLevelEntityHasCorrectName() {
        val content = readFile("entity", "LanguageLevel.groovy")
        assertContains(content, "class LanguageLevel implements Serializable")
        assertContains(content, "@Table(name = \"LANGUAGE_LEVEL\")")
    }

    @Test
    fun paymentMethodInfoResourceHasCorrectPath() {
        val content = readFile("rest", "PaymentMethodInfoResource.groovy")
        assertContains(content, "\"/paymentmethodinfo\"")
    }

    @Test
    fun constantsFileHasCorrectGroovyContent() {
        val content = readFile("utils", "GlobalConstants.groovy")
        assertContains(content, "class GlobalConstants")
        assertContains(content, "static final String CONTEXT_PATH = \"/api/v1\"")
        assertFalse(content.contains("object "))
        assertFalse(content.contains("const val"))
    }

    @Test
    fun genericDaoFileHasCorrectGroovyContent() {
        val content = readFile("dao", "GenericDao.groovy")
        assertContains(content, "interface GenericDao<T>")
        assertContains(content, "List<T> findAll()")
        assertContains(content, "T findOne(Long id)")
        assertContains(content, "T save(T t)")
        assertContains(content, "void delete(Long id)")
    }
}
