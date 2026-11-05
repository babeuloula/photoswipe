package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.port.MediaActionRequestFactory
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository

/**
 * Prépare les demandes système d'ajout aux favoris de la galerie des médias mis en favori pendant
 * le tri (`favorite_pending`, SPEC §3.5). Ces demandes sont distinctes de celles de la corbeille.
 *
 * Tous les favoris en attente sont repris, quelle que soit l'année de revue ; un média en favori
 * pour deux années n'est demandé qu'une fois. Un média qui n'est plus lisible (supprimé, envoyé à
 * la corbeille, hors de la sélection en accès partiel) est laissé de côté : son favori reste en
 * attente, et le nettoyage des orphelins s'en charge s'il a vraiment disparu (SPEC §6).
 *
 * La fabrique découpe les médias en lots : chaque demande sera acceptée ou refusée séparément,
 * puis enregistrée par `RecordFavoriteResult`.
 */
class PrepareFavorites(
    private val reviewRepository: ReviewRepository,
    private val mediaSource: MediaSource,
    private val requestFactory: MediaActionRequestFactory,
) {
    /** Demandes de mise en favori à présenter à l'utilisateur, une par lot ; vide sans favori en attente. */
    suspend operator fun invoke(): List<MediaActionRequest> {
        val favoriteIds = reviewRepository.entriesWithPendingFavorite().mapTo(LinkedHashSet()) { it.mediaId }
        if (favoriteIds.isEmpty()) return emptyList()

        return requestFactory.favoriteRequests(mediaSource.mediaByIds(favoriteIds))
    }
}
