package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.domain.port.ReviewRepository

/**
 * Résumé affiché avant de confirmer (SPEC §3.5) : gardés, marqués, favoris, espace libéré estimé.
 *
 * Marqués, favoris et espace libéré viennent des demandes en attente en base, toutes sessions
 * confondues : ce sont elles que la confirmation enverra. Un média marqué (ou en favori) pour deux
 * années de revue ne compte qu'une fois, comme dans la demande système. L'espace libéré additionne
 * la taille de chaque média mémorisée au moment de sa décision. Les gardés sont ceux de [ReviewSession] dont
 * l'entrée actuelle est `KEPT` sans favori : un undo ou une nouvelle décision sur le même média sont
 * donc pris en compte, et les trois nombres ne se recoupent pas (ADR 0011).
 *
 * Résultat ponctuel (`suspend`), comme le paquet : l'écran de résumé le lit une fois.
 */
class GetSessionSummary(
    private val reviewRepository: ReviewRepository,
) {
    /** Résumé de [session] et des demandes en attente. */
    suspend operator fun invoke(session: ReviewSession): SessionSummary {
        val marked = reviewRepository.entriesMarkedForDeletion().distinctBy { it.mediaId }
        return SessionSummary(
            keptCount = keptCount(session),
            markedForDeletionCount = marked.size,
            favoriteCount = reviewRepository.entriesWithPendingFavorite().distinctBy { it.mediaId }.size,
            estimatedFreedBytes = marked.sumOf { it.sizeBytes },
        )
    }

    /** Médias de [session] dont l'entrée actuelle est gardée sans favori, lus une année de revue à la fois. */
    private suspend fun keptCount(session: ReviewSession): Int =
        session.decisions
            .groupBy({ it.reviewYear }, { it.media.id })
            .entries
            .sumOf { (reviewYear, mediaIds) ->
                reviewRepository
                    .entriesFor(reviewYear, mediaIds.toSet())
                    .count { it.decision == ReviewDecision.KEPT && !it.favoritePending }
            }
}
