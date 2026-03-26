package com.jakobmenke.bootrestgenerator.utils

object EntityToRESTConstants {
    const val DB_ESCAPE_CHARACTER = "`"
    const val PG_DB_ESCAPE_CHARACTER = "\""
    const val SUPPORTED_DATA_TYPES_REGEX = "\\b(?i:varchar|tinyint|bigint|bigserial|int|integer|smallint|serial|double|float|real|numeric|time|datetime|timestamp|date|bit|boolean|bool|text|blob)[()\\d,]*"
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

    const val FOREIGN_KEY_REFERENCES_REGEX = "^FOREIGN KEY\\s*\\(([^)]+)\\)\\s+REFERENCES\\s+([^(\\s]+)\\s*\\(([^)]+)\\)"
    const val PRIMARY_KEY_S_S = "^PRIMARY KEY\\s*\\(([^)]+)\\).*"
    const val UNDERSCORE = "_"
    const val SPACE_CHAR = " "
    const val PK_DATA_TYPE = "Long"
    const val FK_DATA_TYPE = "Integer"
    const val PK_ID = "@Id"
    const val FK_ID = "@ManyToOne"
}
