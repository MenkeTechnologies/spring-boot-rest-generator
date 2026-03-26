package com.jakobmenke.bootrestgenerator.templates

import com.jakobmenke.bootrestgenerator.dto.Entity
import com.jakobmenke.bootrestgenerator.utils.EntityToRESTConstants
import java.util.Scanner

class Templates {
    fun getResourceTemplate(mainPackage: String, entityName: String): String {
        val mainPackageName = mainPackage.replace("/", ".")
        return getFile("templates/rest.resource.tmpl")
            .replace("{{mainPackageName}}", mainPackageName)
            .replace("{{entityName}}", entityName)
            .replace("{{serviceName}}", entityName.lowercase())
            .replace("{{restServicePrefix}}", "GlobalConstants.CONTEXT_PATH")
    }

    fun getDaoTemplate(mainPackage: String, entityName: String): String {
        val mainPackageName = mainPackage.replace("/", ".")
        val camelRepoName = entityName.substring(0, 1).lowercase() + entityName.substring(1) + "Repository"
        return getFile("templates/dao.tmpl")
            .replace("{{mainPackageName}}", mainPackageName)
            .replace("{{entityName}}", entityName)
            .replace("{{camelRepositoryName}}", camelRepoName)
    }

    fun getFileTemplateByName(mainPackage: String, fileName: String): String {
        val mainPackageName = mainPackage.replace("/", ".")
        return getFile("templates/$fileName.tmpl")
            .replace("{{mainPackageName}}", mainPackageName)
    }

    fun getRepositoryTemplate(mainPackage: String, entityName: String): String {
        val mainPackageName = mainPackage.replace("/", ".")
        return getFile("templates/repository.tmpl")
            .replace("{{mainPackageName}}", mainPackageName)
            .replace("{{entityName}}", entityName)
            .replace("{{primaryKeyType}}", EntityToRESTConstants.PK_DATA_TYPE)
    }

    private fun getFile(fileName: String): String {
        val inputStream = Templates::class.java.classLoader.getResourceAsStream(fileName)!!
        return Scanner(inputStream).use { scanner ->
            buildString {
                while (scanner.hasNextLine()) {
                    appendLine(scanner.nextLine())
                }
            }
        }
    }

    fun getEntityTemplate(entity: Entity, mainPackage: String): String {
        val mainPackageName = mainPackage.replace("/", ".")
        var fileTemplate = getFile("templates/entity.tmpl")
            .replace("{{mainPackageName}}", mainPackageName)
            .replace("{{entityName}}", entity.entityName)
            .replace("{{tableName}}", entity.tableName)

        val sb = StringBuilder()
        for (column in entity.columns) {
            val indentation = "    "
            if (column.databaseIdType != null) {
                sb.append(indentation).append(column.databaseIdType).append("\n")
            }
            if (column.databaseIdType != null && column.databaseIdType.equals("@ManyToOne", ignoreCase = true)) {
                sb.append(indentation).append("@JoinColumn(name = \"").append(column.databaseColumnName).append("\")\n")
            } else {
                sb.append(indentation).append("@Column(name = \"").append(column.databaseColumnName).append("\")\n")
            }
            sb.append(indentation).append("private ").append(column.javaType).append(" ").append(column.camelCaseFieldName).append(";\n\n")
        }
        sb.append("}")

        return fileTemplate + sb.toString()
    }
}
