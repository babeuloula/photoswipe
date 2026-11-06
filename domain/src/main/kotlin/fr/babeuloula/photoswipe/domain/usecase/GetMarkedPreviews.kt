package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository

/**
 * Vignettes des médias marqués pour suppression, à montrer dans le résumé avant de confirmer
 * (ADR 0025) : les [limit] marqués le plus récemment, encore dans la galerie, du plus récent au plus
 * ancien. Un média marqué pour deux années de revue n'apparaît qu'une fois.
 *
 * Résultat ponctuel (`suspend`), lu avec le résumé.
 */
class GetMarkedPreviews(
    private val reviewRepository: ReviewRepository,
    private val mediaSource: MediaSource,
) {
    /** Au plus [limit] médias marqués, les plus récemment décidés d'abord. */
    suspend operator fun invoke(limit: Int = DEFAULT_LIMIT): List<MediaItem> {
        require(limit >= 0) { "limit must not be negative: $limit" }
        val ids =
            reviewRepository
                .entriesMarkedForDeletion()
                .sortedByDescending { it.decidedAt }
                .map { it.mediaId }
                .distinct()
                .take(limit)
        if (ids.isEmpty()) return emptyList()
        val media = mediaSource.mediaByIds(ids.toSet()).associateBy { it.id }
        return ids.mapNotNull { media[it] }
    }

    /** Constantes du use case. */
    companion object {
        /** Vignettes du résumé : cinq photos, puis le nombre des autres. */
        const val DEFAULT_LIMIT = 5
    }
}
