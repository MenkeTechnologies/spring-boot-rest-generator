package com.jakobmenke.bootrestgenerator.utils

import java.util.regex.Pattern

object EntityToRESTConstants {
    const val DB_ESCAPE_CHARACTER = "`"
    const val PG_DB_ESCAPE_CHARACTER = "\""
    const val MSSQL_DB_ESCAPE_OPEN = "["
    const val MSSQL_DB_ESCAPE_CLOSE = "]"
    const val SUPPORTED_DATA_TYPES_REGEX = "\\b(?i:nvarchar|nchar|ntext|varchar|uniqueidentifier|datetimeoffset|smalldatetime|datetime2|datetime|smallmoney|tinyint|smallint|bigint|bigserial|integer|serial|decimal|numeric|timestamp|money|float|image|double|bit|boolean|bool|real|date|time|text|blob|char|xml|int)[()\\d,]*"
    const val PRIMARY_FOREIGN_REGEX = "^(PRIMARY|FOREIGN)"
    const val INT_REGEX = "\\b(tinyint|int)[()\\d]*"
    const val VARCHAR_REGEX = "\\bvarchar[()\\d]*"
    const val BIGINT_REGEX = "\\bbigint[()\\d]*"
    const val DATETIME_REGEX = "\\bdatetime[()\\d]*"
    const val BIT_REGEX = "\\bbit[()\\d]*"
    const val FLOAT_REGEX = "\\bfloat[()\\d]*"
    const val DOUBLE_REGEX = "\\bdouble[()\\d]*"
    const val TIME_REGEX = "\\btime[()\\d]*"
    const val TIMESTAMP_REGEX = "\\btimestamp[()\\d]*"

    // PostgreSQL type regexes
    const val PG_INTEGER_REGEX = "\\b(integer|smallint|serial)[()\\d]*"
    const val PG_BIGINT_REGEX = "\\b(bigint|bigserial)[()\\d]*"
    const val PG_TEXT_REGEX = "\\btext"
    const val PG_BOOLEAN_REGEX = "\\b(boolean|bool)"
    const val PG_NUMERIC_REGEX = "\\bnumeric[()\\d,]*"
    const val PG_REAL_REGEX = "\\breal[()\\d]*"
    const val PG_DATE_REGEX = "\\bdate"

    // MSSQL type regexes
    const val MSSQL_NVARCHAR_REGEX = "\\b(nvarchar|nchar|ntext|char)[()\\d]*"
    const val MSSQL_MONEY_REGEX = "\\b(money|smallmoney)"
    const val MSSQL_UNIQUEIDENTIFIER_REGEX = "\\buniqueidentifier"
    const val MSSQL_DATETIME2_REGEX = "\\b(datetime2|datetimeoffset|smalldatetime)[()\\d]*"
    const val MSSQL_DECIMAL_REGEX = "\\bdecimal[()\\d,]*"
    const val MSSQL_IMAGE_REGEX = "\\b(image|xml)"

    const val FOREIGN_KEY_REFERENCES_REGEX = "^FOREIGN KEY\\s*\\(([^)]+)\\)\\s+REFERENCES\\s+([^(\\s]+)\\s*\\(([^)]+)\\)"
    const val PRIMARY_KEY_S_S = "^PRIMARY KEY\\s*\\(([^)]+)\\).*"
    const val UNDERSCORE = "_"
    const val SPACE_CHAR = " "
    const val PK_DATA_TYPE = "Long"
    const val FK_DATA_TYPE = "Integer"
    const val PK_ID = "@Id"
    const val FK_ID = "@ManyToOne"

    // Pre-compiled patterns
    val SUPPORTED_DATA_TYPES_PATTERN: Pattern = Pattern.compile(SUPPORTED_DATA_TYPES_REGEX)
    val PRIMARY_FOREIGN_PATTERN: Pattern = Pattern.compile(PRIMARY_FOREIGN_REGEX)
    val VARCHAR_PATTERN: Pattern = Pattern.compile(VARCHAR_REGEX)
    val MSSQL_NVARCHAR_PATTERN: Pattern = Pattern.compile(MSSQL_NVARCHAR_REGEX)
    val PG_TEXT_PATTERN: Pattern = Pattern.compile(PG_TEXT_REGEX)
    val MSSQL_UNIQUEIDENTIFIER_PATTERN: Pattern = Pattern.compile(MSSQL_UNIQUEIDENTIFIER_REGEX)
    val MSSQL_IMAGE_PATTERN: Pattern = Pattern.compile(MSSQL_IMAGE_REGEX)
    val PG_BIGINT_PATTERN: Pattern = Pattern.compile(PG_BIGINT_REGEX)
    val BIGINT_PATTERN: Pattern = Pattern.compile(BIGINT_REGEX)
    val INT_PATTERN: Pattern = Pattern.compile(INT_REGEX)
    val PG_INTEGER_PATTERN: Pattern = Pattern.compile(PG_INTEGER_REGEX)
    val MSSQL_DATETIME2_PATTERN: Pattern = Pattern.compile(MSSQL_DATETIME2_REGEX)
    val DATETIME_PATTERN: Pattern = Pattern.compile(DATETIME_REGEX)
    val PG_DATE_PATTERN: Pattern = Pattern.compile(PG_DATE_REGEX)
    val PG_BOOLEAN_PATTERN: Pattern = Pattern.compile(PG_BOOLEAN_REGEX)
    val BIT_PATTERN: Pattern = Pattern.compile(BIT_REGEX)
    val FLOAT_PATTERN: Pattern = Pattern.compile(FLOAT_REGEX)
    val PG_REAL_PATTERN: Pattern = Pattern.compile(PG_REAL_REGEX)
    val DOUBLE_PATTERN: Pattern = Pattern.compile(DOUBLE_REGEX)
    val PG_NUMERIC_PATTERN: Pattern = Pattern.compile(PG_NUMERIC_REGEX)
    val MSSQL_DECIMAL_PATTERN: Pattern = Pattern.compile(MSSQL_DECIMAL_REGEX)
    val MSSQL_MONEY_PATTERN: Pattern = Pattern.compile(MSSQL_MONEY_REGEX)
    val TIME_PATTERN: Pattern = Pattern.compile(TIME_REGEX)
    val TIMESTAMP_PATTERN: Pattern = Pattern.compile(TIMESTAMP_REGEX)
    val FOREIGN_KEY_REFERENCES_PATTERN: Pattern = Pattern.compile(FOREIGN_KEY_REFERENCES_REGEX)
    val PRIMARY_KEY_PATTERN: Pattern = Pattern.compile(PRIMARY_KEY_S_S)
}
