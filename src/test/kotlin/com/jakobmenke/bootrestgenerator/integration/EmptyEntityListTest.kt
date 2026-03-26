package com.jakobmenke.bootrestgenerator.integration

import com.jakobmenke.bootrestgenerator.Main
import com.jakobmenke.bootrestgenerator.dto.Entity
import com.jakobmenke.bootrestgenerator.utils.Globals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmptyEntityListTest {

    @TempDir
    lateinit var tempDir: Path

    @AfterEach
    fun tearDown() {
        Globals.LANGUAGE = "java"
    }

    // ── Empty entities still writes shared files ────────────────────────

    @Nested
    inner class JavaEmptyEntities {
        @Test
        fun writesGenericDaoWithNoEntities() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()
            Globals.LANGUAGE = "java"

            val main = Main()
            main.writeTemplates(emptyList())

            val daoDir = File(tempDir.toFile(), "com/test/app/dao")
            assertTrue(daoDir.exists())
            val files = daoDir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(files.contains("GenericDao.java"))
        }

        @Test
        fun writesGlobalConstantsWithNoEntities() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()
            Globals.LANGUAGE = "java"

            val main = Main()
            main.writeTemplates(emptyList())

            val utilsDir = File(tempDir.toFile(), "com/test/app/utils")
            assertTrue(utilsDir.exists())
            val files = utilsDir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(files.contains("GlobalConstants.java"))
        }

        @Test
        fun noEntityDirCreatedWithNoEntities() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()
            Globals.LANGUAGE = "java"

            val main = Main()
            main.writeTemplates(emptyList())

            val entityDir = File(tempDir.toFile(), "com/test/app/entity")
            assertFalse(entityDir.exists())
        }

        @Test
        fun noRestDirCreatedWithNoEntities() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()
            Globals.LANGUAGE = "java"

            val main = Main()
            main.writeTemplates(emptyList())

            val restDir = File(tempDir.toFile(), "com/test/app/rest")
            assertFalse(restDir.exists())
        }

        @Test
        fun noRepositoryDirCreatedWithNoEntities() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()
            Globals.LANGUAGE = "java"

            val main = Main()
            main.writeTemplates(emptyList())

            val repoDir = File(tempDir.toFile(), "com/test/app/repository")
            assertFalse(repoDir.exists())
        }
    }

    @Nested
    inner class KotlinEmptyEntities {
        @Test
        fun writesKotlinGenericDaoWithNoEntities() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()
            Globals.LANGUAGE = "kotlin"

            val main = Main()
            main.writeTemplates(emptyList())

            val daoDir = File(tempDir.toFile(), "com/test/app/dao")
            assertTrue(daoDir.exists())
            val files = daoDir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(files.contains("GenericDao.kt"))
            assertFalse(files.contains("GenericDao.java"))
        }

        @Test
        fun writesKotlinGlobalConstantsWithNoEntities() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()
            Globals.LANGUAGE = "kotlin"

            val main = Main()
            main.writeTemplates(emptyList())

            val utilsDir = File(tempDir.toFile(), "com/test/app/utils")
            val files = utilsDir.listFiles()?.map { it.name } ?: emptyList()
            assertTrue(files.contains("GlobalConstants.kt"))
            assertFalse(files.contains("GlobalConstants.java"))
        }

        @Test
        fun kotlinConstantsFileUsesObject() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()
            Globals.LANGUAGE = "kotlin"

            val main = Main()
            main.writeTemplates(emptyList())

            val constantsFile = File(tempDir.toFile(), "com/test/app/utils/GlobalConstants.kt")
            val content = constantsFile.readText()
            assertContains(content, "object GlobalConstants")
            assertContains(content, "const val CONTEXT_PATH")
        }

        @Test
        fun kotlinGenericDaoFileUsesInterface() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()
            Globals.LANGUAGE = "kotlin"

            val main = Main()
            main.writeTemplates(emptyList())

            val daoFile = File(tempDir.toFile(), "com/test/app/dao/GenericDao.kt")
            val content = daoFile.readText()
            assertContains(content, "interface GenericDao<T>")
            assertContains(content, "fun findAll(): List<T>")
            assertContains(content, "fun findOne(id: Long): T?")
        }
    }

    // ── Single entity generation ────────────────────────────────────────

    @Nested
    inner class SingleEntityGeneration {
        @Test
        fun javaGeneratesFourFilesForSingleEntity() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()
            Globals.LANGUAGE = "java"

            val entities = listOf(Entity(tableName = "USER", entityName = "User"))
            val main = Main()
            main.writeTemplates(entities)

            assertEquals(1, File(tempDir.toFile(), "com/test/app/entity").listFiles()?.size)
            assertEquals(1, File(tempDir.toFile(), "com/test/app/rest").listFiles()?.size)
            assertEquals(2, File(tempDir.toFile(), "com/test/app/dao").listFiles()?.size) // Dao + GenericDao
            assertEquals(1, File(tempDir.toFile(), "com/test/app/repository").listFiles()?.size)
            assertEquals(1, File(tempDir.toFile(), "com/test/app/utils").listFiles()?.size)
        }

        @Test
        fun kotlinGeneratesFourFilesForSingleEntity() {
            Globals.PACKAGE = "com/test/app"
            Globals.SRC_FOLDER = tempDir.toString()
            Globals.LANGUAGE = "kotlin"

            val entities = listOf(Entity(tableName = "USER", entityName = "User"))
            val main = Main()
            main.writeTemplates(entities)

            val entityFiles = File(tempDir.toFile(), "com/test/app/entity").listFiles()!!
            assertEquals(1, entityFiles.size)
            assertEquals("User.kt", entityFiles[0].name)

            val restFiles = File(tempDir.toFile(), "com/test/app/rest").listFiles()!!
            assertEquals(1, restFiles.size)
            assertEquals("UserResource.kt", restFiles[0].name)
        }
    }

    // ── Deep package paths ──────────────────────────────────────────────

    @Nested
    inner class DeepPackagePaths {
        @Test
        fun deeplyNestedPackageCreatesCorrectDirs() {
            Globals.PACKAGE = "com/a/b/c/d/e/f"
            Globals.SRC_FOLDER = tempDir.toString()
            Globals.LANGUAGE = "kotlin"

            val entities = listOf(Entity(tableName = "T", entityName = "T"))
            val main = Main()
            main.writeTemplates(entities)

            val entityDir = File(tempDir.toFile(), "com/a/b/c/d/e/f/entity")
            assertTrue(entityDir.exists())
            assertTrue(entityDir.listFiles()!!.any { it.name == "T.kt" })
        }

        @Test
        fun singleSegmentPackage() {
            Globals.PACKAGE = "myapp"
            Globals.SRC_FOLDER = tempDir.toString()
            Globals.LANGUAGE = "kotlin"

            val entities = listOf(Entity(tableName = "T", entityName = "T"))
            val main = Main()
            main.writeTemplates(entities)

            val entityDir = File(tempDir.toFile(), "myapp/entity")
            assertTrue(entityDir.exists())
        }
    }
}
