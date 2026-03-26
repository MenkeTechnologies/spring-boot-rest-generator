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

class GroovyEntityFieldsTest {

    private val templates = Templates()
    private val testPackage = "com/example/app"

    @BeforeEach
    fun setUp() {
        Globals.LANGUAGE = "groovy"
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

    // ── Field declarations for every type ────────────────────────────────

    @Nested
    inner class FieldDeclarations {
        @Test
        fun stringField() = assertContains(entityWithField("String"), "String field")

        @Test
        fun integerField() = assertContains(entityWithField("Integer"), "Integer field")

        @Test
        fun longField() = assertContains(entityWithField("Long"), "Long field")

        @Test
        fun floatField() = assertContains(entityWithField("Float"), "Float field")

        @Test
        fun doubleField() = assertContains(entityWithField("Double"), "Double field")

        @Test
        fun localDateField() = assertContains(entityWithField("LocalDate"), "LocalDate field")

        @Test
        fun localTimeField() = assertContains(entityWithField("LocalTime"), "LocalTime field")

        @Test
        fun localDateTimeField() = assertContains(entityWithField("LocalDateTime"), "LocalDateTime field")

        @Test
        fun entityReferenceField() = assertContains(entityWithField("Module"), "Module field")

        @Test
        fun unknownTypeField() = assertContains(entityWithField("CustomType"), "CustomType field")
    }

    // ── No default values (unlike Kotlin) ────────────────────────────────

    @Nested
    inner class NoDefaultValues {
        @Test
        fun stringHasNoDefault() {
            val result = entityWithField("String")
            assertFalse(result.contains("= \"\""))
        }

        @Test
        fun integerHasNoDefault() {
            val result = entityWithField("Integer")
            assertFalse(result.contains("= 0"))
        }

        @Test
        fun longHasNoDefault() {
            val result = entityWithField("Long")
            assertFalse(result.contains("= 0L"))
        }

        @Test
        fun floatHasNoDefault() {
            val result = entityWithField("Float")
            assertFalse(result.contains("= 0f"))
        }

        @Test
        fun doubleHasNoDefault() {
            val result = entityWithField("Double")
            assertFalse(result.contains("= 0.0"))
        }

        @Test
        fun localDateHasNoDefault() {
            val result = entityWithField("LocalDate")
            assertFalse(result.contains("= null"))
        }

        @Test
        fun entityReferenceHasNoDefault() {
            val result = entityWithField("Module")
            assertFalse(result.contains("= null"))
        }
    }

    // ── No nullable types (unlike Kotlin) ────────────────────────────────

    @Nested
    inner class NoNullableTypes {
        @Test
        fun localDateNotNullable() {
            val result = entityWithField("LocalDate")
            assertFalse(result.contains("LocalDate?"))
        }

        @Test
        fun localTimeNotNullable() {
            val result = entityWithField("LocalTime")
            assertFalse(result.contains("LocalTime?"))
        }

        @Test
        fun localDateTimeNotNullable() {
            val result = entityWithField("LocalDateTime")
            assertFalse(result.contains("LocalDateTime?"))
        }

        @Test
        fun entityRefNotNullable() {
            val result = entityWithField("Module")
            assertFalse(result.contains("Module?"))
        }
    }

    // ── Multiple fields with mixed types ─────────────────────────────────

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
                    ColumnToField("QUANTITY", "quantity", "int(11)", "Integer")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "@Id")
            assertContains(result, "@Column(name = \"ORDER_ITEM_ID\")")
            assertContains(result, "@ManyToOne")
            assertContains(result, "@JoinColumn(name = \"ORDER_ID\")")
            assertContains(result, "@JoinColumn(name = \"PRODUCT_ID\")")
            assertContains(result, "@Column(name = \"QUANTITY\")")
            assertContains(result, "Long orderItem")
            assertContains(result, "Order order")
            assertContains(result, "Product product")
            assertContains(result, "Integer quantity")
        }
    }

    // ── Groovy mode does NOT produce Java or Kotlin field syntax ─────────

    @Nested
    inner class CompareWithOtherLanguages {
        @Test
        fun javaEntityUsesPrivateAndSemicolon() {
            Globals.LANGUAGE = "java"
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField("NAME", "name", "varchar(50)", "String")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "private String name;")
            Globals.LANGUAGE = "groovy"
        }

        @Test
        fun kotlinEntityUsesVarAndColon() {
            Globals.LANGUAGE = "kotlin"
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField("NAME", "name", "varchar(50)", "String")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "var name: String = \"\"")
            Globals.LANGUAGE = "groovy"
        }

        @Test
        fun groovyEntityUsesPlainTypeAndName() {
            val entity = Entity(
                tableName = "T", entityName = "T",
                columns = mutableListOf(
                    ColumnToField("NAME", "name", "varchar(50)", "String")
                )
            )
            val result = templates.getEntityTemplate(entity, testPackage)
            assertContains(result, "String name")
            assertFalse(result.contains("private String"))
            assertFalse(result.contains("var name:"))
            assertFalse(result.contains("name;"))
        }
    }
}
