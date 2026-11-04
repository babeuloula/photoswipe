package fr.babeuloula.photoswipe.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test as JupiterTest

/**
 * Vérifie que chaque plugin de convention applique bien les valeurs partagées (SPEC §7)
 * sur un projet Gradle de test.
 */
class ConventionPluginsTest {
    private fun newProject(): Project = projectWithFakeCatalog()

    @JupiterTest
    fun `android application convention sets sdk levels and jvm toolchain 17`() {
        val project = newProject()
        project.pluginManager.apply(AndroidApplicationConventionPlugin::class.java)

        val android = project.extensions.getByType(ApplicationExtension::class.java)
        assertEquals(37, android.compileSdk)
        assertEquals(33, android.defaultConfig.minSdk)
        assertEquals(37, android.defaultConfig.targetSdk)
        assertKotlinToolchainIs17(project)
    }

    @JupiterTest
    fun `android library convention sets sdk levels and jvm toolchain 17`() {
        val project = newProject()
        project.pluginManager.apply(AndroidLibraryConventionPlugin::class.java)

        val android = project.extensions.getByType(LibraryExtension::class.java)
        assertEquals(37, android.compileSdk)
        assertEquals(33, android.defaultConfig.minSdk)
        assertKotlinToolchainIs17(project)
    }

    @JupiterTest
    fun `android conventions run instrumented tests with the androidx junit runner`() {
        val library = newProject()
        library.pluginManager.apply(AndroidLibraryConventionPlugin::class.java)
        val application = newProject()
        application.pluginManager.apply(AndroidApplicationConventionPlugin::class.java)

        val runner = "androidx.test.runner.AndroidJUnitRunner"
        assertEquals(runner, library.extensions.getByType(LibraryExtension::class.java).defaultConfig.testInstrumentationRunner)
        assertEquals(runner, application.extensions.getByType(ApplicationExtension::class.java).defaultConfig.testInstrumentationRunner)
    }

    @JupiterTest
    fun `jvm library convention stays free of android and runs tests on junit platform`() {
        val project = newProject()
        project.pluginManager.apply(JvmLibraryConventionPlugin::class.java)

        assertTrue(project.pluginManager.hasPlugin("org.jetbrains.kotlin.jvm"))
        assertFalse(project.pluginManager.hasPlugin("com.android.library"))
        assertFalse(project.pluginManager.hasPlugin("com.android.application"))
        assertKotlinToolchainIs17(project)
        val testTask = project.tasks.withType(Test::class.java).getByName("test")
        assertTrue(testTask.options is JUnitPlatformOptions)
    }

    /** `jvmToolchain(17)` de Kotlin configure le toolchain Java du projet. */
    private fun assertKotlinToolchainIs17(project: Project) {
        val toolchain = project.extensions.getByType(JavaPluginExtension::class.java).toolchain
        assertEquals(17, toolchain.languageVersion.get().asInt())
    }
}
