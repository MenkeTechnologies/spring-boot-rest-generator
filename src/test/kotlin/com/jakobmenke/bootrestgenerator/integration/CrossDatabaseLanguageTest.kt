package com.jakobmenke.bootrestgenerator.integration

import com.jakobmenke.bootrestgenerator.Main
import com.jakobmenke.bootrestgenerator.dto.Entity
import com.jakobmenke.bootrestgenerator.utils.Globals
import com.jakobmenke.bootrestgenerator.utils.Util
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.test.assertContains
import kotlin.test.assertTrue

/**
 * Confirms that every supported database dump format can generate
 * both Java and Kotlin REST API output.
 */
class CrossDatabaseLanguageTest {

    @TempDir
    lateinit var tempDir: Path

    private var originalDbType = "mysql"
    private var originalLanguage = "java"

    @BeforeEach
    fun setUp() {
        originalDbType = Globals.DB_TYPE
        originalLanguage = Globals.LANGUAGE
    }

    @AfterEach
    fun tearDown() {
        Globals.DB_TYPE = originalDbType
        Globals.LANGUAGE = originalLanguage
    }

    companion object {
        @JvmStatic
        fun databaseLanguageMatrix(): Stream<Arguments> = Stream.of(
            Arguments.of("mysql", "dump.sql", "java"),
            Arguments.of("mysql", "dump.sql", "kotlin"),
            Arguments.of("mysql", "dump.sql", "groovy"),
            Arguments.of("postgresql", "pg_dump.sql", "java"),
            Arguments.of("postgresql", "pg_dump.sql", "kotlin"),
            Arguments.of("postgresql", "pg_dump.sql", "groovy"),
            Arguments.of("sqlite", "sqlite_dump.sql", "java"),
            Arguments.of("sqlite", "sqlite_dump.sql", "kotlin"),
            Arguments.of("sqlite", "sqlite_dump.sql", "groovy"),
            Arguments.of("mssql", "mssql_dump.sql", "java"),
            Arguments.of("mssql", "mssql_dump.sql", "kotlin"),
            Arguments.of("mssql", "mssql_dump.sql", "groovy")
        )
    }

    private fun parseEntities(dbType: String, dumpFile: String, language: String): List<Entity> {
        Globals.DB_TYPE = dbType
        Globals.LANGUAGE = language
        val words = mutableListOf<String>()
        val inputStream = javaClass.classLoader.getResourceAsStream(dumpFile)!!
        Util.getWords(words, inputStream)
        when (dbType) {
            "postgresql" -> Util.normalizePostgresqlWords(words)
            "sqlite" -> Util.normalizeSqliteWords(words)
            "mssql" -> Util.normalizeMssqlWords(words)
        }
        val entityList = mutableListOf<Entity>()
        Util.parseWords(entityList, words)
        return entityList
    }

    // ── Parsing produces entities for every DB type ─────────────────────

    @ParameterizedTest(name = "{0} + {2}: parses entities from {1}")
    @MethodSource("databaseLanguageMatrix")
    fun parsesAtLeastOneEntity(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        assertTrue(entities.isNotEmpty(), "$dbType/$language should parse at least one entity")
    }

    @ParameterizedTest(name = "{0} + {2}: all entities have columns")
    @MethodSource("databaseLanguageMatrix")
    fun allEntitiesHaveColumns(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        for (entity in entities) {
            assertTrue(entity.columns.isNotEmpty(), "${entity.entityName} in $dbType/$language should have columns")
        }
    }

    @ParameterizedTest(name = "{0} + {2}: all entities have primary key")
    @MethodSource("databaseLanguageMatrix")
    fun allEntitiesHavePrimaryKey(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        for (entity in entities) {
            val pk = entity.columns.find { it.databaseIdType == "@Id" }
            assertTrue(pk != null, "${entity.entityName} in $dbType/$language should have @Id")
            assertTrue(pk.javaType == "Long", "${entity.entityName} PK should be Long")
        }
    }

    @ParameterizedTest(name = "{0} + {2}: entity names are PascalCase")
    @MethodSource("databaseLanguageMatrix")
    fun entityNamesArePascalCase(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        for (entity in entities) {
            assertTrue(
                entity.entityName[0].isUpperCase(),
                "${entity.entityName} in $dbType/$language should start with uppercase"
            )
            assertTrue(
                !entity.entityName.contains("_"),
                "${entity.entityName} in $dbType/$language should not contain underscores"
            )
        }
    }

    @ParameterizedTest(name = "{0} + {2}: field names are camelCase")
    @MethodSource("databaseLanguageMatrix")
    fun fieldNamesAreCamelCase(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        for (entity in entities) {
            for (col in entity.columns) {
                val fieldName = col.camelCaseFieldName ?: continue
                if (fieldName.isEmpty()) continue // PK columns like "id" get stripped to ""
                assertTrue(
                    fieldName[0].isLowerCase() || fieldName[0].isDigit(),
                    "$dbType/$language: ${entity.entityName}.${fieldName} should start lowercase"
                )
            }
        }
    }

    // ── File generation works for every combo ───────────────────────────

    @ParameterizedTest(name = "{0} + {2}: generates entity files")
    @MethodSource("databaseLanguageMatrix")
    fun generatesEntityFiles(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        Globals.PACKAGE = "com/test/cross"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val ext = when (language) { "kotlin" -> ".kt"; "groovy" -> ".groovy"; else -> ".java" }
        val entityDir = File(tempDir.toFile(), "com/test/cross/entity")
        assertTrue(entityDir.exists(), "$dbType/$language: entity dir should exist")
        val entityFiles = entityDir.listFiles()?.map { it.name } ?: emptyList()
        assertTrue(entityFiles.isNotEmpty(), "$dbType/$language: should have entity files")
        assertTrue(
            entityFiles.all { it.endsWith(ext) },
            "$dbType/$language: all entity files should end with $ext"
        )
    }

    @ParameterizedTest(name = "{0} + {2}: generates REST controller files")
    @MethodSource("databaseLanguageMatrix")
    fun generatesRestFiles(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        Globals.PACKAGE = "com/test/cross"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val ext = when (language) { "kotlin" -> ".kt"; "groovy" -> ".groovy"; else -> ".java" }
        val restDir = File(tempDir.toFile(), "com/test/cross/rest")
        assertTrue(restDir.exists(), "$dbType/$language: rest dir should exist")
        val restFiles = restDir.listFiles()?.map { it.name } ?: emptyList()
        assertTrue(restFiles.isNotEmpty(), "$dbType/$language: should have REST files")
        assertTrue(
            restFiles.all { it.endsWith(ext) },
            "$dbType/$language: all REST files should end with $ext"
        )
    }

    @ParameterizedTest(name = "{0} + {2}: generates DAO files")
    @MethodSource("databaseLanguageMatrix")
    fun generatesDaoFiles(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        Globals.PACKAGE = "com/test/cross"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val ext = when (language) { "kotlin" -> ".kt"; "groovy" -> ".groovy"; else -> ".java" }
        val daoDir = File(tempDir.toFile(), "com/test/cross/dao")
        assertTrue(daoDir.exists(), "$dbType/$language: dao dir should exist")
        val daoFiles = daoDir.listFiles()?.map { it.name } ?: emptyList()
        assertTrue(daoFiles.isNotEmpty(), "$dbType/$language: should have DAO files")
        assertTrue(
            daoFiles.any { it.startsWith("GenericDao") },
            "$dbType/$language: should have GenericDao file"
        )
    }

    @ParameterizedTest(name = "{0} + {2}: generates repository files")
    @MethodSource("databaseLanguageMatrix")
    fun generatesRepositoryFiles(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        Globals.PACKAGE = "com/test/cross"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val ext = when (language) { "kotlin" -> ".kt"; "groovy" -> ".groovy"; else -> ".java" }
        val repoDir = File(tempDir.toFile(), "com/test/cross/repository")
        assertTrue(repoDir.exists(), "$dbType/$language: repository dir should exist")
        val repoFiles = repoDir.listFiles()?.map { it.name } ?: emptyList()
        assertTrue(repoFiles.isNotEmpty(), "$dbType/$language: should have repository files")
    }

    @ParameterizedTest(name = "{0} + {2}: generates utils files")
    @MethodSource("databaseLanguageMatrix")
    fun generatesUtilsFiles(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        Globals.PACKAGE = "com/test/cross"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val ext = when (language) { "kotlin" -> ".kt"; "groovy" -> ".groovy"; else -> ".java" }
        val utilsDir = File(tempDir.toFile(), "com/test/cross/utils")
        assertTrue(utilsDir.exists(), "$dbType/$language: utils dir should exist")
        val utilsFiles = utilsDir.listFiles()?.map { it.name } ?: emptyList()
        assertTrue(
            utilsFiles.any { it.startsWith("GlobalConstants") },
            "$dbType/$language: should have GlobalConstants file"
        )
    }

    // ── Generated content is correct for each language ──────────────────

    @ParameterizedTest(name = "{0} + {2}: entity file has correct annotations")
    @MethodSource("databaseLanguageMatrix")
    fun entityFileHasCorrectAnnotations(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        Globals.PACKAGE = "com/test/cross"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val ext = when (language) { "kotlin" -> ".kt"; "groovy" -> ".groovy"; else -> ".java" }
        val entityDir = File(tempDir.toFile(), "com/test/cross/entity")
        val firstEntityFile = entityDir.listFiles()?.first() ?: error("No entity files")
        val content = firstEntityFile.readText()

        assertContains(content, "package com.test.cross.entity")
        assertContains(content, "@Entity")
        assertContains(content, "@Table")
        assertContains(content, "@Id")
    }

    @ParameterizedTest(name = "{0} + {2}: REST file has correct annotations")
    @MethodSource("databaseLanguageMatrix")
    fun restFileHasCorrectAnnotations(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        Globals.PACKAGE = "com/test/cross"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val restDir = File(tempDir.toFile(), "com/test/cross/rest")
        val firstRestFile = restDir.listFiles()?.first() ?: error("No REST files")
        val content = firstRestFile.readText()

        assertContains(content, "package com.test.cross.rest")
        assertContains(content, "@RestController")
    }

    @ParameterizedTest(name = "{0} + {2}: DAO file has @Service")
    @MethodSource("databaseLanguageMatrix")
    fun daoFileHasServiceAnnotation(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        Globals.PACKAGE = "com/test/cross"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val daoDir = File(tempDir.toFile(), "com/test/cross/dao")
        val daoFile = daoDir.listFiles()?.first { !it.name.startsWith("GenericDao") }
            ?: error("No DAO files")
        val content = daoFile.readText()

        assertContains(content, "@Service")
    }

    @ParameterizedTest(name = "{0} + {2}: repository file has @Repository")
    @MethodSource("databaseLanguageMatrix")
    fun repositoryFileHasRepositoryAnnotation(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        Globals.PACKAGE = "com/test/cross"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val repoDir = File(tempDir.toFile(), "com/test/cross/repository")
        val repoFile = repoDir.listFiles()?.first() ?: error("No repo files")
        val content = repoFile.readText()

        assertContains(content, "@Repository")
    }

    // ── Entity count matches per entity file ────────────────────────────

    @ParameterizedTest(name = "{0} + {2}: file count matches entity count")
    @MethodSource("databaseLanguageMatrix")
    fun fileCountMatchesEntityCount(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        Globals.PACKAGE = "com/test/cross"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val entityCount = entities.size
        val entityFiles = File(tempDir.toFile(), "com/test/cross/entity").listFiles()?.size ?: 0
        val restFiles = File(tempDir.toFile(), "com/test/cross/rest").listFiles()?.size ?: 0
        val repoFiles = File(tempDir.toFile(), "com/test/cross/repository").listFiles()?.size ?: 0

        assertTrue(entityFiles == entityCount, "$dbType/$language: entity files ($entityFiles) should match entity count ($entityCount)")
        assertTrue(restFiles == entityCount, "$dbType/$language: REST files ($restFiles) should match entity count ($entityCount)")
        assertTrue(repoFiles == entityCount, "$dbType/$language: repo files ($repoFiles) should match entity count ($entityCount)")
    }

    // ── Foreign key entities generate @ManyToOne ────────────────────────

    @ParameterizedTest(name = "{0} + {2}: at least one entity has foreign key")
    @MethodSource("databaseLanguageMatrix")
    fun atLeastOneEntityHasForeignKey(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        val hasFk = entities.any { entity ->
            entity.columns.any { it.databaseIdType == "@ManyToOne" }
        }
        assertTrue(hasFk, "$dbType/$language: at least one entity should have a @ManyToOne FK")
    }

    @ParameterizedTest(name = "{0} + {2}: FK entity generates @JoinColumn in file")
    @MethodSource("databaseLanguageMatrix")
    fun fkEntityGeneratesJoinColumn(dbType: String, dumpFile: String, language: String) {
        val entities = parseEntities(dbType, dumpFile, language)
        Globals.PACKAGE = "com/test/cross"
        Globals.SRC_FOLDER = tempDir.toString()

        val main = Main()
        main.writeTemplates(entities)

        val ext = when (language) { "kotlin" -> ".kt"; "groovy" -> ".groovy"; else -> ".java" }
        val entityDir = File(tempDir.toFile(), "com/test/cross/entity")
        val fkEntity = entities.first { entity ->
            entity.columns.any { it.databaseIdType == "@ManyToOne" }
        }
        val fkFile = File(entityDir, "${fkEntity.entityName}$ext")
        assertTrue(fkFile.exists(), "$dbType/$language: FK entity file should exist")
        val content = fkFile.readText()
        assertContains(content, "@ManyToOne")
        assertContains(content, "@JoinColumn")
    }
}
