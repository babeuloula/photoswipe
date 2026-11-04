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
import java.util.Optional

/**
 * Catalogue `libs` simulé : `ProjectBuilder` ne lit pas `settings.gradle.kts`.
 *
 * Chaque alias demandé est résolu en `libs:<alias>`, ce qui permet aux tests de vérifier
 * quelles entrées du catalogue une convention ajoute, et dans quelle configuration.
 */
internal class FakeVersionCatalogs(private val project: Project) : VersionCatalogsExtension {
    private val libs = object : VersionCatalog {
        override fun getName() = "libs"

        override fun findLibrary(alias: String): Optional<Provider<MinimalExternalModuleDependency>> =
            Optional.of(project.provider { dependency(alias) })

        override fun findBundle(alias: String): Optional<Provider<ExternalModuleDependencyBundle>> = Optional.empty()
        override fun findVersion(alias: String): Optional<VersionConstraint> = Optional.empty()
        override fun findPlugin(alias: String): Optional<Provider<PluginDependency>> = Optional.empty()
        override fun getLibraryAliases(): List<String> = emptyList()
        override fun getBundleAliases(): List<String> = emptyList()
        override fun getVersionAliases(): List<String> = emptyList()
        override fun getPluginAliases(): List<String> = emptyList()
    }

    override fun find(name: String): Optional<VersionCatalog> =
        if (name == "libs") Optional.of(libs) else Optional.empty()

    override fun getCatalogNames(): Set<String> = setOf("libs")

    override fun iterator(): MutableIterator<VersionCatalog> = mutableListOf<VersionCatalog>(libs).iterator()

    private fun dependency(alias: String): MinimalExternalModuleDependency =
        DefaultMinimalDependency(
            DefaultModuleIdentifier.newId("libs", alias),
            DefaultMutableVersionConstraint("1.0"),
        )
}

/** Crée un projet de test qui dispose du catalogue simulé. */
internal fun projectWithFakeCatalog(): Project {
    val project = ProjectBuilder.builder().build()
    project.extensions.add(VersionCatalogsExtension::class.java, "versionCatalogs", FakeVersionCatalogs(project))
    return project
}

/** Coordonnées `groupe:nom` des dépendances déclarées dans une configuration. */
internal fun Project.coordinatesOf(configuration: String): List<String> =
    configurations.getByName(configuration).dependencies.map { "${it.group}:${it.name}" }
