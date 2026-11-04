package fr.babeuloula.photoswipe.data.review

import fr.babeuloula.photoswipe.data.database.ReviewEntryDao
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * [ReviewRepository] sur la base Room (SPEC §5.3 et §6).
 *
 * Chaque écriture est terminée quand la fonction rend la main : la décision survit à la fermeture
 * de l'application ou à la mort du process (SPEC §3.4). Les listes d'identifiants sont découpées en
 * lots de [ReviewEntryDao.MAX_IDS_PER_QUERY] pour rester sous la limite de paramètres d'une requête
 * SQLite. Chaque lot est atomique, pas l'ensemble : les écritures découpées (corbeille, purge) sont
 * idempotentes, et un lot interrompu est repris au passage suivant.
 *
 * @param dao accès à la table `review_entry`.
 */
class RoomReviewRepository(
    private val dao: ReviewEntryDao,
) : ReviewRepository {
    override suspend fun entriesFor(
        reviewYear: Int,
        mediaIds: Set<MediaId>,
    ): List<ReviewEntry> =
        mediaIds.inBatches().flatMap { batch -> dao.entriesFor(reviewYear, batch) }.map { it.toDomain() }

    override fun observeEntries(reviewYears: Set<Int>): Flow<List<ReviewEntry>> =
        dao.observeEntries(reviewYears.toList()).map { entities -> entities.map { it.toDomain() } }

    override suspend fun save(entry: ReviewEntry) {
        dao.upsert(entry.toEntity())
    }

    override suspend fun delete(
        mediaId: MediaId,
        reviewYear: Int,
    ) {
        dao.delete(mediaId.value, reviewYear)
    }

    override suspend fun entriesMarkedForDeletion(): List<ReviewEntry> =
        dao.entriesMarkedForDeletion().map {
            it.toDomain()
        }

    override suspend fun entriesWithPendingFavorite(): List<ReviewEntry> =
        dao.entriesWithPendingFavorite().map {
            it.toDomain()
        }

    override suspend fun markTrashed(mediaIds: Set<MediaId>) {
        mediaIds.inBatches().forEach { dao.markTrashed(it) }
    }

    override suspend fun knownMediaIds(): Set<MediaId> = dao.knownMediaIds().mapTo(LinkedHashSet()) { MediaId(it) }

    override suspend fun deleteAllFor(mediaIds: Set<MediaId>) {
        mediaIds.inBatches().forEach { dao.deleteAllFor(it) }
    }

    /** Identifiants bruts de ces médias, en lots acceptés par une requête. */
    private fun Set<MediaId>.inBatches(): List<List<Long>> = map { it.value }.chunked(ReviewEntryDao.MAX_IDS_PER_QUERY)
}
