package fr.babeuloula.photoswipe.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Convention des modules bibliothèque Android (ex. `:data`). */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            configureKotlinAndroid(extensions.getByType(LibraryExtension::class.java))
        }
    }
}
