package com.jakobmenke.bootrestgenerator.utils

import org.junit.jupiter.api.Test
import java.util.Properties
import kotlin.test.assertEquals

class KotlinConfigurationTest {

    @Test
    fun configurationReadsLanguageProperty() {
        val props = Properties().apply {
            setProperty("target.folder", "/tmp/src")
            setProperty("target.package", "com/example")
            setProperty("file.name", "schema.sql")
            setProperty("target.language", "kotlin")
        }
        val config = Configuration(props)
        assertEquals("kotlin", config.language)
    }

    @Test
    fun configurationDefaultsToJavaWhenMissing() {
        val props = Properties().apply {
            setProperty("target.folder", "/tmp/src")
            setProperty("target.package", "com/example")
            setProperty("file.name", "schema.sql")
        }
        val config = Configuration(props)
        assertEquals("java", config.language)
    }

    @Test
    fun configurationFromRealConfigHasLanguage() {
        val props = Configuration.readConfig("config.properties")!!
        val config = Configuration(props)
        assertEquals("kotlin", config.language)
    }

    @Test
    fun globalsIsKotlinWhenLanguageSetToKotlin() {
        val original = Globals.LANGUAGE
        try {
            Globals.LANGUAGE = "kotlin"
            assertEquals(true, Globals.isKotlin)
            assertEquals(".kt", Globals.fileExtension)
        } finally {
            Globals.LANGUAGE = original
        }
    }

    @Test
    fun globalsIsNotKotlinWhenLanguageSetToJava() {
        val original = Globals.LANGUAGE
        try {
            Globals.LANGUAGE = "java"
            assertEquals(false, Globals.isKotlin)
            assertEquals(".java", Globals.fileExtension)
        } finally {
            Globals.LANGUAGE = original
        }
    }

    @Test
    fun globalsIsKotlinCaseInsensitive() {
        val original = Globals.LANGUAGE
        try {
            Globals.LANGUAGE = "Kotlin"
            assertEquals(true, Globals.isKotlin)
            Globals.LANGUAGE = "KOTLIN"
            assertEquals(true, Globals.isKotlin)
        } finally {
            Globals.LANGUAGE = original
        }
    }

    @Test
    fun dataClassEqualityWithLanguage() {
        val a = Configuration(srcFolder = "/tmp", targetPackage = "com/x", fileName = "a.sql", language = "kotlin")
        val b = Configuration(srcFolder = "/tmp", targetPackage = "com/x", fileName = "a.sql", language = "kotlin")
        assertEquals(a, b)
    }

    @Test
    fun dataClassCopyWithLanguage() {
        val original = Configuration(srcFolder = "/tmp", targetPackage = "com/x", fileName = "a.sql", language = "java")
        val copy = original.copy(language = "kotlin")
        assertEquals("kotlin", copy.language)
        assertEquals("java", original.language)
    }
}
