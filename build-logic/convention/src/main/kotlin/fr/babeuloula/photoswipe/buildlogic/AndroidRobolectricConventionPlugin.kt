package fr.babeuloula.photoswipe.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test

/**
 * Convention Robolectric des modules Android : tests JVM qui ont besoin du framework Android
 * (ADR 0002). Le plugin s'applique après une convention Android.
 *
 * Robolectric tourne **hors ligne** : son jar Android (`robolectric-androidAll` du catalogue) est
 * résolu par Gradle, donc mis en cache en local comme en CI, puis copié dans
 * `build/robolectric-jars`, au lieu d'être téléchargé par Robolectric à chaque exécution (Maven
 * Central renvoie des 429 en CI). Les ressources et le manifeste fusionnés sont fournis aux tests.
 */
class AndroidRobolectricConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.getByType(CommonExtension::class.java).testOptions.unitTests.isIncludeAndroidResources = true

            val runtime = configurations.create(RUNTIME_CONFIGURATION) { isTransitive = false }
            val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
            dependencies.add("testImplementation", libs.findLibrary("robolectric").get())
            dependencies.add(RUNTIME_CONFIGURATION, libs.findLibrary("robolectric-androidAll").get())

            val jarsDir = layout.buildDirectory.dir("robolectric-jars")
            val syncJars =
                tasks.register(SYNC_TASK, Sync::class.java) {
                    from(runtime)
                    into(jarsDir)
                }
            tasks.withType(Test::class.java).configureEach {
                inputs
                    .files(syncJars)
                    .withPropertyName("robolectricJars")
                    .withPathSensitivity(PathSensitivity.NAME_ONLY)
                systemProperty("robolectric.offline", "true")
                systemProperty("robolectric.dependency.dir", jarsDir.get().asFile.absolutePath)
            }
        }
    }

    internal companion object {
        /** Configuration qui résout le jar Android de Robolectric. */
        const val RUNTIME_CONFIGURATION: String = "robolectricRuntime"

        /** Tâche qui copie ce jar là où Robolectric le cherche. */
        const val SYNC_TASK: String = "syncRobolectricJars"
    }
}
