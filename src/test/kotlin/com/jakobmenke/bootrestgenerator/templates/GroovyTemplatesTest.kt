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

class GroovyTemplatesTest {

    private val templates = Templates()
    private val testPackage = "com/example/app"
    private val testPackageDot = "com.example.app"

    @BeforeEach
    fun setUp() {
        Globals.LANGUAGE = "groovy"
    }

    @AfterEach
    fun tearDown() {
        Globals.LANGUAGE = "java"
    }

    // ── Groovy Entity Template ───────────────────────────────────────────

    @Nested
    inner class GroovyEntityTemplate {

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
        fun containsCanonicalAnnotation() {
            val entity = Entity(tableName = "USER", entityName = "User")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "@Canonical")
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
        fun containsGroovyClassDeclaration() {
            val entity = Entity(tableName = "USER", entityName = "User")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "class User implements Serializable")
        }

        @Test
        fun usesTypeWithoutPrivate() {
            val entity = Entity(
                tableName = "USER", entityName = "User",
                columns = mutableListOf(
                    ColumnToField("NAME", "name", "varchar(150)", "String")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "String name")
            assertFalse(result.contains("private String"))
        }

        @Test
        fun doesNotUseVarKeyword() {
            val entity = Entity(
                tableName = "USER", entityName = "User",
                columns = mutableListOf(
                    ColumnToField("NAME", "name", "varchar(150)", "String")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertFalse(result.contains("var name"))
        }

        @Test
        fun fieldsDoNotHaveDefaultValues() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField("NAME", "name", "varchar(150)", "String"),
                    ColumnToField("COUNT", "count", "int(11)", "Integer"),
                    ColumnToField("AMOUNT", "amount", "double", "Double")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "String name")
            assertContains(result, "Integer count")
            assertContains(result, "Double amount")
            assertFalse(result.contains("= \"\""))
            assertFalse(result.contains("= 0"))
            assertFalse(result.contains("= null"))
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
            assertContains(result, "Long user")
            assertContains(result, "String name")
            assertContains(result, "String email")
            assertContains(result, "LocalDate createDate")
        }

        @Test
        fun fieldsDoNotHaveSemicolons() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField("NAME", "name", "varchar(50)", "String")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            // Groovy fields should not have semicolons
            assertFalse(result.contains("name;"))
        }

        @Test
        fun importsGroovyTransform() {
            val entity = Entity(tableName = "USER", entityName = "User")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "import groovy.transform.*")
        }

        @Test
        fun endsWithClosingBrace() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(ColumnToField("C", "c", "int(11)", "Integer"))
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
            assertContains(result, "class Empty implements Serializable")
            assertTrue(result.trimEnd().endsWith("}"))
        }

        @Test
        fun allFieldTypes() {
            val entity = Entity(
                tableName = "ALL", entityName = "All",
                columns = mutableListOf(
                    ColumnToField(
                        databaseIdType = "@Id",
                        databaseColumnName = "ID",
                        camelCaseFieldName = "id",
                        databaseType = null,
                        javaType = "Long"
                    ),
                    ColumnToField("NAME", "name", "varchar(100)", "String"),
                    ColumnToField("COUNT", "count", "int(11)", "Integer"),
                    ColumnToField("PRICE", "price", "float", "Float"),
                    ColumnToField("AMOUNT", "amount", "double", "Double"),
                    ColumnToField("CREATED", "created", "datetime", "LocalDate"),
                    ColumnToField("STARTED", "started", "time", "LocalTime"),
                    ColumnToField("UPDATED", "updated", "timestamp", "LocalDateTime"),
                    ColumnToField(
                        databaseIdType = "@ManyToOne",
                        databaseColumnName = "PARENT_ID",
                        camelCaseFieldName = "parent",
                        databaseType = null,
                        javaType = "Parent"
                    )
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "Long id")
            assertContains(result, "String name")
            assertContains(result, "Integer count")
            assertContains(result, "Float price")
            assertContains(result, "Double amount")
            assertContains(result, "LocalDate created")
            assertContains(result, "LocalTime started")
            assertContains(result, "LocalDateTime updated")
            assertContains(result, "Parent parent")
        }
    }

    // ── Groovy Resource Template ─────────────────────────────────────────

    @Nested
    inner class GroovyResourceTemplate {
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
        fun usesAutowiredInjection() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "@Autowired")
            assertContains(result, "UserDao dao")
        }

        @Test
        fun doesNotUseConstructorInjection() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertFalse(result.contains("private val dao"))
        }

        @Test
        fun containsCrudMethods() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "readAll()")
            assertContains(result, "read(")
            assertContains(result, "create(")
            assertContains(result, "update(")
            assertContains(result, "delete(")
            assertContains(result, "deleteAll(")
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
            assertContains(result, "class LanguageLevelResource")
            assertContains(result, "/languagelevel")
        }

        @Test
        fun methodReturnTypes() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "List<User> readAll()")
            assertContains(result, "User read(")
            assertContains(result, "User create(")
            assertContains(result, "User update(")
            assertContains(result, "boolean delete(")
            assertContains(result, "boolean deleteAll(")
        }
    }

    // ── Groovy DAO Template ──────────────────────────────────────────────

    @Nested
    inner class GroovyDaoTemplate {
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
        fun implementsGenericDao() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertContains(result, "implements GenericDao<User>")
        }

        @Test
        fun usesAutowiredInjection() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertContains(result, "@Autowired")
            assertContains(result, "UserRepository userRepository")
        }

        @Test
        fun doesNotUseConstructorInjection() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertFalse(result.contains("private val"))
        }

        @Test
        fun containsDaoMethods() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertContains(result, "findAll()")
            assertContains(result, "findOne(Long id)")
            assertContains(result, "save(User entity)")
            assertContains(result, "delete(Long id)")
            assertContains(result, "deleteAll(")
        }

        @Test
        fun doesNotUseOverrideKeyword() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertFalse(result.contains("override"))
        }

        @Test
        fun noPlaceholdersRemaining() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertFalse(result.contains("{{"))
        }

        @Test
        fun multiWordEntityCamelCaseRepo() {
            val result = templates.getDaoTemplate(testPackage, "LanguageLevel")
            assertContains(result, "languageLevelRepository")
        }
    }

    // ── Groovy Repository Template ───────────────────────────────────────

    @Nested
    inner class GroovyRepositoryTemplate {
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
        fun extendsJpaRepository() {
            val result = templates.getRepositoryTemplate(testPackage, "User")
            assertContains(result, "extends JpaRepository<User, Long>")
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

    // ── Groovy Constants Template ────────────────────────────────────────

    @Nested
    inner class GroovyConstantsTemplate {
        @Test
        fun usesClassKeyword() {
            val result = templates.getFileTemplateByName(testPackage, "constants")
            assertContains(result, "class GlobalConstants")
        }

        @Test
        fun doesNotUseObjectKeyword() {
            val result = templates.getFileTemplateByName(testPackage, "constants")
            assertFalse(result.contains("object GlobalConstants"))
        }

        @Test
        fun containsContextPath() {
            val result = templates.getFileTemplateByName(testPackage, "constants")
            assertContains(result, "static final String CONTEXT_PATH = \"/api/v1\"")
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

    // ── Groovy Generic DAO Template ──────────────────────────────────────

    @Nested
    inner class GroovyGenericDaoTemplate {
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
        fun containsMethodDeclarations() {
            val result = templates.getFileTemplateByName(testPackage, "genericdao")
            assertContains(result, "List<T> findAll()")
            assertContains(result, "T findOne(Long id)")
            assertContains(result, "T save(T t)")
            assertContains(result, "void delete(Long id)")
            assertContains(result, "void delete(T t)")
            assertContains(result, "void deleteAll(List<T> list)")
        }

        @Test
        fun noPlaceholdersRemaining() {
            val result = templates.getFileTemplateByName(testPackage, "genericdao")
            assertFalse(result.contains("{{"))
        }
    }

    // ── Package slash-to-dot conversion ──────────────────────────────────

    @Nested
    inner class GroovyPackageConversion {
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

    // ── Groovy does NOT produce Java or Kotlin syntax ────────────────────

    @Nested
    inner class GroovyDoesNotProduceOtherSyntax {
        @Test
        fun groovyEntityDoesNotUseSemicolonsInFields() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField("NAME", "name", "varchar(50)", "String")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertFalse(result.contains("name;"))
        }

        @Test
        fun groovyEntityDoesNotUseKotlinVarSyntax() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField("NAME", "name", "varchar(50)", "String")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertFalse(result.contains("var name:"))
            assertFalse(result.contains("val name:"))
        }

        @Test
        fun groovyResourceDoesNotUseKotlinFunKeyword() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertFalse(result.contains("fun "))
        }

        @Test
        fun groovyDaoDoesNotUseColonForInheritance() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertFalse(result.contains(": GenericDao"))
        }

        @Test
        fun groovyRepositoryDoesNotUseColonForInheritance() {
            val result = templates.getRepositoryTemplate(testPackage, "User")
            assertFalse(result.contains(": JpaRepository"))
        }
    }
}
