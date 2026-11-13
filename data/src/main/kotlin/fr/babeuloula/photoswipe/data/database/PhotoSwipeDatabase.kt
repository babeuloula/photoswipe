package fr.babeuloula.photoswipe.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Base Room de l'application (SPEC §6). Son schéma est exporté dans `data/schemas/` à chaque
 * compilation et versionné : toute évolution passe par une nouvelle version et une migration testée.
 */
@Database(
    entities = [
        ReviewEntryEntity::class,
        DecisionRecordEntity::class,
        ConfirmedDeletionEntity::class,
        DailyActivityEntity::class,
        PhotoMeasureEntity::class,
    ],
    version = 7,
    exportSchema = true,
    autoMigrations = [
        // Version 2 (PS-57) : ajout du journal des décisions pour l'undo (ADR 0019).
        AutoMigration(from = 1, to = 2),
        // Version 3 (PS-108) : colonne `reopened`, par la migration manuelle [MIGRATION_2_3].
        // Version 4 (PS-135) : historique des suppressions confirmées (ADR 0034).
        AutoMigration(from = 3, to = 4),
        // Version 5 (PS-154) : activité de tri par jour (ADR 0037).
        AutoMigration(from = 4, to = 5),
        // Version 6 (PS-164) : mesures des photos peut-être ratées (ADR 0039).
        AutoMigration(from = 5, to = 6),
        // Version 7 (PS-165) : empreinte des photos, pour repérer les doublons.
        AutoMigration(from = 6, to = 7),
    ],
)
abstract class PhotoSwipeDatabase : RoomDatabase() {
    /** Accès aux décisions de tri. */
    abstract fun reviewEntryDao(): ReviewEntryDao

    /** Accès au journal des décisions, pour l'undo. */
    abstract fun decisionJournalDao(): DecisionJournalDao

    /** Accès à l'historique des suppressions confirmées. */
    abstract fun confirmedDeletionDao(): ConfirmedDeletionDao

    /** Accès aux mesures des photos. */
    abstract fun photoMeasureDao(): PhotoMeasureDao

    /** Accès à l'activité de tri par jour. */
    abstract fun dailyActivityDao(): DailyActivityDao

    companion object {
        /** Nom du fichier de la base sur l'appareil. */
        const val NAME: String = "photoswipe.db"
    }
}
