package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.model.SessionDecision
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
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
    private val sameDayMatcher: SameDayMatcher = SameDayMatcher(),
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

    /**
     * Garde [media] pour chaque autre année de revue où il est encore marqué pour suppression
     * (PS-113) : une décision vaut pour un couple (média, année), et la confirmation enverrait sinon
     * à la corbeille un média gardé cette année. Chaque décision est journalisée dans le jour de
     * revue de son année, comme dans la pile des marqués ; renvoie les décisions à empiler.
     */
    suspend fun keepOtherMarkings(
        media: MediaItem,
        day: LocalDate,
    ): List<SessionDecision> =
        reviewRepository
            .entriesMarkedForDeletion()
            .filter { it.mediaId == media.id && it.reviewYear != day.year }
            .sortedBy { it.reviewYear }
            .map { record(media, sameDayMatcher.reviewDay(media, it.reviewYear, clock.zone), ReviewDecision.KEPT) }

    /**
     * Décision [decision] pour le fichier RAW de [media] s'il en a un (PS-166), à empiler avant
     * celle de la photo : la carte s'annule alors d'un bloc. Vide sans RAW.
     */
    suspend fun recordCompanion(
        media: MediaItem,
        day: LocalDate,
        decision: ReviewDecision,
    ): List<SessionDecision> = listOfNotNull(media.rawCompanion?.let { record(it, day, decision) })
}
