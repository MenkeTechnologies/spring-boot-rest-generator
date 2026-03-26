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

class KotlinPipelineTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var entities: List<Entity>

    @BeforeEach
    fun setUp() {
        Globals.LANGUAGE = "kotlin"
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

    // ── Kotlin type mapping ─────────────────────────────────────────────

    @Nested
    inner class KotlinTypeMappingFromDump {

        @Test
        fun intColumnsMapToInt() {
            val activity = entities.first { it.entityName == "Activity" }
            val intCols = activity.columns.filter { it.databaseType?.startsWith("int") == true || it.databaseType?.startsWith("tinyint") == true }
            for (col in intCols) {
                if (col.databaseIdType == null) {
                    assertEquals("Int", col.javaType, "Column ${col.databaseColumnName} should map to Int in Kotlin")
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
        fun datetimeColumnsMapToLocalDate() {
            val language = entities.first { it.entityName == "Language" }
            val createDateCol = language.columns.find { it.databaseColumnName == "CREATE_DATE" }
            assertEquals("LocalDate", createDateCol?.javaType)
        }

        @Test
        fun timestampColumnsMapToLocalDateTime() {
            val language = entities.first { it.entityName == "Language" }
            val updateDateCol = language.columns.find { it.databaseColumnName == "UPDATE_DATE" }
            assertEquals("LocalDateTime", updateDateCol?.javaType)
        }

        @Test
        fun primaryKeyMapsToLong() {
            val user = entities.first { it.entityName == "User" }
            val pkCol = user.columns.find { it.databaseIdType == "@Id" }
            assertEquals("Long", pkCol?.javaType)
        }

        @Test
        fun foreignKeyMapsToInt() {
            val activity = entities.first { it.entityName == "Activity" }
            val fkCol = activity.columns.find { it.databaseIdType == "@ManyToOne" }
            assertEquals("Int", fkCol?.javaType)
        }
    }

    // ── Kotlin type mapping from custom SQL ──────────────────────────────

    @Nested
    inner class KotlinTypeMappingCustomSql {

        private fun parseFromSql(sql: String): List<Entity> {
            val words = mutableListOf<String>()
            Util.getWords(words, ByteArrayInputStream(sql.toByteArray()))
            val entityList = mutableListOf<Entity>()
            Util.parseWords(entityList, words)
            return entityList
        }

        @Test
        fun intMapsToKotlinInt() {
            val sql = "CREATE TABLE `T`\n(\n`COL` int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("Int", entities[0].columns[0].javaType)
        }

        @Test
        fun tinyintMapsToKotlinInt() {
            val sql = "CREATE TABLE `T`\n(\n`COL` tinyint(3) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("Int", entities[0].columns[0].javaType)
        }

        @Test
        fun bitMapsToKotlinBoolean() {
            val sql = "CREATE TABLE `T`\n(\n`COL` bit(1) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("Boolean", entities[0].columns[0].javaType)
        }

        @Test
        fun bigintMapsToLong() {
            val sql = "CREATE TABLE `T`\n(\n`COL` bigint(20) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("Long", entities[0].columns[0].javaType)
        }

        @Test
        fun varcharMapsToString() {
            val sql = "CREATE TABLE `T`\n(\n`COL` varchar(255) NOT NULL\n)"
            val entities = parseFromSql(sql)
            assertEquals("String", entities[0].columns[0].javaType)
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
        fun allTypesInKotlin() {
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
            assertEquals(listOf("String", "Int", "Long", "LocalDate", "LocalDateTime", "Float", "Double", "Int", "Boolean"), types)
        }

        @Test
        fun foreignKeyTypeIsIntInKotlin() {
            val sql = """
                CREATE TABLE `BOOK`
                (
                    `BOOK_ID` int(11) NOT NULL AUTO_INCREMENT,
                    `AUTHOR_ID` int(11) NOT NULL,
                    PRIMARY KEY (`BOOK_ID`) extra words pad here to ten,
                    FOREIGN KEY (`AUTHOR_ID`) REFERENCES `AUTHOR` (`AUTHOR_ID`) ON DELETE CASCADE
                )
            """.trimIndent()
            val entities = parseFromSql(sql)
            val fk = entities[0].columns.find { it.databaseIdType == "@ManyToOne" }!!
            assertEquals("Int", fk.javaType)
        }
    }

    // ── Kotlin template generation from parsed SQL ──────────────────────

    @Nested
    inner class KotlinTemplateGeneration {

        private fun parseFromSql(sql: String): List<Entity> {
            val words = mutableListOf<String>()
            Util.getWords(words, ByteArrayInputStream(sql.toByteArray()))
            val entityList = mutableListOf<Entity>()
            Util.parseWords(entityList, words)
            return entityList
        }

        @Test
        fun parsedEntityGeneratesValidKotlinEntity() {
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
            assertContains(entityTemplate, "class Customer : Serializable")
            assertContains(entityTemplate, "@Id")
            assertContains(entityTemplate, "var firstName: String = \"\"")
            assertContains(entityTemplate, "var lastName: String = \"\"")
            assertContains(entityTemplate, "var email: String = \"\"")
            assertContains(entityTemplate, "var createdAt: LocalDateTime? = null")
            assertFalse(entityTemplate.contains("{{"))
            assertFalse(entityTemplate.contains("public class"))
            assertFalse(entityTemplate.contains("lombok"))
        }

        @Test
        fun parsedEntityGeneratesValidKotlinResource() {
            val sql = "CREATE TABLE `ORDER`\n(\n`ORDER_ID` int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            val templates = Templates()
            val restTemplate = templates.getResourceTemplate("com/myapp", entities[0].entityName)
            assertContains(restTemplate, "class OrderResource(private val dao: OrderDao)")
            assertContains(restTemplate, "\"/order\"")
            assertContains(restTemplate, "fun readAll()")
            assertFalse(restTemplate.contains("@Autowired"))
        }

        @Test
        fun parsedEntityGeneratesValidKotlinDao() {
            val sql = "CREATE TABLE `PAYMENT`\n(\n`PAYMENT_ID` int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            val templates = Templates()
            val daoTemplate = templates.getDaoTemplate("com/myapp", entities[0].entityName)
            assertContains(daoTemplate, "class PaymentDao(private val paymentRepository: PaymentRepository) : GenericDao<Payment>")
            assertContains(daoTemplate, "override fun findAll()")
        }

        @Test
        fun parsedEntityGeneratesValidKotlinRepository() {
            val sql = "CREATE TABLE `INVOICE`\n(\n`INVOICE_ID` int(11) NOT NULL\n)"
            val entities = parseFromSql(sql)
            val templates = Templates()
            val repoTemplate = templates.getRepositoryTemplate("com/myapp", entities[0].entityName)
            assertContains(repoTemplate, "interface InvoiceRepository : JpaRepository<Invoice, Long>")
        }
    }

    // ── File generation integration (Kotlin) ────────────────────────────

    @Nested
    inner class KotlinFileGeneration {

        @Test
        fun writesKotlinEntityFiles() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val entityDir = File(tempDir.toFile(), "com/test/app/entity")
            assertTrue(entityDir.exists())
            val entityFiles = entityDir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(entityFiles.contains("User.kt"))
            assertTrue(entityFiles.contains("Activity.kt"))
            assertTrue(entityFiles.contains("Language.kt"))
            assertFalse(entityFiles.any { it.endsWith(".java") })
        }

        @Test
        fun writesKotlinRestFiles() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val restDir = File(tempDir.toFile(), "com/test/app/rest")
            assertTrue(restDir.exists())
            val restFiles = restDir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(restFiles.contains("UserResource.kt"))
            assertTrue(restFiles.contains("ActivityResource.kt"))
            assertFalse(restFiles.any { it.endsWith(".java") })
        }

        @Test
        fun writesKotlinDaoFiles() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val daoDir = File(tempDir.toFile(), "com/test/app/dao")
            assertTrue(daoDir.exists())
            val daoFiles = daoDir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(daoFiles.contains("UserDao.kt"))
            assertTrue(daoFiles.contains("GenericDao.kt"))
            assertFalse(daoFiles.any { it.endsWith(".java") })
        }

        @Test
        fun writesKotlinRepositoryFiles() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val repoDir = File(tempDir.toFile(), "com/test/app/repository")
            assertTrue(repoDir.exists())
            val repoFiles = repoDir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(repoFiles.contains("UserRepository.kt"))
            assertFalse(repoFiles.any { it.endsWith(".java") })
        }

        @Test
        fun writesKotlinUtilsFiles() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val utilsDir = File(tempDir.toFile(), "com/test/app/utils")
            assertTrue(utilsDir.exists())
            val utilsFiles = utilsDir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(utilsFiles.contains("GlobalConstants.kt"))
            assertFalse(utilsFiles.any { it.endsWith(".java") })
        }

        @Test
        fun generatedKotlinEntityFileHasCorrectContent() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val userFile = File(tempDir.toFile(), "com/test/app/entity/User.kt")
            val content = userFile.readText()
            assertContains(content, "package com.test.app.entity")
            assertContains(content, "@Entity")
            assertContains(content, "@Table(name = \"USER\")")
            assertContains(content, "class User : Serializable")
            assertContains(content, "@Id")
            assertContains(content, "var name: String = \"\"")
            assertContains(content, "var email: String = \"\"")
            assertFalse(content.contains("public class"))
            assertFalse(content.contains("lombok"))
        }

        @Test
        fun generatedKotlinRestFileHasCorrectContent() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val restFile = File(tempDir.toFile(), "com/test/app/rest/UserResource.kt")
            val content = restFile.readText()
            assertContains(content, "package com.test.app.rest")
            assertContains(content, "@RestController")
            assertContains(content, "import com.test.app.entity.User")
            assertContains(content, "import com.test.app.dao.UserDao")
            assertContains(content, "class UserResource(private val dao: UserDao)")
            assertFalse(content.contains("@Autowired"))
        }

        @Test
        fun generatedKotlinDaoFileHasCorrectContent() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val daoFile = File(tempDir.toFile(), "com/test/app/dao/UserDao.kt")
            val content = daoFile.readText()
            assertContains(content, "@Service")
            assertContains(content, ": GenericDao<User>")
            assertContains(content, "override fun")
            assertFalse(content.contains("implements"))
        }

        @Test
        fun generatedKotlinRepositoryFileHasCorrectContent() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val repoFile = File(tempDir.toFile(), "com/test/app/repository/UserRepository.kt")
            val content = repoFile.readText()
            assertContains(content, "@Repository")
            assertContains(content, ": JpaRepository<User, Long>")
            assertFalse(content.contains("extends"))
        }

        @Test
        fun generatedKotlinConstantsFileHasCorrectContent() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val constantsFile = File(tempDir.toFile(), "com/test/app/utils/GlobalConstants.kt")
            val content = constantsFile.readText()
            assertContains(content, "object GlobalConstants")
            assertContains(content, "const val CONTEXT_PATH = \"/api/v1\"")
        }

        @Test
        fun totalGeneratedKotlinFilesPerEntity() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val entityCount = entities.size
            val entityFiles = File(tempDir.toFile(), "com/test/app/entity").listFiles()?.size ?: 0
            val restFiles = File(tempDir.toFile(), "com/test/app/rest").listFiles()?.size ?: 0
            val daoFiles = File(tempDir.toFile(), "com/test/app/dao").listFiles()?.size ?: 0
            val repoFiles = File(tempDir.toFile(), "com/test/app/repository").listFiles()?.size ?: 0
            val utilsFiles = File(tempDir.toFile(), "com/test/app/utils").listFiles()?.size ?: 0

            assertEquals(entityCount, entityFiles)
            assertEquals(entityCount, restFiles)
            assertEquals(entityCount + 1, daoFiles) // +1 for GenericDao.kt
            assertEquals(entityCount, repoFiles)
            assertEquals(1, utilsFiles) // GlobalConstants.kt
        }

        @Test
        fun entityWithForeignKeyGeneratesJoinColumn() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()

            val main = Main()
            main.writeTemplates(entities)

            val activityFile = File(tempDir.toFile(), "com/test/app/entity/Activity.kt")
            val content = activityFile.readText()
            assertContains(content, "@ManyToOne")
            assertContains(content, "@JoinColumn")
        }
    }
}
