package com.jakobmenke.bootrestgenerator.utils

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.regex.Pattern
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntityToRESTConstantsTest {

    // ── SUPPORTED_DATA_TYPES_REGEX ─────────────────────────────────────

    @Nested
    inner class SupportedDataTypesRegex {
        private val pattern = Pattern.compile(EntityToRESTConstants.SUPPORTED_DATA_TYPES_REGEX)

        @Test fun matchesVarchar() = assertTrue(pattern.matcher("varchar(255)").matches())
        @Test fun matchesVarcharNoLength() = assertTrue(pattern.matcher("varchar").matches())
        @Test fun matchesInt() = assertTrue(pattern.matcher("int(11)").matches())
        @Test fun matchesTinyint() = assertTrue(pattern.matcher("tinyint(3)").matches())
        @Test fun matchesBigint() = assertTrue(pattern.matcher("bigint(20)").matches())
        @Test fun matchesDatetime() = assertTrue(pattern.matcher("datetime").matches())
        @Test fun matchesTimestamp() = assertTrue(pattern.matcher("timestamp").matches())
        @Test fun matchesFloat() = assertTrue(pattern.matcher("float").matches())
        @Test fun matchesDouble() = assertTrue(pattern.matcher("double").matches())
        @Test fun matchesBit() = assertTrue(pattern.matcher("bit(1)").matches())
        @Test fun matchesTime() = assertTrue(pattern.matcher("time").matches())
        @Test fun caseInsensitive() = assertTrue(pattern.matcher("VARCHAR(100)").matches())
        @Test fun doesNotMatchText() = assertFalse(pattern.matcher("text").matches())
        @Test fun doesNotMatchBlob() = assertFalse(pattern.matcher("blob").matches())
        @Test fun doesNotMatchDecimal() = assertFalse(pattern.matcher("decimal(10,2)").matches())
    }

    // ── PRIMARY_FOREIGN_REGEX ──────────────────────────────────────────

    @Nested
    inner class PrimaryForeignRegex {
        private val pattern = Pattern.compile(EntityToRESTConstants.PRIMARY_FOREIGN_REGEX)

        @Test fun matchesPrimary() = assertTrue(pattern.matcher("PRIMARY").find())
        @Test fun matchesForeign() = assertTrue(pattern.matcher("FOREIGN").find())
        @Test fun doesNotMatchOther() = assertFalse(pattern.matcher("UNIQUE").find())
        @Test fun doesNotMatchLowercase() = assertFalse(pattern.matcher("primary").find())
        @Test fun doesNotMatchPartial() = assertFalse(pattern.matcher("XPRIMARY").find())
    }

    // ── INT_REGEX ──────────────────────────────────────────────────────

    @Nested
    inner class IntRegex {
        private val pattern = Pattern.compile(EntityToRESTConstants.INT_REGEX)

        @Test fun matchesInt11() = assertTrue(pattern.matcher("int(11)").matches())
        @Test fun matchesTinyint3() = assertTrue(pattern.matcher("tinyint(3)").matches())
        @Test fun doesNotMatchBigint() = assertFalse(pattern.matcher("bigint(20)").matches())
        @Test fun matchesBareInt() = assertTrue(pattern.matcher("int").matches())
    }

    // ── VARCHAR_REGEX ──────────────────────────────────────────────────

    @Nested
    inner class VarcharRegex {
        private val pattern = Pattern.compile(EntityToRESTConstants.VARCHAR_REGEX)

        @Test fun matchesVarchar255() = assertTrue(pattern.matcher("varchar(255)").matches())
        @Test fun matchesBareVarchar() = assertTrue(pattern.matcher("varchar").matches())
        @Test fun doesNotMatchChar() = assertFalse(pattern.matcher("char(10)").matches())
    }

    // ── BIGINT_REGEX ───────────────────────────────────────────────────

    @Nested
    inner class BigintRegex {
        private val pattern = Pattern.compile(EntityToRESTConstants.BIGINT_REGEX)

        @Test fun matchesBigint20() = assertTrue(pattern.matcher("bigint(20)").matches())
        @Test fun doesNotMatchInt() = assertFalse(pattern.matcher("int(11)").matches())
    }

    // ── DATETIME_REGEX ─────────────────────────────────────────────────

    @Nested
    inner class DatetimeRegex {
        private val pattern = Pattern.compile(EntityToRESTConstants.DATETIME_REGEX)

        @Test fun matchesDatetime() = assertTrue(pattern.matcher("datetime").matches())
        @Test fun doesNotMatchDate() = assertFalse(pattern.matcher("date").matches())
    }

    // ── TIMESTAMP_REGEX ────────────────────────────────────────────────

    @Nested
    inner class TimestampRegex {
        private val pattern = Pattern.compile(EntityToRESTConstants.TIMESTAMP_REGEX)

        @Test fun matchesTimestamp() = assertTrue(pattern.matcher("timestamp").matches())
        @Test fun doesNotMatchTime() = assertFalse(pattern.matcher("time").matches())
    }

    // ── TIME_REGEX ─────────────────────────────────────────────────────

    @Nested
    inner class TimeRegex {
        private val pattern = Pattern.compile(EntityToRESTConstants.TIME_REGEX)

        @Test fun matchesTime() = assertTrue(pattern.matcher("time").matches())
    }

    // ── FLOAT_REGEX / DOUBLE_REGEX / BIT_REGEX ─────────────────────────

    @Nested
    inner class FloatRegex {
        private val pattern = Pattern.compile(EntityToRESTConstants.FLOAT_REGEX)
        @Test fun matchesFloat() = assertTrue(pattern.matcher("float").matches())
        @Test fun doesNotMatchDouble() = assertFalse(pattern.matcher("double").matches())
    }

    @Nested
    inner class DoubleRegex {
        private val pattern = Pattern.compile(EntityToRESTConstants.DOUBLE_REGEX)
        @Test fun matchesDouble() = assertTrue(pattern.matcher("double").matches())
        @Test fun doesNotMatchFloat() = assertFalse(pattern.matcher("float").matches())
    }

    @Nested
    inner class BitRegex {
        private val pattern = Pattern.compile(EntityToRESTConstants.BIT_REGEX)
        @Test fun matchesBit1() = assertTrue(pattern.matcher("bit(1)").matches())
    }

    // ── FOREIGN_KEY_REFERENCES_REGEX ───────────────────────────────────

    @Nested
    inner class ForeignKeyReferencesRegex {
        private val pattern = Pattern.compile(EntityToRESTConstants.FOREIGN_KEY_REFERENCES_REGEX)

        @Test
        fun capturesAllGroups() {
            val matcher = pattern.matcher("FOREIGN KEY (`MODULE_ID`) REFERENCES `MODULE` (`MODULE_ID`)")
            assertTrue(matcher.find())
            assertEquals("`MODULE_ID`", matcher.group(1))
            assertEquals("`MODULE`", matcher.group(2))
            assertEquals("`MODULE_ID`", matcher.group(3))
        }

        @Test
        fun multiWordTable() {
            val matcher = pattern.matcher("FOREIGN KEY (`LANGUAGE_LEVEL_ID`) REFERENCES `LANGUAGE_LEVEL` (`LANGUAGE_LEVEL_ID`)")
            assertTrue(matcher.find())
            assertEquals("`LANGUAGE_LEVEL_ID`", matcher.group(1))
            assertEquals("`LANGUAGE_LEVEL`", matcher.group(2))
        }
    }

    // ── PRIMARY_KEY_S_S ────────────────────────────────────────────────

    @Nested
    inner class PrimaryKeySS {
        private val pattern = Pattern.compile(EntityToRESTConstants.PRIMARY_KEY_S_S)

        @Test
        fun capturesColumnName() {
            val matcher = pattern.matcher("PRIMARY KEY (`USER_ID`)")
            assertTrue(matcher.matches())
            assertEquals("`USER_ID`", matcher.group(1))
        }

        @Test
        fun matchesWithTrailingContent() {
            val matcher = pattern.matcher("PRIMARY KEY (`ID`) extra stuff here")
            assertTrue(matcher.matches())
            assertEquals("`ID`", matcher.group(1))
        }
    }

    // ── Constants values ───────────────────────────────────────────────

    @Test fun pkDataType() = assertEquals("Long", EntityToRESTConstants.PK_DATA_TYPE)
    @Test fun fkDataType() = assertEquals("Integer", EntityToRESTConstants.FK_DATA_TYPE)
    @Test fun pkId() = assertEquals("@Id", EntityToRESTConstants.PK_ID)
    @Test fun fkId() = assertEquals("@ManyToOne", EntityToRESTConstants.FK_ID)
    @Test fun dbEscapeChar() = assertEquals("`", EntityToRESTConstants.DB_ESCAPE_CHARACTER)
    @Test fun underscore() = assertEquals("_", EntityToRESTConstants.UNDERSCORE)
    @Test fun spaceChar() = assertEquals(" ", EntityToRESTConstants.SPACE_CHAR)
}
