package com.jakobmenke.bootrestgenerator.utils

import org.junit.jupiter.api.Test
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConfigurationTest {

    // ── readConfig ─────────────────────────────────────────────────────

    @Test
    fun readValidConfigReturnsProperties() {
        val properties = Configuration.readConfig("config.properties")
        assertNotNull(properties)
    }

    @Test
    fun readValidConfigHasTargetFolder() {
        val properties = Configuration.readConfig("config.properties")!!
        assertNotNull(properties.getProperty("target.folder"))
    }

    @Test
    fun readValidConfigHasTargetPackage() {
        val properties = Configuration.readConfig("config.properties")!!
        assertNotNull(properties.getProperty("target.package"))
    }

    @Test
    fun readValidConfigHasFileName() {
        val properties = Configuration.readConfig("config.properties")!!
        assertNotNull(properties.getProperty("file.name"))
    }

    @Test
    fun readInvalidConfigReturnsNull() {
        assertNull(Configuration.readConfig("nonexistent.properties"))
    }

    @Test
    fun readEmptyStringConfigReturnsEmptyProperties() {
        // Empty string resolves to a valid (empty) resource stream
        val result = Configuration.readConfig("")
        assertNotNull(result)
    }

    // ── Configuration constructor ──────────────────────────────────────

    @Test
    fun constructFromProperties() {
        val props = Properties().apply {
            setProperty("target.folder", "/tmp/src")
            setProperty("target.package", "com/example")
            setProperty("file.name", "schema.sql")
        }
        val config = Configuration(props)
        assertEquals("/tmp/src", config.srcFolder)
        assertEquals("com/example", config.targetPackage)
        assertEquals("schema.sql", config.fileName)
    }

    @Test
    fun constructFromRealConfig() {
        val props = Configuration.readConfig("config.properties")!!
        val config = Configuration(props)
        assertEquals("com/reallingua/api", config.targetPackage)
        assertEquals("dump.sql", config.fileName)
    }

    // ── Data class behavior ────────────────────────────────────────────

    @Test
    fun dataClassEquality() {
        val a = Configuration(srcFolder = "/tmp", targetPackage = "com/x", fileName = "a.sql")
        val b = Configuration(srcFolder = "/tmp", targetPackage = "com/x", fileName = "a.sql")
        assertEquals(a, b)
    }

    @Test
    fun dataClassCopy() {
        val original = Configuration(srcFolder = "/tmp", targetPackage = "com/x", fileName = "a.sql")
        val copy = original.copy(fileName = "b.sql")
        assertEquals("b.sql", copy.fileName)
        assertEquals("a.sql", original.fileName)
    }
}
