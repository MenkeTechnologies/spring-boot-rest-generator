package com.jakobmenke.bootrestgenerator.templates

import com.jakobmenke.bootrestgenerator.dto.ColumnToField
import com.jakobmenke.bootrestgenerator.dto.Entity
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemplatesTest {

    private val templates = Templates()
    private val testPackage = "com/example/app"
    private val testPackageDot = "com.example.app"

    // ── getResourceTemplate ────────────────────────────────────────────

    @Nested
    inner class ResourceTemplate {
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
        fun containsClassName() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "class UserResource")
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
        fun containsAutowiredDao() {
            val result = templates.getResourceTemplate(testPackage, "User")
            assertContains(result, "@Autowired")
            assertContains(result, "UserDao dao")
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
            assertContains(result, "LanguageLevelDao dao")
            assertContains(result, "/languagelevel")
        }
    }

    // ── getDaoTemplate ─────────────────────────────────────────────────

    @Nested
    inner class DaoTemplate {
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
        fun containsCamelCaseRepositoryName() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertContains(result, "UserRepository userRepository")
        }

        @Test
        fun containsCrudMethods() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertContains(result, "findAll()")
            assertContains(result, "findOne(Long id)")
            assertContains(result, "save(User entity)")
            assertContains(result, "delete(Long id)")
            assertContains(result, "deleteAll")
        }

        @Test
        fun noPlaceholdersRemaining() {
            val result = templates.getDaoTemplate(testPackage, "User")
            assertFalse(result.contains("{{"))
        }

        @Test
        fun multiWordEntityCamelCaseRepo() {
            val result = templates.getDaoTemplate(testPackage, "LanguageLevel")
            assertContains(result, "LanguageLevelRepository languageLevelRepository")
        }
    }

    // ── getRepositoryTemplate ──────────────────────────────────────────

    @Nested
    inner class RepositoryTemplate {
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
        fun containsEntityImport() {
            val result = templates.getRepositoryTemplate(testPackage, "User")
            assertContains(result, "import $testPackageDot.entity.User")
        }

        @Test
        fun noPlaceholdersRemaining() {
            val result = templates.getRepositoryTemplate(testPackage, "User")
            assertFalse(result.contains("{{"))
        }

        @Test
        fun interfaceNaming() {
            val result = templates.getRepositoryTemplate(testPackage, "Payment")
            assertContains(result, "interface PaymentRepository")
        }
    }

    // ── getEntityTemplate ──────────────────────────────────────────────

    @Nested
    inner class EntityTemplate {

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
        fun containsLombokAnnotations() {
            val entity = Entity(tableName = "USER", entityName = "User")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "@Data")
            assertContains(result, "@AllArgsConstructor")
            assertContains(result, "@NoArgsConstructor")
        }

        @Test
        fun containsClassName() {
            val entity = Entity(tableName = "USER", entityName = "User")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "public class User implements Serializable")
        }

        @Test
        fun containsJakartaPersistenceImport() {
            val entity = Entity(tableName = "USER", entityName = "User")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "import jakarta.persistence.*")
        }

        @Test
        fun regularColumnGetAnnotation() {
            val entity = Entity(
                tableName = "USER", entityName = "User",
                columns = mutableListOf(
                    ColumnToField("NAME", "name", "varchar(150)", "String")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "@Column(name = \"NAME\")")
            assertContains(result, "private String name;")
        }

        @Test
        fun primaryKeyColumnGetIdAnnotation() {
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
            assertContains(result, "private Long user;")
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
                        javaType = "Integer"
                    )
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "@ManyToOne")
            assertContains(result, "@JoinColumn(name = \"MODULE_ID\")")
            assertContains(result, "private Integer module;")
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
            assertContains(result, "private Long user;")
            assertContains(result, "private String name;")
            assertContains(result, "private String email;")
            assertContains(result, "private LocalDate createDate;")
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
        }

        @Test
        fun emptyColumnsStillGeneratesValidClass() {
            val entity = Entity(tableName = "EMPTY", entityName = "Empty")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "public class Empty implements Serializable")
            assertTrue(result.trimEnd().endsWith("}"))
        }
    }

    // ── getFileTemplateByName ──────────────────────────────────────────

    @Nested
    inner class FileTemplateByName {
        @Test
        fun constantsTemplateHasPackage() {
            val result = templates.getFileTemplateByName(testPackage, "constants")
            assertContains(result, "package $testPackageDot.utils")
        }

        @Test
        fun constantsTemplateHasContextPath() {
            val result = templates.getFileTemplateByName(testPackage, "constants")
            assertContains(result, "CONTEXT_PATH = \"/api/v1\"")
        }

        @Test
        fun genericDaoTemplateHasPackage() {
            val result = templates.getFileTemplateByName(testPackage, "genericdao")
            assertContains(result, "package $testPackageDot.dao")
        }

        @Test
        fun genericDaoTemplateHasInterface() {
            val result = templates.getFileTemplateByName(testPackage, "genericdao")
            assertContains(result, "public interface GenericDao<T>")
        }

        @Test
        fun genericDaoTemplateHasCrudMethods() {
            val result = templates.getFileTemplateByName(testPackage, "genericdao")
            assertContains(result, "List<T> findAll()")
            assertContains(result, "T findOne(Long id)")
            assertContains(result, "T save(T t )")
            assertContains(result, "void delete(Long id)")
            assertContains(result, "void delete(T t)")
            assertContains(result, "void deleteAll(List<T> list)")
        }

        @Test
        fun noPlaceholdersInConstants() {
            val result = templates.getFileTemplateByName(testPackage, "constants")
            assertFalse(result.contains("{{"))
        }

        @Test
        fun noPlaceholdersInGenericDao() {
            val result = templates.getFileTemplateByName(testPackage, "genericdao")
            assertFalse(result.contains("{{"))
        }
    }

    // ── Package slash-to-dot conversion ────────────────────────────────

    @Nested
    inner class PackageConversion {
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
