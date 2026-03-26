package com.jakobmenke.bootrestgenerator.dto

data class ColumnToField(
    var databaseIdType: String? = null,
    var databaseColumnName: String? = null,
    var camelCaseFieldName: String? = null,
    var databaseType: String? = null,
    var javaType: String? = null
) {
    constructor(
        databaseColumnName: String?,
        camelCaseFieldName: String?,
        databaseType: String?,
        javaType: String?
    ) : this(
        databaseIdType = null,
        databaseColumnName = databaseColumnName,
        camelCaseFieldName = camelCaseFieldName,
        databaseType = databaseType,
        javaType = javaType
    )
}
