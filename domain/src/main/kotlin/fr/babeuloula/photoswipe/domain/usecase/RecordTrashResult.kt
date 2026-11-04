package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.port.ReviewRepository

/**
 * Enregistre la réponse de l'utilisateur à une demande de corbeille (SPEC §3.5).
 *
 * Confirmée : les médias de la demande passent à `TRASHED` ; leurs décisions ne s'annulent plus
 * (`UndoLastDecision`). Refusée ou annulée : rien ne change, les marquages restent en base pour une
 * prochaine confirmation. Chaque lot est enregistré séparément : une confirmation partielle ne
 * fait passer à `TRASHED` que les médias des lots acceptés.
 */
class RecordTrashResult(
    private val reviewRepository: ReviewRepository,
) {
    /** Enregistre la réponse à [request] : [confirmed] vaut vrai si l'utilisateur l'a acceptée. */
    suspend operator fun invoke(
        request: MediaActionRequest,
        confirmed: Boolean,
    ) {
        if (confirmed) reviewRepository.markTrashed(request.mediaIds)
    }
}
