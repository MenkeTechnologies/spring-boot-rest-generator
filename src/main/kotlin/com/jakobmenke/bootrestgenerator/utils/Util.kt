package com.jakobmenke.bootrestgenerator.utils

import com.jakobmenke.bootrestgenerator.dto.ColumnToField
import com.jakobmenke.bootrestgenerator.dto.Entity
import java.io.InputStream
import java.util.Scanner
import java.util.regex.Pattern

object Util {
    fun parseWords(entities: MutableList<Entity>, words: MutableList<String>) {
        val pattern = Pattern.compile(EntityToRESTConstants.SUPPORTED_DATA_TYPES_REGEX)

        for (i in words.indices) {
            val word = words[i]

            if (word.equals("create", ignoreCase = true)) {
                val nextWord = words[i + 1]
                if (nextWord.equals("table", ignoreCase = true)) {
                    val tableWord = words[i + 2]
                    val comps = tableWord.replace(EntityToRESTConstants.DB_ESCAPE_CHARACTER, "")
                        .split(EntityToRESTConstants.UNDERSCORE)
                    val entityName = comps.joinToString("") { w ->
                        w.substring(0, 1).uppercase() + w.substring(1).lowercase()
                    }
                    val entity = Entity(
                        entityName = entityName,
                        tableName = tableWord.replace(EntityToRESTConstants.DB_ESCAPE_CHARACTER, "")
                    )
                    entities.add(entity)
                }
            }

            if (pattern.matcher(word).matches()) {
                val columnName = words[i - 1].replace(EntityToRESTConstants.DB_ESCAPE_CHARACTER, "")
                val javaType = getJavaType(word)
                val comps = columnName.replace(EntityToRESTConstants.DB_ESCAPE_CHARACTER, "").split("_")
                var camelName = comps.joinToString("") { w ->
                    w.substring(0, 1).uppercase() + w.substring(1).lowercase()
                }
                camelName = camelName.substring(0, 1).lowercase() + camelName.substring(1)

                entities.last().columns.add(ColumnToField(columnName, camelName, word, javaType))
            }

            setPKorFKColumns(entities, words, i, word)
        }
    }

    private fun setPKorFKColumns(entities: MutableList<Entity>, words: MutableList<String>, i: Int, word: String) {
        val keyPattern = Pattern.compile(EntityToRESTConstants.PRIMARY_FOREIGN_REGEX)
        if (keyPattern.matcher(word.uppercase()).matches()) {
            val keyString = words.subList(i, i + 10).joinToString(" ")
            val keyColumn = getId(keyString)
            val entity = entities.last()
            val columns = entity.columns
            for (j in columns.indices) {
                val column = columns[j]
                if (column.databaseIdType == null && column.databaseColumnName == keyColumn.databaseColumnName) {
                    keyColumn.camelCaseFieldName = column.camelCaseFieldName?.replace(Regex("[iI]d$"), "")
                    entity.columns[j] = keyColumn
                }
            }
        }
    }

    fun getWords(words: MutableList<String>, inputStream: InputStream) {
        try {
            Scanner(inputStream).use { scanner ->
                while (scanner.hasNext()) {
                    val line = scanner.nextLine()
                    if (!line.trim().startsWith("#") && !line.trim().startsWith("--")) {
                        words.addAll(line.split(EntityToRESTConstants.SPACE_CHAR).filter { it.isNotEmpty() })
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getJavaType(datatype: String): String {
        val isKotlin = Globals.isKotlin
        return when {
            Pattern.compile(EntityToRESTConstants.VARCHAR_REGEX).matcher(datatype).matches() -> "String"
            Pattern.compile(EntityToRESTConstants.INT_REGEX).matcher(datatype).matches() -> if (isKotlin) "Int" else "Integer"
            Pattern.compile(EntityToRESTConstants.BIGINT_REGEX).matcher(datatype).matches() -> "Long"
            Pattern.compile(EntityToRESTConstants.DATETIME_REGEX).matcher(datatype).matches() -> "LocalDate"
            Pattern.compile(EntityToRESTConstants.BIT_REGEX).matcher(datatype).matches() -> if (isKotlin) "Boolean" else "String"
            Pattern.compile(EntityToRESTConstants.FLOAT_REGEX).matcher(datatype).matches() -> "Float"
            Pattern.compile(EntityToRESTConstants.DOUBLE_REGEX).matcher(datatype).matches() -> "Double"
            Pattern.compile(EntityToRESTConstants.TIME_REGEX).matcher(datatype).matches() -> "LocalTime"
            Pattern.compile(EntityToRESTConstants.TIMESTAMP_REGEX).matcher(datatype).matches() -> "LocalDateTime"
            else -> "String"
        }
    }

    fun firstLetterToCaps(string: String): String {
        if (string.isEmpty()) return ""
        return string.uppercase()[0] + string.substring(1)
    }

    fun camelName(string: String): String {
        val buffer = StringBuilder(string.lowercase())
        var i = 0
        while (i < buffer.length) {
            if (buffer[i] == '_') {
                buffer.replace(i, i + 2, buffer.substring(i + 1, i + 2).uppercase())
            }
            i++
        }
        return buffer.toString()
    }

    fun getId(key: String): ColumnToField {
        var camelName: String? = null
        var dbName: String? = null
        var dataType: String? = null
        var javaType: String? = null
        val idType: String

        val fkPattern = Pattern.compile(EntityToRESTConstants.FOREIGN_KEY_REFERENCES_REGEX)
        val fkMatcher = fkPattern.matcher(key)
        if (fkMatcher.find()) {
            val foreignKey = fkMatcher.group(1)
            val otherTableName = fkMatcher.group(2)
            val primaryKeyOtherTable = fkMatcher.group(3)
            javaType = firstLetterToCaps(camelName(otherTableName.replace(EntityToRESTConstants.DB_ESCAPE_CHARACTER, "")))
            camelName = camelName(foreignKey.replace(EntityToRESTConstants.DB_ESCAPE_CHARACTER, ""))
            dbName = primaryKeyOtherTable.replace(EntityToRESTConstants.DB_ESCAPE_CHARACTER, "")
        }

        val pkPattern = Pattern.compile(EntityToRESTConstants.PRIMARY_KEY_S_S)
        val pkMatcher = pkPattern.matcher(key)
        if (pkMatcher.matches()) {
            idType = EntityToRESTConstants.PK_ID
            dbName = pkMatcher.group(1).replace(EntityToRESTConstants.DB_ESCAPE_CHARACTER, "")
            javaType = EntityToRESTConstants.PK_DATA_TYPE
        } else {
            idType = EntityToRESTConstants.FK_ID
            javaType = if (Globals.isKotlin) "Int" else EntityToRESTConstants.FK_DATA_TYPE
        }

        return ColumnToField(
            databaseIdType = idType,
            databaseColumnName = dbName,
            camelCaseFieldName = camelName,
            databaseType = dataType,
            javaType = javaType
        )
    }
}
