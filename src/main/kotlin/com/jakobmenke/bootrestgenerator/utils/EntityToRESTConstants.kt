package com.jakobmenke.bootrestgenerator.utils

object EntityToRESTConstants {
    const val DB_ESCAPE_CHARACTER = "`"
    const val SUPPORTED_DATA_TYPES_REGEX = "\\b(?i:varchar|tinyint|bigint|int|double|float|time|datetime|timestamp|bit)[()\\d]*"
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
    const val FOREIGN_KEY_REFERENCES_REGEX = "^FOREIGN KEY\\s*\\((\\S+)\\)\\s+REFERENCES\\s*(\\S+)\\s*\\((\\S+)\\)"
    const val PRIMARY_KEY_S_S = "^PRIMARY KEY\\s*\\((\\S+)\\).*"
    const val UNDERSCORE = "_"
    const val SPACE_CHAR = " "
    const val PK_DATA_TYPE = "Long"
    const val FK_DATA_TYPE = "Integer"
    const val PK_ID = "@Id"
    const val FK_ID = "@ManyToOne"
}
