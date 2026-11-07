package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.port.MediaActionRequestFactory

/**
 * Prépare la demande système de retrait des favoris de la galerie d'un média, depuis le menu de la
 * pile (PS-124) : silencieuse avec l'accès « Gestion des médias », dialogue d'Android sinon. Elle
 * ne prend aucune décision de tri : la carte reste en place.
 */
class RequestUnfavorite(
    private val requestFactory: MediaActionRequestFactory,
) {
    /** Demande de retrait des favoris de [media], seul dans son lot. */
    operator fun invoke(media: MediaItem): MediaActionRequest =
        requestFactory.unfavoriteRequests(listOf(media)).single()
}
