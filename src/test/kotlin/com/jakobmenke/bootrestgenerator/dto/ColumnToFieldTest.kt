package com.jakobmenke.bootrestgenerator.dto

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ColumnToFieldTest {

    @Test
    fun defaultConstructorAllNull() {
        val col = ColumnToField()
        assertNull(col.databaseIdType)
        assertNull(col.databaseColumnName)
        assertNull(col.camelCaseFieldName)
        assertNull(col.databaseType)
        assertNull(col.javaType)
    }

    @Test
    fun fourArgConstructorSetsIdTypeNull() {
        val col = ColumnToField("COL_NAME", "colName", "int(11)", "Integer")
        assertNull(col.databaseIdType)
        assertEquals("COL_NAME", col.databaseColumnName)
        assertEquals("colName", col.camelCaseFieldName)
        assertEquals("int(11)", col.databaseType)
        assertEquals("Integer", col.javaType)
    }

    @Test
    fun fiveArgConstructorSetsAll() {
        val col = ColumnToField("@Id", "USER_ID", "userId", "int(11)", "Long")
        assertEquals("@Id", col.databaseIdType)
        assertEquals("USER_ID", col.databaseColumnName)
        assertEquals("userId", col.camelCaseFieldName)
        assertEquals("int(11)", col.databaseType)
        assertEquals("Long", col.javaType)
    }

    @Test
    fun dataClassEquality() {
        val a = ColumnToField("@Id", "ID", "id", null, "Long")
        val b = ColumnToField("@Id", "ID", "id", null, "Long")
        assertEquals(a, b)
    }

    @Test
    fun dataClassInequality() {
        val a = ColumnToField("@Id", "ID", "id", null, "Long")
        val b = ColumnToField("@ManyToOne", "ID", "id", null, "Integer")
        assertNotEquals(a, b)
    }

    @Test
    fun dataClassCopy() {
        val original = ColumnToField("@Id", "ID", "id", null, "Long")
        val copy = original.copy(javaType = "Integer")
        assertEquals("Integer", copy.javaType)
        assertEquals("Long", original.javaType)
    }

    @Test
    fun hashCodeConsistent() {
        val a = ColumnToField("@Id", "ID", null, null, "Long")
        val b = ColumnToField("@Id", "ID", null, null, "Long")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun toStringContainsFields() {
        val col = ColumnToField("@Id", "COL", "col", "int(11)", "Integer")
        val str = col.toString()
        assert(str.contains("@Id"))
        assert(str.contains("COL"))
        assert(str.contains("col"))
        assert(str.contains("Integer"))
    }

    @Test
    fun mutableFields() {
        val col = ColumnToField()
        col.databaseIdType = "@Id"
        col.databaseColumnName = "MY_COL"
        col.camelCaseFieldName = "myCol"
        col.databaseType = "varchar(50)"
        col.javaType = "String"
        assertEquals("@Id", col.databaseIdType)
        assertEquals("MY_COL", col.databaseColumnName)
        assertEquals("myCol", col.camelCaseFieldName)
        assertEquals("varchar(50)", col.databaseType)
        assertEquals("String", col.javaType)
    }
}
