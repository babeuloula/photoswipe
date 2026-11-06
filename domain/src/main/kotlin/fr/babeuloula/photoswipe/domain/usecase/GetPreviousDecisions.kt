package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import java.time.LocalDate

/**
 * Décisions précédentes des médias d'un jour rouvert pour être retrié (PS-108, ADR 0026) : la pile
 * les montre sur la carte (gardé, marqué pour suppression, favori).
 */
class GetPreviousDecisions(
    private val reviewRepository: ReviewRepository,
) {
    /** Entrées rouvertes des médias [mediaIds] pour l'année de revue de [day], par média. */
    suspend operator fun invoke(
        day: LocalDate,
        mediaIds: Set<MediaId>,
    ): Map<MediaId, ReviewEntry> {
        if (mediaIds.isEmpty()) return emptyMap()
        return reviewRepository
            .entriesFor(day.year, mediaIds)
            .filter { it.reopened }
            .associateBy { it.mediaId }
    }
}
