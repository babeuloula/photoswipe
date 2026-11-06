package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * [ReviewRepository] en mémoire : une entrée au plus par couple (média, année de revue), comme
 * l'index unique de la table Room (SPEC §6).
 *
 * Les résultats suivent l'ordre de première écriture de chaque couple ; le port ne garantit
 * aucun ordre. [entries] permet au test de vérifier ce qui a été enregistré. Les entrées vivent
 * dans un `StateFlow` : [observeEntries] émet après chaque écriture qui change le contenu.
 *
 * @param entries entrées présentes au départ.
 */
class FakeReviewRepository(
    entries: Iterable<ReviewEntry> = emptyList(),
) : ReviewRepository {
    private val stored = MutableStateFlow(entries.associateBy { it.key() })

    /** Toutes les entrées enregistrées, dans l'ordre de première écriture de chaque couple. */
    val entries: List<ReviewEntry> get() = stored.value.values.toList()

    override suspend fun entriesFor(
        reviewYear: Int,
        mediaIds: Set<MediaId>,
    ): List<ReviewEntry> = entries.filter { it.reviewYear == reviewYear && it.mediaId in mediaIds }

    override fun observeEntries(reviewYears: Set<Int>): Flow<List<ReviewEntry>> =
        stored.map { current -> current.values.filter { it.reviewYear in reviewYears } }

    override suspend fun save(entry: ReviewEntry) {
        stored.update { it + (entry.key() to entry) }
    }

    override suspend fun delete(
        mediaId: MediaId,
        reviewYear: Int,
    ) {
        stored.update { it - Key(mediaId, reviewYear) }
    }

    override suspend fun entriesMarkedForDeletion(): List<ReviewEntry> =
        entries.filter { it.decision == ReviewDecision.MARKED_FOR_DELETION && !it.reopened }

    override suspend fun entriesWithPendingFavorite(): List<ReviewEntry> = entries.filter { it.favoritePending }

    override suspend fun markTrashed(mediaIds: Set<MediaId>) {
        stored.update { current ->
            current.mapValues { (_, entry) ->
                if (entry.mediaId in mediaIds && entry.decision == ReviewDecision.MARKED_FOR_DELETION &&
                    !entry.reopened
                ) {
                    entry.copy(decision = ReviewDecision.TRASHED)
                } else {
                    entry
                }
            }
        }
    }

    override suspend fun markFavoritesApplied(mediaIds: Set<MediaId>) {
        stored.update { current ->
            current.mapValues { (_, entry) ->
                if (entry.mediaId in mediaIds) entry.copy(favoritePending = false) else entry
            }
        }
    }

    override suspend fun knownMediaIds(): Set<MediaId> = entries.mapTo(LinkedHashSet()) { it.mediaId }

    override suspend fun deleteAllFor(mediaIds: Set<MediaId>) {
        stored.update { current -> current.filterValues { it.mediaId !in mediaIds } }
    }

    /** Clé de l'index unique (média, année de revue). */
    private data class Key(
        val mediaId: MediaId,
        val reviewYear: Int,
    )

    private fun ReviewEntry.key() = Key(mediaId, reviewYear)
}
