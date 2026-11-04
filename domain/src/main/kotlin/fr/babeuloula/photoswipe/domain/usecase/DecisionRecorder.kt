package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.model.SessionDecision
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import java.time.Clock
import java.time.LocalDate

/**
 * Écrit la décision [decision] pour le couple ([media], année de [day]), avec un favori en attente
 * si [favoritePending] est vrai, et renvoie la
 * [SessionDecision] à empiler pour l'undo, avec l'entrée qu'elle remplace.
 *
 * Partagé par les use cases de décision (garder, marquer, favori) pour qu'ils enregistrent tous
 * de la même façon : taille du média, instant de l'horloge, écriture avant de rendre la main.
 */
internal suspend fun ReviewRepository.recordDecision(
    media: MediaItem,
    day: LocalDate,
    decision: ReviewDecision,
    clock: Clock,
    favoritePending: Boolean = false,
): SessionDecision {
    val previous = entriesFor(day.year, setOf(media.id)).singleOrNull()
    save(
        ReviewEntry(
            mediaId = media.id,
            reviewYear = day.year,
            decision = decision,
            sizeBytes = media.sizeBytes,
            decidedAt = clock.instant(),
            favoritePending = favoritePending,
        ),
    )
    return SessionDecision(media, day, previous)
}
