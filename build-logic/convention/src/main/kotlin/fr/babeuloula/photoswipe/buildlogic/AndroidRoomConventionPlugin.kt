package fr.babeuloula.photoswipe.buildlogic

import androidx.room.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

/**
 * Convention Room des modules Android (SPEC §6) : applique KSP et le plugin Room, exporte le
 * schéma de la base dans `schemas/` du module (versionné dans le dépôt, pour tester les
 * migrations), puis ajoute Room et son processeur d'annotations.
 *
 * Le processeur passe par KSP, jamais par kapt. Le plugin s'applique après une convention Android.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("androidx.room")
            extensions.getByType(RoomExtension::class.java).schemaDirectory(layout.projectDirectory.dir(SCHEMA_DIRECTORY).asFile.path)

            val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
            dependencies.add("implementation", libs.findLibrary("room-runtime").get())
            dependencies.add("ksp", libs.findLibrary("room-compiler").get())
        }
    }

    internal companion object {
        /** Dossier des schémas exportés, relatif au module. */
        const val SCHEMA_DIRECTORY: String = "schemas"
    }
}
