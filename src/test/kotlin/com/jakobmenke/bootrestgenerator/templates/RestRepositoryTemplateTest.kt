package com.jakobmenke.bootrestgenerator.templates

import com.jakobmenke.bootrestgenerator.utils.Globals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class RestRepositoryTemplateTest {

    private val templates = Templates()
    private val testPackage = "com/example/app"
    private val testPackageDot = "com.example.app"

    @AfterEach
    fun tearDown() {
        Globals.LANGUAGE = "java"
    }

    // ── restrepository.tmpl (Java) ──────────────────────────────────────

    @Nested
    inner class JavaRestRepositoryTemplate {
        @Test
        fun containsPackageDeclaration() {
            val result = templates.getFileTemplateByName(testPackage, "restrepository")
            assertContains(result, "package $testPackageDot.repositories")
        }

        @Test
        fun containsRepositoryRestResourceAnnotation() {
            val result = templates.getFileTemplateByName(testPackage, "restrepository")
            assertContains(result, "@RepositoryRestResource")
        }

        @Test
        fun containsDataRestImport() {
            val result = templates.getFileTemplateByName(testPackage, "restrepository")
            assertContains(result, "import org.springframework.data.rest.core.annotation.RepositoryRestResource")
        }

        @Test
        fun noPlaceholdersForPackage() {
            val result = templates.getFileTemplateByName(testPackage, "restrepository")
            assertFalse(result.contains("{{mainPackageName}}"))
        }

        // Note: restrepository.tmpl still has {{entityName}} placeholders because
        // getFileTemplateByName only replaces {{mainPackageName}}. This is expected
        // behavior - it's a template that would need a different method to fully resolve.
        @Test
        fun stillHasEntityNamePlaceholders() {
            val result = templates.getFileTemplateByName(testPackage, "restrepository")
            assertContains(result, "{{entityName}}")
        }
    }
}
