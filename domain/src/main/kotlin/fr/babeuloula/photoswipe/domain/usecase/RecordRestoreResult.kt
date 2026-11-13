package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.port.ActivityHistory
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.DeletionHistory
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.port.UsageStatisticsRepository
import kotlinx.coroutines.flow.first
import java.time.Clock

/**
 * Enregistre la réponse à une demande de restauration depuis la page « Corbeille » (PS-168).
 *
 * Acceptée : chaque média revient dans la galerie comme un souvenir à trier. Ses entrées restantes
 * (`TRASHED`, si le nettoyage des orphelins ne les a pas déjà purgées) et ses décisions du journal
 * sont retirées ; sa suppression sort de l'historique (ADR 0034), donc du résumé et de l'accueil ;
 * elle est retirée des statistiques d'utilisation (ADR 0030) et de l'activité du jour où elle avait
 * été confirmée (ADR 0037), un média ne comptant qu'une fois. Refusée : rien ne change.
 *
 * Une dépendance par chose qu'une restauration corrige : d'où la longue liste de paramètres.
 *
 * @param clock fuseau dans lequel le jour de chaque confirmation est lu.
 */
@Suppress("LongParameterList")
class RecordRestoreResult(
    private val history: DeletionHistory,
    private val reviewRepository: ReviewRepository,
    private val journal: DecisionJournal,
    private val statistics: UsageStatisticsRepository,
    private val activity: ActivityHistory,
    private val clock: Clock,
) {
    /** Enregistre la réponse à [request] : [confirmed] vaut vrai si les médias sont restaurés. */
    suspend operator fun invoke(
        request: MediaActionRequest,
        confirmed: Boolean,
    ) {
        if (!confirmed) return
        val ids = request.mediaIds
        val restored =
            history
                .observe()
                .first()
                .filter { it.mediaId in ids }
                .groupBy { it.mediaId }
                .map { (_, deletions) -> deletions.maxBy { it.confirmedAt } }
        if (restored.isNotEmpty()) {
            statistics.removeDeletion(restored.size, restored.sumOf { it.sizeBytes })
            restored
                .groupBy { it.confirmedAt.atZone(clock.zone).toLocalDate() }
                .forEach { (day, deletions) ->
                    activity.removeTrashed(day, deletions.size, deletions.sumOf { it.sizeBytes })
                }
        }
        history.forget(ids)
        reviewRepository.deleteAllFor(ids)
        journal.forget(ids)
    }
}
