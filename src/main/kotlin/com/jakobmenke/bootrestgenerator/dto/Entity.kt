package com.jakobmenke.bootrestgenerator.dto

data class Entity(
    var tableName: String = "",
    var entityName: String = "",
    val columns: MutableList<ColumnToField> = mutableListOf()
)
