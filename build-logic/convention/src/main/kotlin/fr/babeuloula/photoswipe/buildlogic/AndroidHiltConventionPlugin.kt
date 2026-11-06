package fr.babeuloula.photoswipe.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

/**
 * Convention Hilt des modules Android (SPEC §5.5) : applique KSP et le plugin Hilt,
 * puis ajoute la bibliothèque Hilt et son processeur d'annotations.
 *
 * Le processeur passe par KSP, jamais par kapt. Le plugin s'applique après une convention
 * Android (`photoswipe.android.application` ou `photoswipe.android.library`).
 */
class AndroidHiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            applyKsp()
            pluginManager.apply("com.google.dagger.hilt.android")

            val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
            dependencies.add("implementation", libs.findLibrary("hilt-android").get())
            dependencies.add("ksp", libs.findLibrary("hilt-compiler").get())
        }
    }
}
