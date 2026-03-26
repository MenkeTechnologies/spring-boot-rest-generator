package com.jakobmenke.bootrestgenerator.templates

import com.jakobmenke.bootrestgenerator.dto.ColumnToField
import com.jakobmenke.bootrestgenerator.dto.Entity
import com.jakobmenke.bootrestgenerator.utils.Globals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinTemplatesTest {

    private val templates = Templates()
    private val testPackage = "com/example/app"
    private val testPackageDot = "com.example.app"

    @BeforeEach
    fun setUp() {
        Globals.LANGUAGE = "kotlin"
    }

    @AfterEach
    fun tearDown() {
        Globals.LANGUAGE = "java"
    }

    // ── Kotlin Entity Template ──────────────────────────────────────────

    @Nested
    inner class KotlinEntityTemplate {

        @Test
        fun containsPackageDeclaration() {
            val entity = Entity(tableName = "USER", entityName = "User")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "package $testPackageDot.entity")
        }

        @Test
        fun containsJpaAnnotations() {
            val entity = Entity(tableName = "USER", entityName = "User")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "@Entity")
            assertContains(result, "@Table(name = \"USER\")")
        }

        @Test
        fun doesNotContainLombok() {
            val entity = Entity(tableName = "USER", entityName = "User")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertFalse(result.contains("@Data"))
            assertFalse(result.contains("@AllArgsConstructor"))
            assertFalse(result.contains("@NoArgsConstructor"))
            assertFalse(result.contains("lombok"))
        }

        @Test
        fun doesNotContainPublicClassKeyword() {
            val entity = Entity(tableName = "USER", entityName = "User")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertFalse(result.contains("public class"))
        }

        @Test
        fun containsKotlinClassDeclaration() {
            val entity = Entity(tableName = "USER", entityName = "User")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "class User : Serializable")
        }

        @Test
        fun usesVarForFields() {
            val entity = Entity(
                tableName = "USER", entityName = "User",
                columns = mutableListOf(
                    ColumnToField("NAME", "name", "varchar(150)", "String")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "var name: String")
        }

        @Test
        fun doesNotUsePrivateKeyword() {
            val entity = Entity(
                tableName = "USER", entityName = "User",
                columns = mutableListOf(
                    ColumnToField("NAME", "name", "varchar(150)", "String")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertFalse(result.contains("private String"))
            assertFalse(result.contains("private var"))
        }

        @Test
        fun stringFieldHasEmptyDefault() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField("NAME", "name", "varchar(150)", "String")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "var name: String = \"\"")
        }

        @Test
        fun intFieldHasZeroDefault() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField("COUNT", "count", "int(11)", "Int")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "var count: Int = 0")
        }

        @Test
        fun longFieldHasZeroLDefault() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField(
                        databaseIdType = "@Id",
                        databaseColumnName = "ID",
                        camelCaseFieldName = "id",
                        databaseType = null,
                        javaType = "Long"
                    )
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "var id: Long = 0L")
        }

        @Test
        fun floatFieldHasZeroFDefault() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField("PRICE", "price", "float", "Float")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "var price: Float = 0f")
        }

        @Test
        fun doubleFieldHasZeroDefault() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField("AMOUNT", "amount", "double", "Double")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "var amount: Double = 0.0")
        }

        @Test
        fun localDateFieldIsNullable() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField("CREATE_DATE", "createDate", "datetime", "LocalDate")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "var createDate: LocalDate? = null")
        }

        @Test
        fun localDateTimeFieldIsNullable() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField("UPDATED_AT", "updatedAt", "timestamp", "LocalDateTime")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "var updatedAt: LocalDateTime? = null")
        }

        @Test
        fun entityReferenceFieldIsNullable() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField(
                        databaseIdType = "@ManyToOne",
                        databaseColumnName = "MODULE_ID",
                        camelCaseFieldName = "module",
                        databaseType = null,
                        javaType = "Module"
                    )
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "var module: Module? = null")
        }

        @Test
        fun primaryKeyColumnGetsIdAnnotation() {
            val entity = Entity(
                tableName = "USER", entityName = "User",
                columns = mutableListOf(
                    ColumnToField(
                        databaseIdType = "@Id",
                        databaseColumnName = "USER_ID",
                        camelCaseFieldName = "user",
                        databaseType = null,
                        javaType = "Long"
                    )
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "@Id")
            assertContains(result, "@Column(name = \"USER_ID\")")
        }

        @Test
        fun foreignKeyColumnGetsManyToOneAnnotation() {
            val entity = Entity(
                tableName = "ACTIVITY", entityName = "Activity",
                columns = mutableListOf(
                    ColumnToField(
                        databaseIdType = "@ManyToOne",
                        databaseColumnName = "MODULE_ID",
                        camelCaseFieldName = "module",
                        databaseType = null,
                        javaType = "Module"
                    )
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "@ManyToOne")
            assertContains(result, "@JoinColumn(name = \"MODULE_ID\")")
        }

        @Test
        fun multipleColumnsAllPresent() {
            val entity = Entity(
                tableName = "USER", entityName = "User",
                columns = mutableListOf(
                    ColumnToField(
                        databaseIdType = "@Id",
                        databaseColumnName = "USER_ID",
                        camelCaseFieldName = "user",
                        databaseType = null,
                        javaType = "Long"
                    ),
                    ColumnToField("NAME", "name", "varchar(150)", "String"),
                    ColumnToField("EMAIL", "email", "varchar(250)", "String"),
                    ColumnToField("CREATE_DATE", "createDate", "datetime", "LocalDate")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "var user: Long = 0L")
            assertContains(result, "var name: String = \"\"")
            assertContains(result, "var email: String = \"\"")
            assertContains(result, "var createDate: LocalDate? = null")
        }

        @Test
        fun noSemicolons() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField("NAME", "name", "varchar(50)", "String")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertFalse(result.contains(";"))
        }

        @Test
        fun endsWithClosingBrace() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(ColumnToField("C", "c", "int(11)", "Int"))
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertTrue(result.trimEnd().endsWith("}"))
        }

        @Test
        fun noPlaceholdersRemaining() {
            val entity = Entity(tableName = "USER", entityName = "User")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertFalse(result.contains("{{"))
            assertFalse(result.contains("}}"))
        }

        @Test
        fun emptyColumnsStillGeneratesValidClass() {
            val entity = Entity(tableName = "EMPTY", entityName = "Empty")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "class Empty : Serializable")
            assertTrue(result.trimEnd().endsWith("}"))
        }
    }

    // ── Kotlin Resource Template ────────────────────────────────────────

    @Nested
    inner class KotlinResourceTemplate {
        @Test
        fun containsPackageDeclaration() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "package $testPackageDot.rest")
        }

        @Test
        fun containsEntityImport() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "import $testPackageDot.entity.User")
        }

        @Test
        fun containsDaoImport() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "import $testPackageDot.dao.UserDao")
        }

        @Test
        fun containsRestControllerAnnotation() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "@RestController")
        }

        @Test
        fun containsRequestMapping() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "@RequestMapping(GlobalConstants.CONTEXT_PATH + \"/user\")")
        }

        @Test
        fun usesConstructorInjection() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "class UserResource(private val dao: UserDao)")
        }

        @Test
        fun doesNotUseAutowired() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertFalse(result.contains("@Autowired"))
        }

        @Test
        fun containsFunKeyword() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "fun readAll()")
            assertContains(result, "fun read(")
            assertContains(result, "fun create(")
            assertContains(result, "fun update(")
            assertContains(result, "fun delete(")
            assertContains(result, "fun deleteAll(")
        }

        @Test
        fun containsGetMapping() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "@GetMapping")
        }

        @Test
        fun containsPostMapping() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "@PostMapping")
        }

        @Test
        fun containsPutMapping() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "@PutMapping")
        }

        @Test
        fun containsDeleteMapping() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "@DeleteMapping")
        }

        @Test
        fun noPlaceholdersRemaining() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertFalse(result.contains("{{"))
            assertFalse(result.contains("}}"))
        }

        @Test
        fun multiWordEntityName() {
            val result = templates.getResourceTemplate(testPackage, "LanguageLevel")
            assertContains(result, "class LanguageLevelResource(private val dao: LanguageLevelDao)")
            assertContains(result, "/languagelevel")
        }
    }

    // ── Kotlin DAO Template ─────────────────────────────────────────────

    @Nested
    inner class KotlinDaoTemplate {
        @Test
        fun containsPackageDeclaration() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertContains(result, "package $testPackageDot.dao")
        }

        @Test
        fun containsServiceAnnotation() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertContains(result, "@Service")
        }

        @Test
        fun containsTransactionalAnnotation() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertContains(result, "@Transactional")
        }

        @Test
        fun extendsGenericDaoWithColon() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertContains(result, ": GenericDao<User>")
        }

        @Test
        fun doesNotUseImplementsKeyword() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertFalse(result.contains("implements"))
        }

        @Test
        fun usesConstructorInjection() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertContains(result, "class UserDao(private val userRepository: UserRepository)")
        }

        @Test
        fun doesNotUseAutowired() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertFalse(result.contains("@Autowired"))
        }

        @Test
        fun containsOverrideFunctions() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertContains(result, "override fun findAll()")
            assertContains(result, "override fun findOne(id: Long)")
            assertContains(result, "override fun save(entity: User)")
            assertContains(result, "override fun delete(id: Long)")
            assertContains(result, "override fun deleteAll(")
        }

        @Test
        fun noPlaceholdersRemaining() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertFalse(result.contains("{{"))
        }

        @Test
        fun multiWordEntityCamelCaseRepo() {
            val result = templates.getDaoTemplate(testPackage, "LanguageLevel")
            assertContains(result, "languageLevelRepository: LanguageLevelRepository")
        }
    }

    // ── Kotlin Repository Template ──────────────────────────────────────

    @Nested
    inner class KotlinRepositoryTemplate {
        @Test
        fun containsPackageDeclaration() {
            val result = templates.getRepositoryTemplate(testPackage, "User")
            assertContains(result, "package $testPackageDot.repository")
        }

        @Test
        fun containsRepositoryAnnotation() {
            val result = templates.getRepositoryTemplate(testPackage, "User")
            assertContains(result, "@Repository")
        }

        @Test
        fun extendsJpaRepositoryWithColon() {
            val result = templates.getRepositoryTemplate(testPackage, "User")
            assertContains(result, ": JpaRepository<User, Long>")
        }

        @Test
        fun doesNotUseExtendsKeyword() {
            val result = templates.getRepositoryTemplate(testPackage, "User")
            assertFalse(result.contains("extends"))
        }

        @Test
        fun usesInterfaceKeyword() {
            val result = templates.getRepositoryTemplate(testPackage, "User")
            assertContains(result, "interface UserRepository")
        }

        @Test
        fun containsEntityImport() {
            val result = templates.getRepositoryTemplate(testPackage, "User")
            assertContains(result, "import $testPackageDot.entity.User")
        }

        @Test
        fun noPlaceholdersRemaining() {
            val result = templates.getRepositoryTemplate(testPackage, "User")
            assertFalse(result.contains("{{"))
        }
    }

    // ── Kotlin Constants Template ───────────────────────────────────────

    @Nested
    inner class KotlinConstantsTemplate {
        @Test
        fun usesObjectKeyword() {
            val result = templates.getFileTemplateByName(testPackage, "constants")
            assertContains(result, "object GlobalConstants")
        }

        @Test
        fun doesNotUsePublicClass() {
            val result = templates.getFileTemplateByName(testPackage, "constants")
            assertFalse(result.contains("public class"))
        }

        @Test
        fun containsContextPath() {
            val result = templates.getFileTemplateByName(testPackage, "constants")
            assertContains(result, "const val CONTEXT_PATH = \"/api/v1\"")
        }

        @Test
        fun containsPackageDeclaration() {
            val result = templates.getFileTemplateByName(testPackage, "constants")
            assertContains(result, "package $testPackageDot.utils")
        }

        @Test
        fun noPlaceholdersRemaining() {
            val result = templates.getFileTemplateByName(testPackage, "constants")
            assertFalse(result.contains("{{"))
        }
    }

    // ── Kotlin Generic DAO Template ─────────────────────────────────────

    @Nested
    inner class KotlinGenericDaoTemplate {
        @Test
        fun containsPackageDeclaration() {
            val result = templates.getFileTemplateByName(testPackage, "genericdao")
            assertContains(result, "package $testPackageDot.dao")
        }

        @Test
        fun usesInterfaceKeyword() {
            val result = templates.getFileTemplateByName(testPackage, "genericdao")
            assertContains(result, "interface GenericDao<T>")
        }

        @Test
        fun doesNotUsePublicKeyword() {
            val result = templates.getFileTemplateByName(testPackage, "genericdao")
            assertFalse(result.contains("public interface"))
        }

        @Test
        fun containsFunDeclarations() {
            val result = templates.getFileTemplateByName(testPackage, "genericdao")
            assertContains(result, "fun findAll(): List<T>")
            assertContains(result, "fun findOne(id: Long): T?")
            assertContains(result, "fun save(entity: T): T")
            assertContains(result, "fun delete(id: Long)")
            assertContains(result, "fun delete(entity: T)")
            assertContains(result, "fun deleteAll(list: List<T>)")
        }

        @Test
        fun noPlaceholdersRemaining() {
            val result = templates.getFileTemplateByName(testPackage, "genericdao")
            assertFalse(result.contains("{{"))
        }
    }

    // ── Package slash-to-dot conversion ─────────────────────────────────

    @Nested
    inner class KotlinPackageConversion {
        @Test
        fun slashesConvertedToDots() {
            val result = templates.getResourceTemplate("com/foo/bar", "Test")
            assertContains(result, "package com.foo.bar.rest")
        }

        @Test
        fun deeplyNestedPackage() {
            val result = templates.getDaoTemplate("com/a/b/c/d", "Test")
            assertContains(result, "package com.a.b.c.d.dao")
        }

        @Test
        fun singleSegmentPackage() {
            val result = templates.getRepositoryTemplate("mypackage", "Test")
            assertContains(result, "package mypackage.repository")
        }
    }
}
