package com.jakobmenke.bootrestgenerator

import com.jakobmenke.bootrestgenerator.utils.Configuration
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConfigurationTest {
    @Test
    fun readValidConfig() {
        val properties = Configuration.readConfig("config.properties")
        assertNotNull(properties)
    }

    @Test
    fun readInvalidConfig() {
        val properties = Configuration.readConfig("ifsalf.properties")
        assertNull(properties)
    }

    @Test
    fun readNullConfig() {
        val properties = Configuration.readConfig("ifsalf.properties")
        assertNull(properties)
    }
}
