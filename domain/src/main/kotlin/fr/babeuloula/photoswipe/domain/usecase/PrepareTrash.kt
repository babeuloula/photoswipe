package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.port.MediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository

/**
 * Prépare les demandes système d'envoi à la corbeille des médias marqués (SPEC §3.5).
 *
 * Tous les marquages en attente sont repris, quelle que soit la session ou l'année de revue :
 * c'est l'ensemble annoncé par le résumé (ADR 0011). Un média marqué pour deux années de revue
 * n'est demandé qu'une fois. Un média qui n'est plus lisible (supprimé, déjà à la corbeille, hors
 * de la sélection en accès partiel) est laissé de côté : son entrée reste en base, et le nettoyage
 * des orphelins s'en charge s'il a vraiment disparu (SPEC §6).
 *
 * La fabrique découpe les médias en lots (environ 500 par demande, ADR 0007) : chaque demande sera
 * confirmée ou refusée séparément, puis enregistrée par `RecordTrashResult`.
 */
class PrepareTrash(
    private val reviewRepository: ReviewRepository,
    private val mediaSource: MediaSource,
    private val requestFactory: MediaActionRequestFactory,
) {
    /** Demandes de corbeille à présenter à l'utilisateur, une par lot ; vide si rien n'est marqué. */
    suspend operator fun invoke(): List<MediaActionRequest> {
        val markedIds = reviewRepository.entriesMarkedForDeletion().mapTo(LinkedHashSet()) { it.mediaId }
        if (markedIds.isEmpty()) return emptyList()

        return requestFactory.trashRequests(mediaSource.mediaByIds(markedIds))
    }
}
