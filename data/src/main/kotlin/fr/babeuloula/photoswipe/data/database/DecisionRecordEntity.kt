package fr.babeuloula.photoswipe.data.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Ligne de la table `decision_journal` : une décision d'un jour, telle que l'undo doit pouvoir
 * l'annuler, même après la mort du process (SPEC §3.4, ADR 0019).
 *
 * @property id rang dans le journal, attribué par SQLite (croissant) : il ordonne les décisions.
 * @property day jour cible de la pile, en jours depuis l'epoch (`LocalDate.toEpochDay`).
 * @property made entrée écrite par la décision, colonnes préfixées `made_`.
 * @property previous entrée remplacée, colonnes préfixées `previous_`, toutes nulles si le média
 * était à trier.
 */
@Entity(
    tableName = "decision_journal",
    indices = [Index("day"), Index("made_media_id")],
)
data class DecisionRecordEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "day") val day: Long,
    @Embedded(prefix = "made_") val made: ReviewEntryEntity,
    @Embedded(prefix = "previous_") val previous: ReviewEntryEntity?,
)
