package com.jakobmenke.bootrestgenerator.dto

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class EntityTest {

    @Test
    fun defaultConstructor() {
        val entity = Entity()
        assertEquals("", entity.tableName)
        assertEquals("", entity.entityName)
        assertTrue(entity.columns.isEmpty())
    }

    @Test
    fun namedParameters() {
        val entity = Entity(tableName = "USER", entityName = "User")
        assertEquals("USER", entity.tableName)
        assertEquals("User", entity.entityName)
        assertTrue(entity.columns.isEmpty())
    }

    @Test
    fun addColumnsToMutableList() {
        val entity = Entity(tableName = "T", entityName = "T")
        entity.columns.add(ColumnToField("COL1", "col1", "int(11)", "Integer"))
        entity.columns.add(ColumnToField("COL2", "col2", "varchar(50)", "String"))
        assertEquals(2, entity.columns.size)
    }

    @Test
    fun dataClassEquality() {
        val a = Entity(tableName = "USER", entityName = "User")
        val b = Entity(tableName = "USER", entityName = "User")
        assertEquals(a, b)
    }

    @Test
    fun dataClassInequalityDifferentName() {
        val a = Entity(tableName = "USER", entityName = "User")
        val b = Entity(tableName = "LANGUAGE", entityName = "Language")
        assertNotEquals(a, b)
    }

    @Test
    fun dataClassCopy() {
        val original = Entity(tableName = "USER", entityName = "User")
        val copy = original.copy(entityName = "ModifiedUser")
        assertEquals("ModifiedUser", copy.entityName)
        assertEquals("User", original.entityName)
    }

    @Test
    fun mutableTableName() {
        val entity = Entity()
        entity.tableName = "NEW_TABLE"
        assertEquals("NEW_TABLE", entity.tableName)
    }

    @Test
    fun mutableEntityName() {
        val entity = Entity()
        entity.entityName = "NewEntity"
        assertEquals("NewEntity", entity.entityName)
    }

    @Test
    fun entityWithColumns() {
        val cols = mutableListOf(
            ColumnToField("@Id", "ID", "id", "int(11)", "Long"),
            ColumnToField("NAME", "name", "varchar(150)", "String")
        )
        val entity = Entity(tableName = "USER", entityName = "User", columns = cols)
        assertEquals(2, entity.columns.size)
        assertEquals("@Id", entity.columns[0].databaseIdType)
    }

    @Test
    fun toStringContainsFields() {
        val entity = Entity(tableName = "USER", entityName = "User")
        val str = entity.toString()
        assertTrue(str.contains("USER"))
        assertTrue(str.contains("User"))
    }
}
