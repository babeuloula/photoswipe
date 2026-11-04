package fr.babeuloula.photoswipe.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

/**
 * Convention des modules Kotlin JVM purs (ex. `:domain`) : aucune dépendance Android,
 * tests sur la plateforme JUnit (Jupiter).
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureKotlinToolchain()
            tasks.withType(Test::class.java).configureEach { useJUnitPlatform() }
        }
    }
}
