package fr.babeuloula.photoswipe.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Convention du module application : SDK, JVM et cible Play Store. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            val android = extensions.getByType(ApplicationExtension::class.java)
            configureKotlinAndroid(android)
            android.defaultConfig.targetSdk = ProjectConfig.TARGET_SDK
        }
    }
}
