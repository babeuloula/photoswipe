package fr.babeuloula.photoswipe.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base Room de l'application (SPEC §6). Son schéma est exporté dans `data/schemas/` à chaque
 * compilation et versionné : toute évolution passe par une nouvelle version et une migration testée.
 */
@Database(
    entities = [ReviewEntryEntity::class, DecisionRecordEntity::class, ConfirmedDeletionEntity::class],
    version = 4,
    exportSchema = true,
    autoMigrations = [
        // Version 2 (PS-57) : ajout du journal des décisions pour l'undo (ADR 0019).
        AutoMigration(from = 1, to = 2),
        // Version 3 (PS-108) : colonne `reopened`, par la migration manuelle [MIGRATION_2_3].
        // Version 4 (PS-135) : historique des suppressions confirmées (ADR 0034).
        AutoMigration(from = 3, to = 4),
    ],
)
abstract class PhotoSwipeDatabase : RoomDatabase() {
    /** Accès aux décisions de tri. */
    abstract fun reviewEntryDao(): ReviewEntryDao

    /** Accès au journal des décisions, pour l'undo. */
    abstract fun decisionJournalDao(): DecisionJournalDao

    /** Accès à l'historique des suppressions confirmées. */
    abstract fun confirmedDeletionDao(): ConfirmedDeletionDao

    companion object {
        /** Nom du fichier de la base sur l'appareil. */
        const val NAME: String = "photoswipe.db"
    }
}
