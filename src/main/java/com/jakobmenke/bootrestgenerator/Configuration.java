package com.jakobmenke.bootrestgenerator;

import lombok.Data;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Data
class Configuration {
    private final String srcFolder;
    private final String targetPackage;
    private final String fileName;

    Configuration(Properties props) {
        this.fileName = props.getProperty("file.name");
        this.srcFolder = props.getProperty("target.folder");
        this.targetPackage = props.getProperty("target.package");
    }

    static Properties readConfig(String configFileName) {
        ClassLoader classLoader = Templates.class.getClassLoader();
        InputStream in = classLoader.getResourceAsStream(configFileName);
        if (in == null) {
            return null;
        }
        Properties props = new Properties();
        try {
            props.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return props;
    }
}
