package com.jakobmenke.bootrestgenerator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTest {
    @Test
    void getKey() {
        String primaryKey = "PRIMARY KEY (`ACTIVITY_ID`)";
        ColumnToField expected = new ColumnToField("@Id", "ACTIVITY_ID", null, null, "Integer");
        assertEquals(expected, Util.getId(primaryKey));
    }

    @Test
    void getKey2() {
        String foreignKey = "FOREIGN KEY (`TRANSCRIPT_ID`) REFERENCES `TRANSCRIPT` (`TRANSCRIPT_ID`) ";
        ColumnToField expected1 = new ColumnToField("@ManyToOne", "TRANSCRIPT_ID", "transcriptId", null, "Transcript");
        assertEquals(expected1, Util.getId(foreignKey));
    }

    @Test
    void getKey3() {
        ColumnToField expected2 = new ColumnToField("@ManyToOne", "TESTING_ID", "testingId", null, "Testing");
        String foreignKey1 = "FOREIGN KEY (`TESTING_ID`) REFERENCES `TESTING` (`TESTING_ID`)";
        assertEquals(expected2, Util.getId(foreignKey1));
    }

    @Test
    void getKey4() {

        ColumnToField expected3 = new ColumnToField("@ManyToOne", "TESTING_NO_BAD_ID", "testingNoBadId", null, "TestingNoBad");
        String foreignKey2 = "FOREIGN KEY (`TESTING_NO_BAD_ID`) REFERENCES `TESTING_NO_BAD` (`TESTING_NO_BAD_ID`)";
        assertEquals(expected3, Util.getId(foreignKey2));
    }
    @Test
    void toCaps() {
        String testCase0 = "this will be capitalized";
        String testCase1 = "";
        String testCase2 = "1019 test";
        assertEquals("This will be capitalized", Util.firstLetterToCaps(testCase0));
        assertEquals("", Util.firstLetterToCaps(testCase1));
        assertEquals("1019 test", Util.firstLetterToCaps(testCase2));
    }

    @Test
    void camelName() {
        assertEquals("helloWorld", Util.camelName("HELLO_WorLD"));
        assertEquals("helloW", Util.camelName("HELLO_W"));
    }
}
