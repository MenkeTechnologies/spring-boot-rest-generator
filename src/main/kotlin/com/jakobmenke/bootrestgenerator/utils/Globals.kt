package com.jakobmenke.bootrestgenerator.utils

object Globals {
    lateinit var PACKAGE: String
    lateinit var SRC_FOLDER: String
    lateinit var FILE_NAME: String
    var LANGUAGE: String = "java"
    var DB_TYPE: String = "mysql"

    val isKotlin: Boolean get() = LANGUAGE.equals("kotlin", ignoreCase = true)
    val isGroovy: Boolean get() = LANGUAGE.equals("groovy", ignoreCase = true)
    val isPostgresql: Boolean get() = DB_TYPE.equals("postgresql", ignoreCase = true)
    val isSqlite: Boolean get() = DB_TYPE.equals("sqlite", ignoreCase = true)
    val isMssql: Boolean get() = DB_TYPE.equals("mssql", ignoreCase = true)
    val escapeCharacter: String get() = when {
        isPostgresql || isSqlite -> EntityToRESTConstants.PG_DB_ESCAPE_CHARACTER
        isMssql -> EntityToRESTConstants.MSSQL_DB_ESCAPE_OPEN
        else -> EntityToRESTConstants.DB_ESCAPE_CHARACTER
    }
    val fileExtension: String get() = when {
        isKotlin -> ".kt"
        isGroovy -> ".groovy"
        else -> ".java"
    }
}
