package fr.babeuloula.photoswipe.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.internal.artifacts.dependencies.DefaultMinimalDependency
import org.gradle.api.internal.artifacts.dependencies.DefaultMutableVersionConstraint
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional

/**
 * Vérifie que la convention Hilt applique KSP (et non kapt) et ajoute Hilt et son processeur.
 *
 * `ProjectBuilder` ne lit pas `settings.gradle.kts` : le catalogue `libs` est donc simulé.
 */
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

        assertEquals(listOf("com.google.dagger:hilt-android"), coordinatesOf(project, "implementation"))
        assertEquals(listOf("com.google.dagger:hilt-compiler"), coordinatesOf(project, "ksp"))
    }

    private fun androidProjectWithHilt(): Project {
        val project = ProjectBuilder.builder().build()
        project.extensions.add(VersionCatalogsExtension::class.java, "versionCatalogs", FakeCatalogs(project))
        project.pluginManager.apply(AndroidLibraryConventionPlugin::class.java)
        project.pluginManager.apply(AndroidHiltConventionPlugin::class.java)
        return project
    }

    private fun coordinatesOf(project: Project, configuration: String): List<String> =
        project.configurations.getByName(configuration).dependencies.map { "${it.group}:${it.name}" }

    /** Catalogue minimal : seules les bibliothèques demandées par la convention sont résolues. */
    private class FakeCatalogs(private val project: Project) : VersionCatalogsExtension {
        private val libs = object : VersionCatalog {
            override fun getName() = "libs"

            override fun findLibrary(alias: String): Optional<Provider<MinimalExternalModuleDependency>> {
                return Optional.of(project.provider { dependency("com.google.dagger", alias) })
            }

            override fun findBundle(alias: String): Optional<Provider<ExternalModuleDependencyBundle>> = Optional.empty()
            override fun findVersion(alias: String): Optional<VersionConstraint> = Optional.empty()
            override fun findPlugin(alias: String): Optional<Provider<PluginDependency>> = Optional.empty()
            override fun getLibraryAliases(): List<String> = listOf("hilt-android", "hilt-compiler")
            override fun getBundleAliases(): List<String> = emptyList()
            override fun getVersionAliases(): List<String> = emptyList()
            override fun getPluginAliases(): List<String> = emptyList()
        }

        override fun find(name: String): Optional<VersionCatalog> =
            if (name == "libs") Optional.of(libs) else Optional.empty()

        override fun getCatalogNames(): Set<String> = setOf("libs")

        override fun iterator(): MutableIterator<VersionCatalog> = mutableListOf<VersionCatalog>(libs).iterator()

        private fun dependency(group: String, name: String): MinimalExternalModuleDependency =
            DefaultMinimalDependency(
                DefaultModuleIdentifier.newId(group, name),
                DefaultMutableVersionConstraint("1.0"),
            )
    }
}
