package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.port.UsageStatisticsRepository

/**
 * Enregistre la réponse de l'utilisateur à une demande de corbeille (SPEC §3.5).
 *
 * Confirmée : les médias de la demande passent à `TRASHED` et quittent le journal des jours :
 * leurs décisions ne s'annulent plus (`UndoLastDecision`, ADR 0019). Refusée ou annulée : rien ne change, les marquages restent en base pour une
 * prochaine confirmation. Chaque lot est enregistré séparément : une confirmation partielle ne
 * fait passer à `TRASHED` que les médias des lots acceptés.
 *
 * Une confirmation s'ajoute aussi aux statistiques d'utilisation (PS-122) : nombre de médias et
 * poids enregistré au moment du marquage, un média marqué pour plusieurs années ne comptant qu'une fois.
 */
class RecordTrashResult(
    private val reviewRepository: ReviewRepository,
    private val journal: DecisionJournal,
    private val statistics: UsageStatisticsRepository,
) {
    /** Enregistre la réponse à [request] : [confirmed] vaut vrai si l'utilisateur l'a acceptée. */
    suspend operator fun invoke(
        request: MediaActionRequest,
        confirmed: Boolean,
    ) {
        if (!confirmed) return
        val freedBytes =
            reviewRepository
                .entriesMarkedForDeletion()
                .filter { it.mediaId in request.mediaIds }
                .distinctBy { it.mediaId }
                .sumOf { it.sizeBytes }
        reviewRepository.markTrashed(request.mediaIds)
        journal.forget(request.mediaIds)
        statistics.recordDeletion(request.mediaIds.size, freedBytes)
    }
}
