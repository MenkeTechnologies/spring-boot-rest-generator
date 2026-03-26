package com.jakobmenke.bootrestgenerator.utils

import com.jakobmenke.bootrestgenerator.dto.ColumnToField
import com.jakobmenke.bootrestgenerator.dto.Entity
import java.io.InputStream
import java.util.Scanner
object Util {
    fun parseWords(entities: MutableList<Entity>, words: MutableList<String>) {
        val pattern = EntityToRESTConstants.SUPPORTED_DATA_TYPES_PATTERN
        val escapeChar = Globals.escapeCharacter

        for (i in words.indices) {
            val word = words[i]

            if (word.equals("create", ignoreCase = true)) {
                if (i + 2 < words.size) {
                    val nextWord = words[i + 1]
                    if (nextWord.equals("table", ignoreCase = true)) {
                        val tableWord = words[i + 2]
                        val cleanName = stripSchemaAndEscape(tableWord, escapeChar)
                        val comps = cleanName.split(EntityToRESTConstants.UNDERSCORE)
                        val entityName = comps.joinToString("") { w ->
                            w.substring(0, 1).uppercase() + w.substring(1).lowercase()
                        }
                        val entity = Entity(
                            entityName = entityName,
                            tableName = cleanName
                        )
                        entities.add(entity)
                    }
                }
            }

            if (entities.isNotEmpty() && pattern.matcher(word).matches()) {
                val columnName = words[i - 1].replace(escapeChar, "")
                val javaType = getJavaType(word)
                val comps = columnName.replace(escapeChar, "").split("_")
                var camelName = comps.joinToString("") { w ->
                    w.substring(0, 1).uppercase() + w.substring(1).lowercase()
                }
                camelName = camelName.substring(0, 1).lowercase() + camelName.substring(1)

                entities.last().columns.add(ColumnToField(columnName, camelName, word, javaType))

                // Inline PRIMARY KEY: "column_name TYPE [NOT NULL] PRIMARY KEY [AUTOINCREMENT/IDENTITY]"
                if (Globals.isSqlite || Globals.isMssql) {
                    val maxLook = minOf(i + 5, words.size - 1)
                    for (k in i + 1..maxLook) {
                        val w = words[k]
                        // Stop scanning if we hit another column def or constraint boundary
                        if (w.equals("CONSTRAINT", ignoreCase = true)
                            || w.equals("FOREIGN", ignoreCase = true)
                            || w == ")" || w == "("
                        ) break
                        if (w.equals("PRIMARY", ignoreCase = true)
                            && k + 1 < words.size
                            && words[k + 1].equals("KEY", ignoreCase = true)
                        ) {
                            val entity = entities.last()
                            val lastIdx = entity.columns.size - 1
                            val pkColumn = ColumnToField(
                                databaseIdType = EntityToRESTConstants.PK_ID,
                                databaseColumnName = columnName,
                                camelCaseFieldName = camelName.replace(Regex("[iI]d$"), ""),
                                databaseType = null,
                                javaType = EntityToRESTConstants.PK_DATA_TYPE
                            )
                            entity.columns[lastIdx] = pkColumn
                            break
                        }
                    }
                }
            }

            if (entities.isNotEmpty()) {
                setPKorFKColumns(entities, words, i, word, escapeChar)
            }
        }
    }

    private fun setPKorFKColumns(
        entities: MutableList<Entity>,
        words: MutableList<String>,
        i: Int,
        word: String,
        escapeChar: String
    ) {
        val keyPattern = EntityToRESTConstants.PRIMARY_FOREIGN_PATTERN
        if (keyPattern.matcher(word.uppercase()).matches()) {
            val endIndex = minOf(i + 10, words.size)
            val keyString = words.subList(i, endIndex).joinToString(" ")
            val keyColumn = getId(keyString, escapeChar)

            val targetEntity = if (Globals.isPostgresql) {
                findAlterTableEntity(entities, words, i, escapeChar) ?: entities.last()
            } else {
                entities.last()
            }

            val columns = targetEntity.columns
            for (j in columns.indices) {
                val column = columns[j]
                if (column.databaseIdType == null && column.databaseColumnName == keyColumn.databaseColumnName) {
                    keyColumn.camelCaseFieldName = column.camelCaseFieldName?.replace(Regex("[iI]d$"), "")
                    targetEntity.columns[j] = keyColumn
                }
            }
        }
    }

    private fun findAlterTableEntity(
        entities: MutableList<Entity>,
        words: MutableList<String>,
        currentIndex: Int,
        escapeChar: String
    ): Entity? {
        for (j in currentIndex - 1 downTo maxOf(0, currentIndex - 10)) {
            if (words[j].equals("alter", ignoreCase = true) && j + 2 < words.size) {
                if (words[j + 1].equals("table", ignoreCase = true)) {
                    val tableIdx = if (j + 2 < words.size && words[j + 2].equals("only", ignoreCase = true)) j + 3 else j + 2
                    if (tableIdx < words.size) {
                        val tableName = stripSchemaAndEscape(words[tableIdx], escapeChar)
                        return entities.find { it.tableName.equals(tableName, ignoreCase = true) }
                    }
                }
            }
        }
        return null
    }

    private fun stripSchemaAndEscape(name: String, escapeChar: String): String {
        var clean = name.replace(escapeChar, "")
        // Also strip closing bracket for MSSQL
        if (Globals.isMssql) {
            clean = clean.replace(EntityToRESTConstants.MSSQL_DB_ESCAPE_CLOSE, "")
        }
        // Remove trailing punctuation (semicolons, commas, parens)
        clean = clean.trimEnd(';', ',', '(', ')')
        // Remove schema prefix (e.g., "public.table_name" → "table_name", "dbo.Users" → "Users")
        if (clean.contains(".")) {
            clean = clean.substringAfterLast(".")
        }
        return clean
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

    fun normalizePostgresqlWords(words: MutableList<String>) {
        // First pass: strip trailing commas and semicolons from all tokens
        for (i in words.indices) {
            words[i] = words[i].trimEnd(';', ',')
        }

        // Second pass: combine multi-word PostgreSQL types into single tokens
        var i = 0
        while (i < words.size) {
            val word = words[i].lowercase()
            when {
                // "character varying(n)" → "varchar(n)"
                word == "character" && i + 1 < words.size && words[i + 1].lowercase().startsWith("varying") -> {
                    val varying = words[i + 1]
                    val size = varying.substringAfter("varying", "").substringAfter("VARYING", "")
                    words[i] = "varchar$size"
                    words.removeAt(i + 1)
                }
                // "timestamp/time without time zone" or "timestamp/time with time zone"
                (word == "timestamp" || word == "time") && i + 3 < words.size
                        && (words[i + 1].equals("without", ignoreCase = true) || words[i + 1].equals("with", ignoreCase = true))
                        && words[i + 2].equals("time", ignoreCase = true)
                        && words[i + 3].lowercase().startsWith("zone") -> {
                    words.removeAt(i + 3)
                    words.removeAt(i + 2)
                    words.removeAt(i + 1)
                }
                // "double precision" → "double"
                word == "double" && i + 1 < words.size && words[i + 1].lowercase().startsWith("precision") -> {
                    words.removeAt(i + 1)
                }
            }
            i++
        }
    }

    fun normalizeSqliteWords(words: MutableList<String>) {
        // First pass: strip trailing commas and semicolons
        for (i in words.indices) {
            words[i] = words[i].trimEnd(';', ',')
        }

        // Second pass: remove IF NOT EXISTS, PRAGMA, INSERT, BEGIN, COMMIT noise
        var i = 0
        while (i < words.size) {
            val upper = words[i].uppercase()
            when {
                // Remove "IF NOT EXISTS" (common in SQLite CREATE TABLE IF NOT EXISTS)
                upper == "IF" && i + 2 < words.size
                        && words[i + 1].equals("NOT", ignoreCase = true)
                        && words[i + 2].equals("EXISTS", ignoreCase = true) -> {
                    words.removeAt(i + 2)
                    words.removeAt(i + 1)
                    words.removeAt(i)
                }
                // Remove PRAGMA, INSERT, BEGIN, COMMIT, DELETE statements
                upper == "PRAGMA" || upper == "INSERT" || upper == "BEGIN"
                        || upper == "COMMIT" || upper == "DELETE" -> {
                    // Remove tokens until we hit a CREATE, ALTER, or end of list
                    while (i < words.size) {
                        val next = words[i].uppercase().trimEnd(';', ',', '(', ')')
                        if (next == "CREATE" || next == "ALTER") break
                        words.removeAt(i)
                    }
                }
                else -> i++
            }
        }
    }

    fun normalizeMssqlWords(words: MutableList<String>) {
        // First pass: strip square brackets and trailing punctuation from all tokens
        for (i in words.indices) {
            words[i] = words[i]
                .replace("[", "")
                .replace("]", "")
                .trimEnd(';', ',')
            // Strip (max)/(MAX) from type tokens like nvarchar(max)
            words[i] = words[i].replace(Regex("\\((?i:max)\\)"), "")
        }

        // Second pass: remove MSSQL noise tokens
        var i = 0
        while (i < words.size) {
            val upper = words[i].uppercase()
            when {
                // Remove SET, USE, GO, EXEC, PRINT, IF, DROP, INSERT statements
                upper == "SET" || upper == "USE" || upper == "GO" || upper == "EXEC"
                        || upper == "PRINT" || upper == "IF" || upper == "DROP"
                        || upper == "INSERT" || upper == "BEGIN" || upper == "END" -> {
                    while (i < words.size) {
                        val next = words[i].uppercase().trimEnd(';', ',', '(', ')')
                        if (next == "CREATE" || next == "ALTER") break
                        words.removeAt(i)
                    }
                }
                // Remove CLUSTERED, NONCLUSTERED, ASC, DESC keywords (may have trailing parens)
                upper.trimEnd('(', ')') == "CLUSTERED" || upper.trimEnd('(', ')') == "NONCLUSTERED"
                        || upper.trimEnd('(', ')') == "ASC" || upper.trimEnd('(', ')') == "DESC" -> {
                    val trailing = words[i].filter { it == '(' || it == ')' }
                    if (trailing.isNotEmpty()) {
                        words[i] = trailing
                        i++
                    } else {
                        words.removeAt(i)
                    }
                }
                // Remove IDENTITY(n,n) tokens
                upper.startsWith("IDENTITY") -> {
                    words.removeAt(i)
                }
                else -> i++
            }
        }
    }

    private fun getJavaType(datatype: String): String {
        val isKotlin = Globals.isKotlin
        val dt = datatype.lowercase()
        return when {
            EntityToRESTConstants.VARCHAR_PATTERN.matcher(dt).matches() -> "String"
            EntityToRESTConstants.MSSQL_NVARCHAR_PATTERN.matcher(dt).matches() -> "String"
            EntityToRESTConstants.PG_TEXT_PATTERN.matcher(dt).matches() -> "String"
            EntityToRESTConstants.MSSQL_UNIQUEIDENTIFIER_PATTERN.matcher(dt).matches() -> "String"
            EntityToRESTConstants.MSSQL_IMAGE_PATTERN.matcher(dt).matches() -> "String"
            EntityToRESTConstants.PG_BIGINT_PATTERN.matcher(dt).matches() -> "Long"
            EntityToRESTConstants.BIGINT_PATTERN.matcher(dt).matches() -> "Long"
            EntityToRESTConstants.INT_PATTERN.matcher(dt).matches() -> if (isKotlin) "Int" else "Integer"
            EntityToRESTConstants.PG_INTEGER_PATTERN.matcher(dt).matches() -> if (isKotlin) "Int" else "Integer"
            EntityToRESTConstants.MSSQL_DATETIME2_PATTERN.matcher(dt).matches() -> "LocalDateTime"
            EntityToRESTConstants.DATETIME_PATTERN.matcher(dt).matches() -> "LocalDate"
            EntityToRESTConstants.PG_DATE_PATTERN.matcher(dt).matches() -> "LocalDate"
            EntityToRESTConstants.PG_BOOLEAN_PATTERN.matcher(dt).matches() -> if (isKotlin) "Boolean" else "String"
            EntityToRESTConstants.BIT_PATTERN.matcher(dt).matches() -> if (isKotlin) "Boolean" else "String"
            EntityToRESTConstants.FLOAT_PATTERN.matcher(dt).matches() -> "Float"
            EntityToRESTConstants.PG_REAL_PATTERN.matcher(dt).matches() -> "Float"
            EntityToRESTConstants.DOUBLE_PATTERN.matcher(dt).matches() -> "Double"
            EntityToRESTConstants.PG_NUMERIC_PATTERN.matcher(dt).matches() -> "Double"
            EntityToRESTConstants.MSSQL_DECIMAL_PATTERN.matcher(dt).matches() -> "Double"
            EntityToRESTConstants.MSSQL_MONEY_PATTERN.matcher(dt).matches() -> "Double"
            EntityToRESTConstants.TIME_PATTERN.matcher(dt).matches() -> "LocalTime"
            EntityToRESTConstants.TIMESTAMP_PATTERN.matcher(dt).matches() -> "LocalDateTime"
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
                if (i + 1 < buffer.length) {
                    buffer.replace(i, i + 2, buffer.substring(i + 1, i + 2).uppercase())
                } else {
                    buffer.deleteCharAt(i)
                }
            }
            i++
        }
        return buffer.toString()
    }

    fun getId(key: String, escapeChar: String = EntityToRESTConstants.DB_ESCAPE_CHARACTER): ColumnToField {
        var camelName: String? = null
        var dbName: String? = null
        var dataType: String? = null
        var javaType: String? = null
        val idType: String

        val fkMatcher = EntityToRESTConstants.FOREIGN_KEY_REFERENCES_PATTERN.matcher(key)
        if (fkMatcher.find()) {
            val foreignKey = fkMatcher.group(1)
            val otherTableName = fkMatcher.group(2)
            val primaryKeyOtherTable = fkMatcher.group(3)
            val cleanTableName = stripSchemaAndEscape(otherTableName, escapeChar)
            javaType = firstLetterToCaps(camelName(cleanTableName))
            camelName = camelName(foreignKey.replace(escapeChar, ""))
            dbName = primaryKeyOtherTable.replace(escapeChar, "")
        }

        val pkMatcher = EntityToRESTConstants.PRIMARY_KEY_PATTERN.matcher(key)
        if (pkMatcher.matches()) {
            idType = EntityToRESTConstants.PK_ID
            dbName = pkMatcher.group(1).replace(escapeChar, "").trim()
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
