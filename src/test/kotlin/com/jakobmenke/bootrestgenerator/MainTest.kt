package com.jakobmenke.bootrestgenerator

import com.jakobmenke.bootrestgenerator.dto.ColumnToField
import com.jakobmenke.bootrestgenerator.utils.Util
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MainTest {
    @Test
    fun getKey() {
        val primaryKey = "PRIMARY KEY (`ACTIVITY_ID`)"
        val expected = ColumnToField("@Id", "ACTIVITY_ID", null, null, "Long")
        assertEquals(expected, Util.getId(primaryKey))
    }

    @Test
    fun getKey2() {
        val foreignKey = "FOREIGN KEY (`TRANSCRIPT_ID`) REFERENCES `TRANSCRIPT` (`TRANSCRIPT_ID`) "
        val expected = ColumnToField("@ManyToOne", "TRANSCRIPT_ID", "transcriptId", null, "Integer")
        assertEquals(expected, Util.getId(foreignKey))
    }

    @Test
    fun getKey3() {
        val expected = ColumnToField("@ManyToOne", "TESTING_ID", "testingId", null, "Integer")
        val foreignKey = "FOREIGN KEY (`TESTING_ID`) REFERENCES `TESTING` (`TESTING_ID`)"
        assertEquals(expected, Util.getId(foreignKey))
    }

    @Test
    fun getKey4() {
        val expected = ColumnToField("@ManyToOne", "TESTING_NO_BAD_ID", "testingNoBadId", null, "Integer")
        val foreignKey = "FOREIGN KEY (`TESTING_NO_BAD_ID`) REFERENCES `TESTING_NO_BAD` (`TESTING_NO_BAD_ID`)"
        assertEquals(expected, Util.getId(foreignKey))
    }

    @Test
    fun toCaps() {
        assertEquals("This will be capitalized", Util.firstLetterToCaps("this will be capitalized"))
        assertEquals("", Util.firstLetterToCaps(""))
        assertEquals("1019 test", Util.firstLetterToCaps("1019 test"))
    }

    @Test
    fun camelName() {
        assertEquals("helloWorld", Util.camelName("HELLO_WorLD"))
        assertEquals("helloW", Util.camelName("HELLO_W"))
    }
}
