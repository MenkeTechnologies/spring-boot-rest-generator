package com.jakobmenke.bootrestgenerator

import com.jakobmenke.bootrestgenerator.dto.Entity
import com.jakobmenke.bootrestgenerator.templates.Templates
import com.jakobmenke.bootrestgenerator.utils.Configuration
import com.jakobmenke.bootrestgenerator.utils.Globals
import com.jakobmenke.bootrestgenerator.utils.Util
import java.io.File
import java.io.PrintWriter

fun main() {
    val configuration = Configuration(Configuration.readConfig("config.properties")!!)
    Globals.PACKAGE = configuration.targetPackage
    Globals.SRC_FOLDER = configuration.srcFolder
    Globals.FILE_NAME = configuration.fileName
    Globals.LANGUAGE = configuration.language

    val entities = mutableListOf<Entity>()
    val words = mutableListOf<String>()

    Util.getWords(words, Main::class.java.classLoader.getResourceAsStream(Globals.FILE_NAME)!!)
    Util.parseWords(entities, words)

    val main = Main()
    main.writeTemplates(entities)
}

class Main {
    fun writeTemplates(entities: List<Entity>) {
        val templates = Templates()
        val ext = Globals.fileExtension
        for (entity in entities) {
            val entityTemplate = templates.getEntityTemplate(entity, Globals.PACKAGE)
            createFile("entity", "${entity.entityName}$ext", entityTemplate)

            val serviceTemplate = templates.getResourceTemplate(Globals.PACKAGE, entity.entityName)
            createFile("rest", "${entity.entityName}Resource$ext", serviceTemplate)

            val daoTemplate = templates.getDaoTemplate(Globals.PACKAGE, entity.entityName)
            createFile("dao", "${entity.entityName}Dao$ext", daoTemplate)

            val repositoryTemplate = templates.getRepositoryTemplate(Globals.PACKAGE, entity.entityName)
            createFile("repository", "${entity.entityName}Repository$ext", repositoryTemplate)
        }

        val constantsTemplate = templates.getFileTemplateByName(Globals.PACKAGE, "constants")
        createFile("utils", "GlobalConstants$ext", constantsTemplate)

        val daoTemplate = templates.getFileTemplateByName(Globals.PACKAGE, "genericdao")
        createFile("dao", "GenericDao$ext", daoTemplate)
    }

    private fun createFile(folderName: String, fileName: String, fileTemplate: String) {
        val path = "${Globals.SRC_FOLDER}${File.separator}${Globals.PACKAGE}${File.separator}$folderName"
        val dir = File(path)

        if (!dir.exists()) {
            val mkdirStatus = dir.mkdirs()
            if (!mkdirStatus) {
                System.err.println("Failed to create directory: $path")
            }
        }

        PrintWriter("$path/$fileName", "UTF-8").use { writer ->
            writer.print(fileTemplate)
        }
    }
}
