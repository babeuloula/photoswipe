package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.model.SessionDecision
import fr.babeuloula.photoswipe.domain.model.isSorted
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import java.time.LocalDate

/**
 * Reprend la pile d'undo d'un jour depuis le journal (SPEC §3.4, ADR 0019) : en revenant sur la
 * pile, ou après la mort du process, « Annuler » remonte encore les décisions déjà prises.
 *
 * Une décision du journal n'est reprise que si son média est encore lisible (sinon il ne pourrait
 * pas revenir sur la pile) et encore décidé pour l'année de revue (sinon il est déjà sur la pile).
 * Les jours sortis de la fenêtre de revue ne seront plus jamais ouverts : ils sont oubliés au
 * passage.
 */
class GetDaySession(
    private val journal: DecisionJournal,
    private val reviewRepository: ReviewRepository,
    private val mediaSource: MediaSource,
    private val windowProvider: ReviewWindowProvider,
) {
    /** Session du jour cible [day] : ses décisions encore annulables, de la plus ancienne à la plus récente. */
    suspend operator fun invoke(day: LocalDate): ReviewSession {
        windowProvider.days().minOrNull()?.let { journal.forgetDaysBefore(it) }
        val records = journal.recordsFor(day)
        if (records.isEmpty()) return ReviewSession()

        val ids = records.mapTo(LinkedHashSet()) { it.mediaId }
        val decided = reviewRepository.entriesFor(day.year, ids).filter { it.isSorted }.mapTo(HashSet()) { it.mediaId }
        val media = mediaSource.mediaByIds(ids).associateBy { it.id }
        val decisions =
            records.mapNotNull { record ->
                media[record.mediaId]?.takeIf { record.mediaId in decided }?.let { SessionDecision(it, record) }
            }
        return ReviewSession(decisions)
    }
}
