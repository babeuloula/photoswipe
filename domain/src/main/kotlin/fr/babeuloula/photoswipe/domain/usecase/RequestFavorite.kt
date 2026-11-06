package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.port.MediaActionRequestFactory

/**
 * Prépare la demande système d'ajout aux favoris de la galerie d'un média, dès qu'il est mis en
 * favori dans la pile (SPEC §2, §3.5) : silencieuse avec l'accès « Gestion des médias », dialogue
 * d'Android sinon. La réponse est enregistrée par `RecordFavoriteResult`.
 */
class RequestFavorite(
    private val requestFactory: MediaActionRequestFactory,
) {
    /** Demande de mise en favori de [media], seul dans son lot. */
    operator fun invoke(media: MediaItem): MediaActionRequest = requestFactory.favoriteRequests(listOf(media)).single()
}
