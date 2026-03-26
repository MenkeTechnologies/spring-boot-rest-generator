package com.jakobmenke.bootrestgenerator.utils

import org.junit.jupiter.api.Test
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroovyConfigurationTest {

    @Test
    fun configurationReadsGroovyLanguageProperty() {
        val props = Properties().apply {
            setProperty("target.folder", "/tmp/src")
            setProperty("target.package", "com/example")
            setProperty("file.name", "schema.sql")
            setProperty("target.language", "groovy")
        }
        val config = Configuration(props)
        assertEquals("groovy", config.language)
    }

    @Test
    fun defaultFolderForGroovy() {
        assertEquals("build/generated/src/main/groovy/", Configuration.defaultFolderForLanguage("groovy"))
    }

    @Test
    fun missingTargetFolderDefaultsToGroovyPath() {
        val props = Properties().apply {
            setProperty("target.package", "com/example")
            setProperty("file.name", "schema.sql")
            setProperty("target.language", "groovy")
        }
        val config = Configuration(props)
        assertEquals("build/generated/src/main/groovy/", config.srcFolder)
    }

    @Test
    fun globalsIsGroovyWhenLanguageSetToGroovy() {
        val original = Globals.LANGUAGE
        try {
            Globals.LANGUAGE = "groovy"
            assertTrue(Globals.isGroovy)
            assertEquals(".groovy", Globals.fileExtension)
        } finally {
            Globals.LANGUAGE = original
        }
    }

    @Test
    fun globalsIsNotGroovyWhenLanguageSetToJava() {
        val original = Globals.LANGUAGE
        try {
            Globals.LANGUAGE = "java"
            assertFalse(Globals.isGroovy)
            assertEquals(".java", Globals.fileExtension)
        } finally {
            Globals.LANGUAGE = original
        }
    }

    @Test
    fun globalsIsNotGroovyWhenLanguageSetToKotlin() {
        val original = Globals.LANGUAGE
        try {
            Globals.LANGUAGE = "kotlin"
            assertFalse(Globals.isGroovy)
            assertEquals(".kt", Globals.fileExtension)
        } finally {
            Globals.LANGUAGE = original
        }
    }

    @Test
    fun globalsIsGroovyCaseInsensitive() {
        val original = Globals.LANGUAGE
        try {
            Globals.LANGUAGE = "Groovy"
            assertTrue(Globals.isGroovy)
            Globals.LANGUAGE = "GROOVY"
            assertTrue(Globals.isGroovy)
        } finally {
            Globals.LANGUAGE = original
        }
    }

    @Test
    fun groovyIsNotKotlin() {
        val original = Globals.LANGUAGE
        try {
            Globals.LANGUAGE = "groovy"
            assertTrue(Globals.isGroovy)
            assertFalse(Globals.isKotlin)
        } finally {
            Globals.LANGUAGE = original
        }
    }

    @Test
    fun dataClassEqualityWithGroovyLanguage() {
        val a = Configuration(srcFolder = "/tmp", targetPackage = "com/x", fileName = "a.sql", language = "groovy")
        val b = Configuration(srcFolder = "/tmp", targetPackage = "com/x", fileName = "a.sql", language = "groovy")
        assertEquals(a, b)
    }

    @Test
    fun dataClassCopyWithGroovyLanguage() {
        val original = Configuration(srcFolder = "/tmp", targetPackage = "com/x", fileName = "a.sql", language = "java")
        val copy = original.copy(language = "groovy")
        assertEquals("groovy", copy.language)
        assertEquals("java", original.language)
    }
}
