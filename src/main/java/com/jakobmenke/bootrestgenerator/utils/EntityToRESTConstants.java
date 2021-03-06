package com.jakobmenke.bootrestgenerator.utils;

public class EntityToRESTConstants {
    public static final String DB_ESCAPE_CHARACTER = "`";
    public static final String SUPPORTED_DATA_TYPES_REGEX = "\\b(?i:varchar|tinyint|bigint|int|double|float|time|datetime|timestamp|bit)[()\\d]*";
    public static final String PRIMARY_FOREIGN_REGEX = "^(PRIMARY|FOREIGN)";
    public static final String INT_REGEX = "\\b(tinyint|int)[()\\d]*";
    public static final String VARCHAR_REGEX = "\\bvarchar[()\\d]*";
    public static final String BIGINT_REGEX = "\\bbigint[()\\d]*";
    public static final String DATETIME_REGEX = "\\bdatetime[()\\d]*";
    public static final String BIT_REGEX = "\\bbit[()\\d]*";
    public static final String FLOAT_REGEX = "\\bfloat[()\\d]*";
    public static final String DOUBLE_REGEX = "\\bdouble[()\\d]*";
    public static final String TIME_REGEX = "\\btime[()\\d]*";
    public static final String TIMESTAMP_REGEX = "\\btimestamp[()\\d]*";
    public static final String FOREIGN_KEY_REFERENCES_REGEX = "^FOREIGN KEY\\s*\\((\\S+)\\)\\s+REFERENCES\\s*(\\S+)\\s*\\((\\S+)\\)";
    public static final String PRIMARY_KEY_S_S = "^PRIMARY KEY\\s*\\((\\S+)\\).*";
    public static final String UNDERSCORE = "_";
    public static final String SPACE_CHAR = " ";
    public static final String PK_DATA_TYPE = "Long";
    public static final String FK_DATA_TYPE = "Integer";
    public static final String PK_ID = "@Id";
    public static final String FK_ID = "@ManyToOne";
}
