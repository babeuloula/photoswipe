package fr.babeuloula.photoswipe.buildlogic

import com.android.build.api.dsl.LibraryExtension
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Vérifie que la convention qualité rend ktlint, detekt et Android Lint bloquants (SPEC §0, §12)
 * et qu'elle est appliquée d'office par chaque convention de module.
 */
class QualityConventionPluginTest {
    @Test
    fun `every module convention applies ktlint and detekt`() {
        listOf(
            AndroidApplicationConventionPlugin::class.java,
            AndroidLibraryConventionPlugin::class.java,
            JvmLibraryConventionPlugin::class.java,
        ).forEach { convention ->
            val project = projectWithFakeCatalog()
            project.pluginManager.apply(convention)

            assertTrue(project.pluginManager.hasPlugin("org.jlleitschuh.gradle.ktlint"), "ktlint: $convention")
            assertTrue(project.pluginManager.hasPlugin("io.gitlab.arturbosch.detekt"), "detekt: $convention")
        }
    }

    @Test
    fun `ktlint and detekt fail the build on any issue`() {
        val project = jvmProject()

        val ktlint = project.extensions.getByType(KtlintExtension::class.java)
        assertFalse(ktlint.ignoreFailures.get())
        assertEquals("1.0", ktlint.version.get())

        val detekt = project.extensions.getByType(DetektExtension::class.java)
        assertFalse(detekt.ignoreFailures)
        assertTrue(detekt.buildUponDefaultConfig)
        assertEquals(project.rootProject.file("config/detekt/detekt.yml"), detekt.config.singleFile)
    }

    @Test
    fun `android lint treats missing translations as errors and aborts the build`() {
        val project = projectWithFakeCatalog()
        project.pluginManager.apply(AndroidLibraryConventionPlugin::class.java)

        val lint = project.extensions.getByType(LibraryExtension::class.java).lint
        assertTrue("MissingTranslation" in lint.error)
        assertTrue(lint.abortOnError)
    }

    @Test
    fun `quality task runs ktlint, detekt and android lint on android modules`() {
        val project = projectWithFakeCatalog()
        project.pluginManager.apply(AndroidLibraryConventionPlugin::class.java)

        assertEquals(setOf("ktlintCheck", "detekt", "lint"), qualityDependencies(project))
    }

    @Test
    fun `quality task runs ktlint and detekt on jvm modules`() {
        assertEquals(setOf("ktlintCheck", "detekt"), qualityDependencies(jvmProject()))
    }

    private fun jvmProject(): Project {
        val project = projectWithFakeCatalog()
        project.pluginManager.apply(JvmLibraryConventionPlugin::class.java)
        return project
    }

    private fun qualityDependencies(project: Project): Set<String> =
        project.tasks.getByName("quality").dependsOn.map { it.toString() }.toSet()
}
