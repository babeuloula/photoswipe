package fr.babeuloula.photoswipe.buildlogic

import org.gradle.api.Project
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Vérifie que la convention Room applique KSP (et non kapt), le plugin Room, et ajoute Room. */
class AndroidRoomConventionPluginTest {
    @Test
    fun `room convention applies ksp and room plugins without kapt`() {
        val project = androidProjectWithRoom()

        assertTrue(project.pluginManager.hasPlugin("com.google.devtools.ksp"))
        assertTrue(project.pluginManager.hasPlugin("androidx.room"))
        assertFalse(project.pluginManager.hasPlugin("org.jetbrains.kotlin.kapt"))
    }

    @Test
    fun `room convention adds room runtime and its ksp compiler`() {
        val project = androidProjectWithRoom()

        assertEquals(listOf("libs:room-runtime"), project.coordinatesOf("implementation"))
        assertEquals(listOf("libs:room-compiler"), project.coordinatesOf("ksp"))
    }

    private fun androidProjectWithRoom(): Project {
        val project = projectWithFakeCatalog()
        project.pluginManager.apply(AndroidLibraryConventionPlugin::class.java)
        project.pluginManager.apply(AndroidRoomConventionPlugin::class.java)
        return project
    }
}
