package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.model.SessionDecision
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import java.time.Clock
import java.time.LocalDate

/**
 * Enregistrement commun aux use cases de décision (garder, marquer, favori), pour qu'ils écrivent
 * tous de la même façon : taille du média, instant de l'horloge, décision et journal d'un seul
 * coup, avant de rendre la main (SPEC §3.4, ADR 0019).
 */
internal class DecisionRecorder(
    private val reviewRepository: ReviewRepository,
    private val journal: DecisionJournal,
    private val clock: Clock,
) {
    /**
     * Écrit [decision] pour le couple ([media], année de [day]), avec un favori en attente si
     * [favoritePending] est vrai, et la journalise ; renvoie la [SessionDecision] à empiler, avec
     * l'entrée qu'elle remplace.
     */
    suspend fun record(
        media: MediaItem,
        day: LocalDate,
        decision: ReviewDecision,
        favoritePending: Boolean = false,
    ): SessionDecision {
        val previous = reviewRepository.entriesFor(day.year, setOf(media.id)).singleOrNull()
        val made =
            ReviewEntry(
                mediaId = media.id,
                reviewYear = day.year,
                decision = decision,
                sizeBytes = media.sizeBytes,
                decidedAt = clock.instant(),
                favoritePending = favoritePending,
            )
        return SessionDecision(media, journal.record(day, made, previous))
    }
}
