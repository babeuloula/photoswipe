package fr.babeuloula.photoswipe.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base Room de l'application (SPEC §6). Son schéma est exporté dans `data/schemas/` à chaque
 * compilation et versionné : toute évolution passe par une nouvelle version et une migration testée.
 */
@Database(entities = [ReviewEntryEntity::class], version = 1, exportSchema = true)
abstract class PhotoSwipeDatabase : RoomDatabase() {
    /** Accès aux décisions de tri. */
    abstract fun reviewEntryDao(): ReviewEntryDao

    companion object {
        /** Nom du fichier de la base sur l'appareil. */
        const val NAME: String = "photoswipe.db"
    }
}
