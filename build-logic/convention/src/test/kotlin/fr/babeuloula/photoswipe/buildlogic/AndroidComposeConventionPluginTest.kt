package fr.babeuloula.photoswipe.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Vérifie que la convention Compose active le compilateur Compose et aligne les bibliothèques sur la BOM. */
class AndroidComposeConventionPluginTest {
    @Test
    fun `compose convention applies the kotlin compose compiler and enables compose`() {
        val project = androidProjectWithCompose()

        assertTrue(project.pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.compose"))
        assertEquals(true, project.extensions.getByType(ApplicationExtension::class.java).buildFeatures.compose)
    }

    @Test
    fun `compose convention aligns compose libraries on the bom`() {
        val project = androidProjectWithCompose()

        assertEquals(
            listOf("libs:androidx-compose-bom", "libs:androidx-compose-ui", "libs:androidx-compose-ui-tooling-preview"),
            project.coordinatesOf("implementation"),
        )
        assertEquals(listOf("libs:androidx-compose-bom"), project.coordinatesOf("testImplementation"))
    }

    @Test
    fun `compose convention keeps preview tooling out of release builds`() {
        val project = androidProjectWithCompose()

        assertEquals(listOf("libs:androidx-compose-ui-tooling"), project.coordinatesOf("debugImplementation"))
    }

    private fun androidProjectWithCompose(): Project {
        val project = projectWithFakeCatalog()
        project.pluginManager.apply(AndroidApplicationConventionPlugin::class.java)
        project.pluginManager.apply(AndroidComposeConventionPlugin::class.java)
        return project
    }
}
