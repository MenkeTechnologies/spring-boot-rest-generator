package com.jakobmenke.bootrestgenerator;

import com.jakobmenke.bootrestgenerator.utils.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigurationTest {

    @org.junit.jupiter.api.Test
    void readValidConfig() {
        Properties properties = Configuration.readConfig("config.properties");
        assertNotNull(properties);
    }
    @Test
    void readInvalidConfig() {
        Properties properties = Configuration.readConfig("ifsalf.properties");
        assertNull(properties);
    }
    @Test
    void readNullConfig() {
        Properties properties = Configuration.readConfig("ifsalf.properties");
        assertNull(properties);
    }
}