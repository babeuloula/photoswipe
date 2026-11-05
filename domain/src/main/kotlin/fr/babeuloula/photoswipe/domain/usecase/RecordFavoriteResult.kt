package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.port.ReviewRepository

/**
 * Enregistre la réponse de l'utilisateur à une demande de mise en favori (SPEC §3.5).
 *
 * Acceptée : les favoris en attente des médias de la demande sont appliqués (`favorite_pending`
 * remis à faux). Refusée ou annulée : rien ne change, les favoris restent en attente pour une
 * prochaine confirmation. Chaque lot est enregistré séparément.
 */
class RecordFavoriteResult(
    private val reviewRepository: ReviewRepository,
) {
    /** Enregistre la réponse à [request] : [confirmed] vaut vrai si l'utilisateur l'a acceptée. */
    suspend operator fun invoke(
        request: MediaActionRequest,
        confirmed: Boolean,
    ) {
        if (confirmed) reviewRepository.markFavoritesApplied(request.mediaIds)
    }
}
