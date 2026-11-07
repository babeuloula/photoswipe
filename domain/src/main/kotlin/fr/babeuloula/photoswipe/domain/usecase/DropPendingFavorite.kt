package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.port.ReviewRepository

/**
 * Retire un favori resté en attente dans l'application (refusé par la galerie), toutes années
 * (PS-124) : il ne sera plus demandé et le cœur disparaît. Le média reste gardé.
 */
class DropPendingFavorite(
    private val reviewRepository: ReviewRepository,
) {
    /** Oublie le favori en attente de [mediaId]. */
    suspend operator fun invoke(mediaId: MediaId) {
        // Même écriture qu'un favori appliqué : plus rien n'est en attente pour ce média.
        reviewRepository.markFavoritesApplied(setOf(mediaId))
    }
}
