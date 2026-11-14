package fr.babeuloula.photoswipe.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test as JupiterTest

/** Vérifie que la convention Robolectric ajoute Robolectric et le fait tourner hors ligne. */
class AndroidRobolectricConventionPluginTest {
    @JupiterTest
    fun `robolectric convention adds robolectric and resolves its android jar with gradle`() {
        val project = androidProjectWithRobolectric()

        assertEquals(listOf("libs:robolectric"), project.coordinatesOf("testImplementation"))
        assertEquals(listOf("libs:robolectric-androidAll"), project.coordinatesOf("robolectricRuntime"))
        assertEquals(listOf("libs:robolectric-androidAll-q"), project.coordinatesOf("robolectricRuntimeQ"))
        assertTrue(project.tasks.getByName("syncRobolectricJars") is Sync)
    }

    @JupiterTest
    fun `robolectric convention runs tests offline with merged android resources`() {
        val project = androidProjectWithRobolectric()
        val test = project.tasks.register("someUnitTest", Test::class.java).get()

        assertEquals("true", test.systemProperties["robolectric.offline"])
        assertEquals(
            project.layout.buildDirectory
                .dir("robolectric-jars")
                .get()
                .asFile.absolutePath,
            test.systemProperties["robolectric.dependency.dir"],
        )
        assertTrue(
            project.extensions
                .getByType(LibraryExtension::class.java)
                .testOptions.unitTests.isIncludeAndroidResources,
        )
    }

    private fun androidProjectWithRobolectric(): Project {
        val project = projectWithFakeCatalog()
        project.pluginManager.apply(AndroidLibraryConventionPlugin::class.java)
        project.pluginManager.apply(AndroidRobolectricConventionPlugin::class.java)
        return project
    }
}
