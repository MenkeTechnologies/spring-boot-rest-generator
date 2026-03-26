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

class KotlinEntityFieldDefaultsTest {

    private val templates = Templates()
    private val testPackage = "com/example/app"

    @BeforeEach
    fun setUp() {
        Globals.LANGUAGE = "kotlin"
    }

    @AfterEach
    fun tearDown() {
        Globals.LANGUAGE = "java"
    }

    private fun entityWithField(type: String, fieldName: String = "field"): String {
        val entity = Entity(
            tableName = "TEST", entityName = "Test",
            columns = mutableListOf(
                ColumnToField("COL", fieldName, "varchar(50)", type)
            )
        )
        return templates.getEntityTemplate(entity, testPackage)
    }

    // ── Default values for every type ───────────────────────────────────

    @Nested
    inner class DefaultValues {
        @Test
        fun stringDefault() = assertContains(entityWithField("String"), "var field: String = \"\"")

        @Test
        fun intDefault() = assertContains(entityWithField("Int"), "var field: Int = 0")

        @Test
        fun longDefault() = assertContains(entityWithField("Long"), "var field: Long = 0L")

        @Test
        fun floatDefault() = assertContains(entityWithField("Float"), "var field: Float = 0f")

        @Test
        fun doubleDefault() = assertContains(entityWithField("Double"), "var field: Double = 0.0")

        @Test
        fun booleanDefault() = assertContains(entityWithField("Boolean"), "var field: Boolean = false")

        @Test
        fun localDateDefault() = assertContains(entityWithField("LocalDate"), "var field: LocalDate? = null")

        @Test
        fun localTimeDefault() = assertContains(entityWithField("LocalTime"), "var field: LocalTime? = null")

        @Test
        fun localDateTimeDefault() = assertContains(entityWithField("LocalDateTime"), "var field: LocalDateTime? = null")

        @Test
        fun entityReferenceDefault() = assertContains(entityWithField("Module"), "var field: Module? = null")

        @Test
        fun unknownTypeDefault() = assertContains(entityWithField("CustomType"), "var field: CustomType? = null")
    }

    // ── Nullable vs non-nullable ────────────────────────────────────────

    @Nested
    inner class NullableTypes {
        @Test
        fun stringIsNotNullable() {
            val result = entityWithField("String")
            assertFalse(result.contains("String?"))
        }

        @Test
        fun intIsNotNullable() {
            val result = entityWithField("Int")
            assertFalse(result.contains("Int?"))
        }

        @Test
        fun longIsNotNullable() {
            val result = entityWithField("Long")
            assertFalse(result.contains("Long?"))
        }

        @Test
        fun booleanIsNotNullable() {
            val result = entityWithField("Boolean")
            assertFalse(result.contains("Boolean?"))
        }

        @Test
        fun localDateIsNullable() = assertContains(entityWithField("LocalDate"), "LocalDate?")

        @Test
        fun localTimeIsNullable() = assertContains(entityWithField("LocalTime"), "LocalTime?")

        @Test
        fun localDateTimeIsNullable() = assertContains(entityWithField("LocalDateTime"), "LocalDateTime?")

        @Test
        fun entityRefIsNullable() = assertContains(entityWithField("SomeEntity"), "SomeEntity?")
    }

    // ── Multiple fields with mixed types ────────────────────────────────

    @Nested
    inner class MixedFieldTypes {
        @Test
        fun allTypesTogether() {
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
                    ColumnToField("COUNT", "count", "int(11)", "Int"),
                    ColumnToField("PRICE", "price", "float", "Float"),
                    ColumnToField("AMOUNT", "amount", "double", "Double"),
                    ColumnToField("ACTIVE", "active", "bit(1)", "Boolean"),
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
            assertContains(result, "var id: Long = 0L")
            assertContains(result, "var name: String = \"\"")
            assertContains(result, "var count: Int = 0")
            assertContains(result, "var price: Float = 0f")
            assertContains(result, "var amount: Double = 0.0")
            assertContains(result, "var active: Boolean = false")
            assertContains(result, "var created: LocalDate? = null")
            assertContains(result, "var started: LocalTime? = null")
            assertContains(result, "var updated: LocalDateTime? = null")
            assertContains(result, "var parent: Parent? = null")
        }

        @Test
        fun entityWithPkAndFkAnnotations() {
            val entity = Entity(
                tableName = "ORDER_ITEM", entityName = "OrderItem",
                columns = mutableListOf(
                    ColumnToField(
                        databaseIdType = "@Id",
                        databaseColumnName = "ORDER_ITEM_ID",
                        camelCaseFieldName = "orderItem",
                        databaseType = null,
                        javaType = "Long"
                    ),
                    ColumnToField(
                        databaseIdType = "@ManyToOne",
                        databaseColumnName = "ORDER_ID",
                        camelCaseFieldName = "order",
                        databaseType = null,
                        javaType = "Order"
                    ),
                    ColumnToField(
                        databaseIdType = "@ManyToOne",
                        databaseColumnName = "PRODUCT_ID",
                        camelCaseFieldName = "product",
                        databaseType = null,
                        javaType = "Product"
                    ),
                    ColumnToField("QUANTITY", "quantity", "int(11)", "Int")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "@Id")
            assertContains(result, "@Column(name = \"ORDER_ITEM_ID\")")
            assertContains(result, "@ManyToOne")
            assertContains(result, "@JoinColumn(name = \"ORDER_ID\")")
            assertContains(result, "@JoinColumn(name = \"PRODUCT_ID\")")
            assertContains(result, "@Column(name = \"QUANTITY\")")
            assertContains(result, "var orderItem: Long = 0L")
            assertContains(result, "var order: Order? = null")
            assertContains(result, "var product: Product? = null")
            assertContains(result, "var quantity: Int = 0")
        }
    }

    // ── Java mode does NOT produce Kotlin syntax ────────────────────────

    @Nested
    inner class JavaModeDoesNotProduceKotlin {
        @Test
        fun javaEntityUsesPrivateKeyword() {
            Globals.LANGUAGE = "java"
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField("NAME", "name", "varchar(50)", "String")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "private String name;")
            assertFalse(result.contains("var name:"))
        }

        @Test
        fun javaEntityHasLombok() {
            Globals.LANGUAGE = "java"
            val entity = Entity(tableName = "T", entityName = "T")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "@Data")
            assertContains(result, "@AllArgsConstructor")
            assertContains(result, "@NoArgsConstructor")
        }

        @Test
        fun javaEntityHasPublicClass() {
            Globals.LANGUAGE = "java"
            val entity = Entity(tableName = "T", entityName = "T")
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "public class T implements Serializable")
        }
    }
}
