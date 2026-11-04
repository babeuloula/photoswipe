package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.port.ReviewRepository

/**
 * [ReviewRepository] en mémoire : une entrée au plus par couple (média, année de revue), comme
 * l'index unique de la table Room (SPEC §6).
 *
 * Les résultats suivent l'ordre de première écriture de chaque couple ; le port ne garantit
 * aucun ordre. [entries] permet au test de vérifier ce qui a été enregistré.
 *
 * @param entries entrées présentes au départ.
 */
class FakeReviewRepository(
    entries: Iterable<ReviewEntry> = emptyList(),
) : ReviewRepository {
    private val stored = LinkedHashMap<Key, ReviewEntry>()

    init {
        entries.forEach { stored[it.key()] = it }
    }

    /** Toutes les entrées enregistrées, dans l'ordre de première écriture de chaque couple. */
    val entries: List<ReviewEntry> get() = stored.values.toList()

    override suspend fun entriesFor(
        reviewYear: Int,
        mediaIds: Set<MediaId>,
    ): List<ReviewEntry> = stored.values.filter { it.reviewYear == reviewYear && it.mediaId in mediaIds }

    override suspend fun save(entry: ReviewEntry) {
        stored[entry.key()] = entry
    }

    override suspend fun delete(
        mediaId: MediaId,
        reviewYear: Int,
    ) {
        stored.remove(Key(mediaId, reviewYear))
    }

    override suspend fun entriesMarkedForDeletion(): List<ReviewEntry> =
        stored.values.filter { it.decision == ReviewDecision.MARKED_FOR_DELETION }

    override suspend fun entriesWithPendingFavorite(): List<ReviewEntry> = stored.values.filter { it.favoritePending }

    override suspend fun markTrashed(mediaIds: Set<MediaId>) {
        stored.replaceAll { _, entry ->
            if (entry.mediaId in mediaIds && entry.decision == ReviewDecision.MARKED_FOR_DELETION) {
                entry.copy(decision = ReviewDecision.TRASHED)
            } else {
                entry
            }
        }
    }

    override suspend fun knownMediaIds(): Set<MediaId> = stored.values.mapTo(LinkedHashSet()) { it.mediaId }

    override suspend fun deleteAllFor(mediaIds: Set<MediaId>) {
        stored.values.removeAll { it.mediaId in mediaIds }
    }

    /** Clé de l'index unique (média, année de revue). */
    private data class Key(
        val mediaId: MediaId,
        val reviewYear: Int,
    )

    private fun ReviewEntry.key() = Key(mediaId, reviewYear)
}
