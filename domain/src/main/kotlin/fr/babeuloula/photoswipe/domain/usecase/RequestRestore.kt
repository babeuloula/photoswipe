package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.port.MediaActionRequestFactory

/**
 * Prépare la demande système de sortie de la corbeille (PS-168) : silencieuse avec l'accès
 * « Gestion des médias », dialogue d'Android sinon (ADR 0023). La réponse est enregistrée par
 * `RecordRestoreResult`.
 */
class RequestRestore(
    private val requestFactory: MediaActionRequestFactory,
) {
    /** Demandes de restauration de [media], une par lot ; vide si [media] l'est. */
    operator fun invoke(media: List<MediaItem>): List<MediaActionRequest> = requestFactory.restoreRequests(media)
}
