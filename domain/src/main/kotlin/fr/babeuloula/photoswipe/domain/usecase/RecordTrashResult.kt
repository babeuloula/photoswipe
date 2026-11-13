package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.ConfirmedDeletion
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.port.ActivityHistory
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.DeletionHistory
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.port.UsageStatisticsRepository
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import java.time.Clock
import java.time.LocalDate

/**
 * Enregistre la réponse de l'utilisateur à une demande de corbeille (SPEC §3.5).
 *
 * Confirmée : les médias de la demande passent à `TRASHED` et quittent le journal des jours :
 * leurs décisions ne s'annulent plus (`UndoLastDecision`, ADR 0019). Refusée ou annulée : rien ne
 * change, les marquages restent en base pour une prochaine confirmation. Chaque lot est enregistré
 * séparément : une confirmation partielle ne fait passer à `TRASHED` que les médias des lots acceptés.
 *
 * Une confirmation s'ajoute aussi aux statistiques d'utilisation (PS-122) : nombre de médias et
 * poids enregistré au moment du marquage, un média marqué pour plusieurs années ne comptant qu'une
 * fois. Elle entre enfin dans l'historique des suppressions (ADR 0034), une ligne par marquage avec
 * le jour de la fenêtre qui l'a fait, lu dans le journal avant qu'il ne l'oublie ; l'historique
 * oublie au passage ce qui est sorti de la fenêtre. Elle compte aussi dans l'activité du jour de la
 * confirmation (PS-154, ADR 0037), pour les graphiques de la page « Progression ».
 *
 * Une dépendance par chose qu'une confirmation met à jour (décisions, journal, statistiques,
 * historiques) : d'où la longue liste de paramètres.
 *
 * @param window jours de la fenêtre, dont le journal dit où chaque média a été marqué.
 * @param clock instant de la confirmation.
 */
@Suppress("LongParameterList")
class RecordTrashResult(
    private val reviewRepository: ReviewRepository,
    private val journal: DecisionJournal,
    private val statistics: UsageStatisticsRepository,
    private val history: DeletionHistory,
    private val activity: ActivityHistory,
    private val window: ReviewWindowProvider,
    private val clock: Clock,
) {
    /** Enregistre la réponse à [request] : [confirmed] vaut vrai si l'utilisateur l'a acceptée. */
    suspend operator fun invoke(
        request: MediaActionRequest,
        confirmed: Boolean,
    ) {
        if (!confirmed) return
        val marked = reviewRepository.entriesMarkedForDeletion().filter { it.mediaId in request.mediaIds }
        val deletions = deletionsOf(marked, request.mediaIds)
        reviewRepository.markTrashed(request.mediaIds)
        journal.forget(request.mediaIds)
        val bytes = marked.distinctBy { it.mediaId }.sumOf { it.sizeBytes }
        statistics.recordDeletion(request.mediaIds.size, bytes)
        activity.recordTrashed(LocalDate.now(clock), request.mediaIds.size, bytes)
        val days = window.days()
        history.forgetConfirmedBefore(days.last().atStartOfDay(clock.zone).toInstant())
        history.record(deletions)
    }

    /** Une suppression par marquage, rattachée au jour de la fenêtre qui l'a fait s'il y en a un. */
    private suspend fun deletionsOf(
        marked: List<ReviewEntry>,
        mediaIds: Set<MediaId>,
    ): List<ConfirmedDeletion> {
        val now = clock.instant()
        val dayOf = markingDays(mediaIds)
        return marked.map { entry ->
            ConfirmedDeletion(entry.mediaId, dayOf[entry.mediaId to entry.reviewYear], entry.sizeBytes, now)
        }
    }

    /** Jour de la fenêtre où chaque couple (média, année de revue) a été décidé, d'après le journal. */
    private suspend fun markingDays(mediaIds: Set<MediaId>): Map<Pair<MediaId, Int>, LocalDate> =
        window
            .days()
            .flatMap { day -> journal.recordsFor(day).filter { it.mediaId in mediaIds } }
            .associate { (it.mediaId to it.day.year) to it.day }
}
