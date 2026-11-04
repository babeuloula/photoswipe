package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewEntry

/**
 * Stockage des décisions, une par couple (média, année de revue) (SPEC §3.3 et §6). Implémenté
 * dans `:data` sur Room.
 *
 * Chaque écriture est persistée avant de rendre la main, pour survivre à la mort du process
 * (SPEC §3.4).
 */
interface ReviewRepository {
    /** Entrées de l'année de revue [reviewYear] pour les médias [mediaIds] : les autres sont à trier. */
    suspend fun entriesFor(
        reviewYear: Int,
        mediaIds: Set<MediaId>,
    ): List<ReviewEntry>

    /** Enregistre [entry], en remplaçant l'éventuelle entrée du même couple (média, année de revue). */
    suspend fun save(entry: ReviewEntry)

    /** Supprime l'entrée du couple ([mediaId], [reviewYear]) : le média redevient à trier (undo). */
    suspend fun delete(
        mediaId: MediaId,
        reviewYear: Int,
    )

    /** Entrées marquées pour suppression et pas encore confirmées, toutes années confondues. */
    suspend fun entriesMarkedForDeletion(): List<ReviewEntry>

    /** Entrées dont le favori système est demandé mais pas encore appliqué. */
    suspend fun entriesWithPendingFavorite(): List<ReviewEntry>

    /** Passe à `TRASHED` les entrées marquées pour suppression des médias [mediaIds]. */
    suspend fun markTrashed(mediaIds: Set<MediaId>)

    /** Identifiants de tous les médias qui ont au moins une entrée. */
    suspend fun knownMediaIds(): Set<MediaId>

    /** Supprime toutes les entrées des médias [mediaIds], quelle que soit l'année (orphelins). */
    suspend fun deleteAllFor(mediaIds: Set<MediaId>)
}
