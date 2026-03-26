package com.jakobmenke.bootrestgenerator.utils

import com.jakobmenke.bootrestgenerator.templates.Templates
import java.util.Properties

data class Configuration(
    val srcFolder: String,
    val targetPackage: String,
    val fileName: String
) {
    constructor(props: Properties) : this(
        srcFolder = props.getProperty("target.folder"),
        targetPackage = props.getProperty("target.package"),
        fileName = props.getProperty("file.name")
    )

    companion object {
        fun readConfig(configFileName: String): Properties? {
            val inputStream = Templates::class.java.classLoader.getResourceAsStream(configFileName)
                ?: return null
            return Properties().apply { load(inputStream) }
        }
    }
}
