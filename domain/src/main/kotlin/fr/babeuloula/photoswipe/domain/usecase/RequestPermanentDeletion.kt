package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.port.MediaActionRequestFactory

/**
 * Prépare la demande système de suppression définitive d'un média de la corbeille (PS-168) :
 * dialogue d'Android, ou rien avec l'accès « Gestion des médias », l'écran demandant alors sa propre
 * confirmation (ADR 0023). L'historique garde la suppression : le média reste supprimé.
 */
class RequestPermanentDeletion(
    private val requestFactory: MediaActionRequestFactory,
) {
    /** Demandes de suppression définitive de [media], une par lot ; vide si [media] l'est. */
    operator fun invoke(media: List<MediaItem>): List<MediaActionRequest> = requestFactory.deleteRequests(media)
}
