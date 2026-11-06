package fr.babeuloula.photoswipe.data.database

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Version 2 → 3 (PS-108, ADR 0026) : colonne `reopened` des décisions, et de leurs copies dans le
 * journal (`made_reopened`, `previous_reopened`).
 *
 * Manuelle et non automatique : l'entrée remplacée du journal (`previous_*`) est facultative, et
 * Room la lit absente quand toutes ses colonnes sont nulles. Une valeur par défaut sur
 * `previous_reopened` la ferait lire présente, mais vide ; la colonne est donc ajoutée sans valeur
 * par défaut, puis remplie seulement là où l'entrée remplacée existe.
 */
val MIGRATION_2_3: Migration =
    object : Migration(VERSION_2, VERSION_3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE review_entry ADD COLUMN reopened INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE decision_journal ADD COLUMN made_reopened INTEGER NOT NULL DEFAULT 0")
            connection.execSQL("ALTER TABLE decision_journal ADD COLUMN previous_reopened INTEGER")
            connection.execSQL(
                "UPDATE decision_journal SET previous_reopened = 0 WHERE previous_media_id IS NOT NULL",
            )
        }
    }

/** Version 2 de la base : journal des décisions (ADR 0019). */
private const val VERSION_2 = 2

/** Version 3 de la base : décisions rouvertes pour être retriées (ADR 0026). */
private const val VERSION_3 = 3
