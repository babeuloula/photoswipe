package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.port.ReviewRepository

/**
 * Médias mis en favori dans l'application mais pas encore dans la galerie (`favorite_pending`,
 * demande refusée ou pas encore lancée), toutes années de revue confondues : la pile leur affiche
 * quand même un cœur (PS-86).
 */
class GetPendingFavorites(
    private val reviewRepository: ReviewRepository,
) {
    /** Identifiants des médias en favori en attente. */
    suspend operator fun invoke(): Set<MediaId> =
        reviewRepository.entriesWithPendingFavorite().mapTo(HashSet()) { it.mediaId }
}
