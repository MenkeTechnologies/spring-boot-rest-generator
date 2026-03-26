package com.jakobmenke.bootrestgenerator.integration

import com.jakobmenke.bootrestgenerator.Main
import com.jakobmenke.bootrestgenerator.dto.Entity
import com.jakobmenke.bootrestgenerator.templates.Templates
import com.jakobmenke.bootrestgenerator.utils.Globals
import com.jakobmenke.bootrestgenerator.utils.Util
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Path
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroovyPipelineTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var entities: List<Entity>

    @BeforeEach
    fun setUp() {
        Globals.LANGUAGE = "groovy"
        val words = mutableListOf<String>()
        val inputStream = javaClass.classLoader.getResourceAsStream("mysql_dump.sql")!!
        Util.getWords(words, inputStream)
        val entityList = mutableListOf<Entity>()
        Util.parseWords(entityList, words)
        entities = entityList
    }

    @AfterEach
    fun tearDown() {
        Globals.LANGUAGE = "java"
    }

    // ── Groovy type mapping ─────────────────────────────────────────────

    @Nested
    inner class GroovyTypeMappingFromDump {

        @Test
        fun intColumnsMapToInteger() {
            val activity = entities.first { it.entityName == "Activity" }
            val intCols = activity.columns.filter { it.databaseType?.startsWith("int") == true || it.databaseType?.startsWith("tinyint") == true }
            for (col in intCols) {
                if (col.databaseIdType == null) {
                    assertEquals("Integer", col.javaType, "Column ${col.databaseColumnName} should map to Integer in Groovy")
                }
            }
        }

        @Test
        fun varcharColumnsMapToString() {
            val user = entities.first { it.entityName == "User" }
            val nameCol = user.columns.find { it.databaseColumnName == "NAME" }
            assertEquals("String", nameCol?.javaType)
        }

        @Test
        fun primaryKeyMapsToLong() {
            val user = entities.first { it.entityName == "User" }
            val pkCol = user.columns.find { it.databaseIdType == "@Id" }
            assertEquals("Long", pkCol?.javaType)
        }

        @Test
        fun foreignKeyMapsToInteger() {
            val activity = entities.first { it.entityName == "Activity" }
            val fkCol = activity.columns.find { it.databaseIdType == "@ManyToOne" }
            assertEquals("Integer", fkCol?.javaType)
        }
    }

    // ── Groovy template generation ──────────────────────────────────────

    @Nested
    inner class GroovyTemplateGeneration {

        private fun parseFromSql(sql: String): List<Entity> {
            val words = mutableListOf<String>()
            Util.getWords(words, ByteArrayInputStream(sql.toByteArray()))
            val entityList = mutableListOf<Entity>()
            Util.parseWords(entityList, words)
            return entityList
        }

        @Test
        fun parsedEntityGeneratesValidGroovyEntity() {
            val sql = """
                CREATE TABLE `CUSTOMER`
                (
                    `CUSTOMER_ID` int(11) NOT NULL AUTO_INCREMENT,
                    `FIRST_NAME` varchar(100) NOT NULL,
                    `LAST_NAME` varchar(100) NOT NULL,
                    `EMAIL` varchar(250) NOT NULL,
                    `CREATED_AT` timestamp NULL,
                    PRIMARY KEY (`CUSTOMER_ID`) extra words pad here to ten
                )
            """.trimIndent()
            val entities = parseFromSql(sql)
            val customer = entities[0]

            val templates = Templates()
            val entityTemplate = templates.getEntityTemplate(customer, "com/myapp")
            assertContains(entityTemplate, "package com.myapp.entity")
            assertContains(entityTemplate, "@Table(name = \"CUSTOMER\")")
            assertContains(entityTemplate, "class Customer implements Serializable")
            assertContains(entityTemplate, "@Id")
            assertContains(entityTemplate, "String firstName")
            assertContains(entityTemplate, "String lastName")
            assertContains(entityTemplate, "String email")
            assertContains(entityTemplate, "LocalDateTime createdAt")
            assertContains(entityTemplate, "@Canonical")
            assertFalse(entityTemplate.contains("{{"))
            assertFalse(entityTemplate.contains("lombok"))
            assertFalse(entityTemplate.contains("private "))
        }

        @Test
        fun parsedEntityGeneratesValidGroovyResource() {
            val sql = "CREATE TABLE `ORDER`\n(\n`ORDER_ID` int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            val templates = Templates()
            val restTemplate = templates.getResourceTemplate("com/myapp", entities[0].entityName)
            assertContains(restTemplate, "class OrderResource")
            assertContains(restTemplate, "\"/order\"")
            assertContains(restTemplate, "readAll()")
            assertContains(restTemplate, "@Autowired")
        }

        @Test
        fun parsedEntityGeneratesValidGroovyDao() {
            val sql = "CREATE TABLE `PAYMENT`\n(\n`PAYMENT_ID` int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            val templates = Templates()
            val daoTemplate = templates.getDaoTemplate("com/myapp", entities[0].entityName)
            assertContains(daoTemplate, "class PaymentDao implements GenericDao<Payment>")
            assertContains(daoTemplate, "findAll()")
        }

        @Test
        fun parsedEntityGeneratesValidGroovyRepository() {
            val sql = "CREATE TABLE `INVOICE`\n(\n`INVOICE_ID` int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            val templates = Templates()
            val repoTemplate = templates.getRepositoryTemplate("com/myapp", entities[0].entityName)
            assertContains(repoTemplate, "interface InvoiceRepository extends JpaRepository<Invoice, Long>")
        }
    }

    // ── File generation integration (Groovy) ────────────────────────────

    @Nested
    inner class GroovyFileGeneration {

        @Test
        fun writesGroovyEntityFiles() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val entityDir = File(tempDir.toFile(), "com/test/app/entity")
            assertTrue(entityDir.exists())
            val entityFiles = entityDir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(entityFiles.contains("User.groovy"))
            assertTrue(entityFiles.contains("Activity.groovy"))
            assertTrue(entityFiles.contains("Language.groovy"))
            assertFalse(entityFiles.any { it.endsWith(".java") || it.endsWith(".kt") })
        }

        @Test
        fun writesGroovyRestFiles() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val restDir = File(tempDir.toFile(), "com/test/app/rest")
            assertTrue(restDir.exists())
            val restFiles = restDir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(restFiles.contains("UserResource.groovy"))
            assertTrue(restFiles.contains("ActivityResource.groovy"))
            assertFalse(restFiles.any { it.endsWith(".java") || it.endsWith(".kt") })
        }

        @Test
        fun writesGroovyDaoFiles() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val daoDir = File(tempDir.toFile(), "com/test/app/dao")
            assertTrue(daoDir.exists())
            val daoFiles = daoDir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(daoFiles.contains("UserDao.groovy"))
            assertTrue(daoFiles.contains("GenericDao.groovy"))
            assertFalse(daoFiles.any { it.endsWith(".java") || it.endsWith(".kt") })
        }

        @Test
        fun writesGroovyRepositoryFiles() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val repoDir = File(tempDir.toFile(), "com/test/app/repository")
            assertTrue(repoDir.exists())
            val repoFiles = repoDir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(repoFiles.contains("UserRepository.groovy"))
            assertFalse(repoFiles.any { it.endsWith(".java") || it.endsWith(".kt") })
        }

        @Test
        fun writesGroovyUtilsFiles() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val utilsDir = File(tempDir.toFile(), "com/test/app/utils")
            assertTrue(utilsDir.exists())
            val utilsFiles = utilsDir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(utilsFiles.contains("GlobalConstants.groovy"))
            assertFalse(utilsFiles.any { it.endsWith(".java") || it.endsWith(".kt") })
        }

        @Test
        fun generatedGroovyEntityFileHasCorrectContent() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val userFile = File(tempDir.toFile(), "com/test/app/entity/User.groovy")
            val content = userFile.readText()
            assertContains(content, "package com.test.app.entity")
            assertContains(content, "@Entity")
            assertContains(content, "@Table(name = \"USER\")")
            assertContains(content, "class User implements Serializable")
            assertContains(content, "@Id")
            assertContains(content, "String name")
            assertContains(content, "String email")
            assertContains(content, "@Canonical")
            assertFalse(content.contains("public class"))
            assertFalse(content.contains("lombok"))
        }

        @Test
        fun generatedGroovyRestFileHasCorrectContent() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val restFile = File(tempDir.toFile(), "com/test/app/rest/UserResource.groovy")
            val content = restFile.readText()
            assertContains(content, "package com.test.app.rest")
            assertContains(content, "@RestController")
            assertContains(content, "import com.test.app.entity.User")
            assertContains(content, "import com.test.app.dao.UserDao")
            assertContains(content, "class UserResource")
            assertContains(content, "@Autowired")
        }

        @Test
        fun generatedGroovyDaoFileHasCorrectContent() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val daoFile = File(tempDir.toFile(), "com/test/app/dao/UserDao.groovy")
            val content = daoFile.readText()
            assertContains(content, "@Service")
            assertContains(content, "implements GenericDao<User>")
        }

        @Test
        fun generatedGroovyRepositoryFileHasCorrectContent() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val repoFile = File(tempDir.toFile(), "com/test/app/repository/UserRepository.groovy")
            val content = repoFile.readText()
            assertContains(content, "@Repository")
            assertContains(content, "extends JpaRepository<User, Long>")
        }

        @Test
        fun generatedGroovyConstantsFileHasCorrectContent() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val constantsFile = File(tempDir.toFile(), "com/test/app/utils/GlobalConstants.groovy")
            val content = constantsFile.readText()
            assertContains(content, "class GlobalConstants")
            assertContains(content, "static final String CONTEXT_PATH = \"/api/v1\"")
        }

        @Test
        fun entityWithForeignKeyGeneratesJoinColumn() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val activityFile = File(tempDir.toFile(), "com/test/app/entity/Activity.groovy")
            val content = activityFile.readText()
            assertContains(content, "@ManyToOne")
            assertContains(content, "@JoinColumn")
        }
    }
}
