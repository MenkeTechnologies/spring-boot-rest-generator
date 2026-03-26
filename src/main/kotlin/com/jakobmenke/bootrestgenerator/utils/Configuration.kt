package com.jakobmenke.bootrestgenerator.utils

import com.jakobmenke.bootrestgenerator.templates.Templates
import java.util.Properties

data class Configuration(
    val srcFolder: String,
    val targetPackage: String,
    val fileName: String,
    val language: String = "java",
    val databaseType: String = "mysql"
) {
    constructor(props: Properties) : this(
        srcFolder = props.getProperty("target.folder")
            ?: defaultFolderForLanguage(props.getProperty("target.language", "java")),
        targetPackage = props.getProperty("target.package"),
        fileName = props.getProperty("file.name"),
        language = props.getProperty("target.language", "java"),
        databaseType = props.getProperty("database.type", "mysql")
    )

    companion object {
        fun defaultFolderForLanguage(language: String): String = when (language) {
            "kotlin" -> "build/generated/src/main/kotlin/"
            "groovy" -> "build/generated/src/main/groovy/"
            else -> "build/generated/src/main/java/"
        }

        fun readConfig(configFileName: String): Properties? {
            val inputStream = Templates::class.java.classLoader.getResourceAsStream(configFileName)
                ?: return null
            return Properties().apply { load(inputStream) }
        }
    }
}
