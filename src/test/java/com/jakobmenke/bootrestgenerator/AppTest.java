package com.jakobmenke.bootrestgenerator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {
    @Test
    void testGetKey() {
        String primaryKey = "PRIMARY KEY (`ACTIVITY_ID`)";
        Column expected = new Column("@Id", "ACTIVITY_ID", "activity", null, null);
        assertEquals(expected, App.getId(primaryKey));

        String foreignKey = "FOREIGN KEY (`TRANSCRIPT_ID`)";
        Column expected1 = new Column("@ManyToOne", "TRANSCRIPT_ID", "transcript", null, "Transcript");
        assertEquals(expected1, App.getId(foreignKey));

        Column expected2 = new Column("@ManyToOne", "TESTING_ID", "testing", null, "Testing");
        String foreignKey1 = "FOREIGN KEY (`TESTING_ID`)";
        assertEquals(expected2, App.getId(foreignKey1));
    }
    @Test
    void testToCaps() {
        String testCase0 = "this WILL Be Capitalized";
        String testCase1 = "";
        String testCase2 = "1019 test";
        assertEquals("This will be capitalized", App.firstLetterToCaps(testCase0));
        assertEquals("", App.firstLetterToCaps(testCase1));
        assertEquals("1019 test", App.firstLetterToCaps(testCase2));
    }
}
