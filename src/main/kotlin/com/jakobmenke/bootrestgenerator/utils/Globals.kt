package com.jakobmenke.bootrestgenerator.utils

object Globals {
    lateinit var PACKAGE: String
    lateinit var SRC_FOLDER: String
    lateinit var FILE_NAME: String
    var LANGUAGE: String = "kotlin"

    val isKotlin: Boolean get() = LANGUAGE.equals("kotlin", ignoreCase = true)
    val fileExtension: String get() = if (isKotlin) ".kt" else ".java"
}
