package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.ConfirmedDeletion
import fr.babeuloula.photoswipe.domain.model.Trash
import fr.babeuloula.photoswipe.domain.model.TrashItem
import fr.babeuloula.photoswipe.domain.port.DeletionHistory
import fr.babeuloula.photoswipe.domain.port.SystemTrash
import kotlinx.coroutines.flow.first

/**
 * Corbeille de PhotoSwipe (PS-168) : les médias de l'historique des suppressions (ADR 0034) encore
 * dans la corbeille d'Android.
 *
 * Seules les suppressions faites par PhotoSwipe y figurent ; un média déjà vidé de la corbeille ou
 * restauré ailleurs n'y est plus, sans erreur. La date de suppression définitive est celle que lit
 * MediaStore, à défaut la confirmation plus 30 jours ; le poids est celui de MediaStore, à défaut
 * celui enregistré au marquage.
 */
class GetTrash(
    private val history: DeletionHistory,
    private val systemTrash: SystemTrash,
) {
    /** Médias encore restaurables, du plus proche de sa suppression définitive au plus lointain. */
    suspend operator fun invoke(): Trash {
        val latest =
            history
                .observe()
                .first()
                .groupBy { it.mediaId }
                .mapValues { (_, deletions) -> deletions.maxBy(ConfirmedDeletion::confirmedAt) }
        if (latest.isEmpty()) return Trash()
        val items =
            systemTrash.trashed(latest.keys).mapNotNull { trashed ->
                val deletion = latest[trashed.media.id] ?: return@mapNotNull null
                val media =
                    if (trashed.media.sizeBytes > 0) {
                        trashed.media
                    } else {
                        trashed.media.copy(sizeBytes = deletion.sizeBytes)
                    }
                TrashItem(media, deletion.confirmedAt, trashed.purgeAt ?: deletion.expectedPurgeAt)
            }
        return Trash(items.sortedWith(compareBy<TrashItem> { it.purgeAt }.thenBy { it.media.id.value }))
    }
}
