package fr.babeuloula.photoswipe.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Ligne de la table `confirmed_deletion` : un marquage envoyé à la corbeille système (ADR 0034).
 *
 * @property id rang attribué par SQLite.
 * @property mediaId identifiant MediaStore du média supprimé.
 * @property day jour de revue qui l'a marqué, en jours depuis l'epoch, `null` s'il était déjà
 * sorti de la fenêtre.
 * @property sizeBytes taille du média au moment du marquage.
 * @property confirmedAt instant de la confirmation, en millisecondes epoch.
 */
@Entity(tableName = "confirmed_deletion", indices = [Index("confirmed_at")])
data class ConfirmedDeletionEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "media_id") val mediaId: Long,
    @ColumnInfo(name = "day") val day: Long?,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "confirmed_at") val confirmedAt: Long,
)
