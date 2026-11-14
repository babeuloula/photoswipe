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
 * Central renvoie des 429 en CI). Deux jars : l'API 34 des tests et l'API 29 de notre `minSdk`, pour
 * tester les chemins d'Android 10 (PS-181). Les ressources et le manifeste fusionnés sont fournis aux tests.
 */
class AndroidRobolectricConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.getByType(CommonExtension::class.java).testOptions.unitTests.isIncludeAndroidResources = true

            val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
            dependencies.add("testImplementation", libs.findLibrary("robolectric").get())
            // API 34 par défaut (`robolectric.properties`), API 29 pour les tests `@Config(sdk = [29])`.
            // Une configuration par jar : dans une seule, Gradle ne garderait que la version la plus haute.
            val runtimes =
                ANDROID_JARS.map { (configuration, alias) ->
                    configurations.create(configuration) { isTransitive = false }.also {
                        dependencies.add(configuration, libs.findLibrary(alias).get())
                    }
                }

            val jarsDir = layout.buildDirectory.dir("robolectric-jars")
            val syncJars =
                tasks.register(SYNC_TASK, Sync::class.java) {
                    from(runtimes)
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
        /** Configuration qui résout le jar Android de l'API 34 de Robolectric. */
        const val RUNTIME_CONFIGURATION: String = "robolectricRuntime"

        /** Configuration qui résout le jar Android de l'API 29 (Android 10, notre `minSdk`). */
        const val Q_RUNTIME_CONFIGURATION: String = "robolectricRuntimeQ"

        /** Configuration de chaque jar Android et son entrée du catalogue. */
        val ANDROID_JARS: List<Pair<String, String>> =
            listOf(
                RUNTIME_CONFIGURATION to "robolectric-androidAll",
                Q_RUNTIME_CONFIGURATION to "robolectric-androidAll-q",
            )

        /** Tâche qui copie ces jars là où Robolectric les cherche. */
        const val SYNC_TASK: String = "syncRobolectricJars"
    }
}
