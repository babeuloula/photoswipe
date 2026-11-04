package fr.babeuloula.photoswipe.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import fr.babeuloula.photoswipe.domain.model.ReviewDecision

/**
 * Ligne de la table `review_entry` : la décision d'un couple (média, année de revue) (SPEC §6).
 *
 * La clé primaire composite `(media_id, review_year)` sert d'index unique : une seule décision
 * par média et par année de revue. La décision est stockée sous le nom de la constante de
 * [ReviewDecision] (texte) : renommer une constante impose une migration.
 *
 * @property mediaId identifiant MediaStore du média.
 * @property reviewYear année du jour cible de la décision (SPEC §3.3).
 * @property decision décision prise.
 * @property favoritePending favori système demandé, pas encore appliqué.
 * @property sizeBytes taille du média au moment de la décision, pour l'espace libéré estimé.
 * @property decidedAt instant de la décision en millisecondes epoch, pour l'undo.
 */
@Entity(tableName = "review_entry", primaryKeys = ["media_id", "review_year"])
data class ReviewEntryEntity(
    @ColumnInfo(name = "media_id") val mediaId: Long,
    @ColumnInfo(name = "review_year") val reviewYear: Int,
    @ColumnInfo(name = "decision") val decision: ReviewDecision,
    @ColumnInfo(name = "favorite_pending") val favoritePending: Boolean,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "decided_at") val decidedAt: Long,
)
