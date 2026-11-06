package fr.babeuloula.photoswipe.buildlogic

import org.gradle.api.Project
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Vérifie que la convention Hilt applique KSP (et non kapt) et ajoute Hilt et son processeur. */
class AndroidHiltConventionPluginTest {
    @Test
    fun `hilt convention applies ksp and hilt plugins without kapt`() {
        val project = androidProjectWithHilt()

        assertTrue(project.pluginManager.hasPlugin("com.google.devtools.ksp"))
        assertTrue(project.pluginManager.hasPlugin("com.google.dagger.hilt.android"))
        assertFalse(project.pluginManager.hasPlugin("org.jetbrains.kotlin.kapt"))
    }

    @Test
    fun `hilt convention adds hilt runtime and its ksp compiler`() {
        val project = androidProjectWithHilt()

        assertEquals(listOf("libs:hilt-android"), project.coordinatesOf("implementation"))
        assertEquals(listOf("libs:hilt-compiler"), project.coordinatesOf("ksp"))
    }

    private fun androidProjectWithHilt(): Project {
        val project = projectWithFakeCatalog()
        project.pluginManager.apply(AndroidLibraryConventionPlugin::class.java)
        project.pluginManager.apply(AndroidHiltConventionPlugin::class.java)
        return project
    }

    @Test
    fun `ksp processing tasks of every variant are recognized to stay out of the build cache`() {
        assertTrue(isKspTask("kspDebugKotlin"))
        assertTrue(isKspTask("kspReleaseKotlin"))
        assertTrue(isKspTask("kspReleaseUnitTestKotlin"))
        assertFalse(isKspTask("compileReleaseKotlin"))
        assertFalse(isKspTask("hiltJavaCompileRelease"))
    }
}
