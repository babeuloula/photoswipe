package fr.babeuloula.photoswipe.buildlogic

import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import io.gitlab.arturbosch.detekt.getSupportedKotlinVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * Convention qualité de tous les modules (SPEC §0, §12) : ktlint (format) et detekt
 * (analyse statique), tous deux bloquants.
 *
 * Elle enregistre aussi une tâche `quality` par module : `./gradlew quality` lance ktlint,
 * detekt et, sur les modules Android, Android Lint. Le plugin est appliqué automatiquement
 * par les conventions `photoswipe.android.application`, `photoswipe.android.library` et
 * `photoswipe.jvm.library` : aucun module ne peut l'oublier.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            configureKtlint()
            configureDetekt()

            val quality = tasks.register(QUALITY_TASK) {
                group = "verification"
                description = "Runs ktlint, detekt and, on Android modules, Android Lint."
                dependsOn("ktlintCheck", "detekt")
            }
            ANDROID_PLUGINS.forEach { pluginId ->
                pluginManager.withPlugin(pluginId) { quality.configure { dependsOn("lint") } }
            }
        }
    }

    private fun Project.configureKtlint() {
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        extensions.configure(KtlintExtension::class.java) {
            version.set(libs.findVersion("ktlint").get().requiredVersion)
            ignoreFailures.set(false)
            // Le code généré (Hilt, KSP) n'est pas le nôtre : on ne le vérifie pas.
            filter { exclude { it.file.invariantSeparatorsPath.contains("/build/") } }
        }
    }

    private fun Project.configureDetekt() {
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        val configFile = rootProject.file(DETEKT_CONFIG)
        extensions.configure(DetektExtension::class.java) {
            buildUponDefaultConfig = true
            ignoreFailures = false
            parallel = true
            config.setFrom(configFile)
            // Par défaut, detekt ne lit que src/main et src/test : on y ajoute les fixtures de
            // test partagées (plugin `java-test-fixtures`, PS-15), sinon elles échapperaient au contrôle.
            source.from(*TEST_FIXTURES_SOURCE_DIRS)
        }
        // detekt embarque son propre compilateur Kotlin : on l'isole de la version Kotlin
        // du projet, que Gradle tenterait sinon d'imposer à sa configuration (ADR 0003).
        configurations.matching { it.name == "detekt" }.configureEach {
            resolutionStrategy.eachDependency {
                if (requested.group == "org.jetbrains.kotlin") {
                    useVersion(getSupportedKotlinVersion())
                }
            }
        }
    }

    internal companion object {
        const val QUALITY_TASK = "quality"
        const val DETEKT_CONFIG = "config/detekt/detekt.yml"
        val TEST_FIXTURES_SOURCE_DIRS = arrayOf("src/testFixtures/java", "src/testFixtures/kotlin")
        private val ANDROID_PLUGINS = listOf("com.android.application", "com.android.library")
    }
}
