package fr.babeuloula.photoswipe.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

/**
 * Convention Jetpack Compose des modules Android (SPEC §7) : applique le compilateur Compose
 * de Kotlin, active la fonctionnalité Compose d'AGP et aligne les bibliothèques via la BOM.
 *
 * Seul le socle commun est ajouté ici (UI, aperçus `@Preview`). Chaque module déclare
 * ensuite ce qui lui est propre (Material 3, intégration Activity, tests UI).
 * Le plugin s'applique après une convention Android.
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            extensions.getByType(CommonExtension::class.java).buildFeatures.compose = true

            val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
            val bom = dependencies.platform(libs.findLibrary("androidx-compose-bom").get())
            dependencies.add("implementation", bom)
            dependencies.add("testImplementation", bom)
            dependencies.add("implementation", libs.findLibrary("androidx-compose-ui").get())
            dependencies.add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
            // L'outillage des aperçus ne part jamais dans l'APK de release.
            dependencies.add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        }
    }
}
