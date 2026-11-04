package fr.babeuloula.photoswipe.data.database

import androidx.room.Room
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.TRASHED
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** DAO Room sur une base en mémoire, avec le SQLite du framework simulé par Robolectric. */
@RunWith(RobolectricTestRunner::class)
class ReviewEntryDaoTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PhotoSwipeDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    private val dao = database.reviewEntryDao()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `inserts an entry and reads it back for its review year`() =
        runTest {
            val entry = anEntity(mediaId = 1, reviewYear = 2026, favoritePending = true)

            dao.upsert(entry)

            assertEquals(listOf(entry), dao.entriesFor(2026, listOf(1)))
            assertEquals(emptyList<ReviewEntryEntity>(), dao.entriesFor(2025, listOf(1)))
        }

    @Test
    fun `keeps a single entry per media and review year`() =
        runTest {
            dao.upsert(anEntity(mediaId = 1, reviewYear = 2026, decision = KEPT))
            dao.upsert(anEntity(mediaId = 1, reviewYear = 2026, decision = MARKED_FOR_DELETION, decidedAt = 2_000))
            dao.upsert(anEntity(mediaId = 1, reviewYear = 2025))

            assertEquals(
                listOf(anEntity(mediaId = 1, reviewYear = 2026, decision = MARKED_FOR_DELETION, decidedAt = 2_000)),
                dao.entriesFor(2026, listOf(1)),
            )
            assertEquals(listOf(1L), dao.knownMediaIds())
        }

    @Test
    fun `reads only the requested media of a review year`() =
        runTest {
            listOf(1L, 2L, 3L).forEach { dao.upsert(anEntity(mediaId = it)) }

            assertEquals(listOf(1L, 3L), dao.entriesFor(2026, listOf(1, 3, 9)).map { it.mediaId }.sorted())
        }

    @Test
    fun `deletes the entry of one media and review year`() =
        runTest {
            dao.upsert(anEntity(mediaId = 1, reviewYear = 2026))
            dao.upsert(anEntity(mediaId = 1, reviewYear = 2025))

            dao.delete(mediaId = 1, reviewYear = 2026)

            assertEquals(emptyList<ReviewEntryEntity>(), dao.entriesFor(2026, listOf(1)))
            assertEquals(1, dao.entriesFor(2025, listOf(1)).size)
        }

    @Test
    fun `observes the entries of the review years after each write`() =
        runTest {
            dao.upsert(anEntity(mediaId = 1, reviewYear = 2026))
            dao.upsert(anEntity(mediaId = 2, reviewYear = 2024))

            assertEquals(listOf(1L), dao.observeEntries(listOf(2026, 2025)).first().map { it.mediaId })

            dao.upsert(anEntity(mediaId = 3, reviewYear = 2025))

            assertEquals(
                listOf(1L, 3L),
                dao
                    .observeEntries(listOf(2026, 2025))
                    .first()
                    .map { it.mediaId }
                    .sorted(),
            )
        }

    @Test
    fun `lists pending deletions and favorites`() =
        runTest {
            dao.upsert(anEntity(mediaId = 1, decision = MARKED_FOR_DELETION))
            dao.upsert(anEntity(mediaId = 2, decision = KEPT, favoritePending = true))
            dao.upsert(anEntity(mediaId = 3, decision = TRASHED))
            dao.upsert(anEntity(mediaId = 4, decision = KEPT))

            assertEquals(listOf(1L), dao.entriesMarkedForDeletion().map { it.mediaId })
            assertEquals(listOf(2L), dao.entriesWithPendingFavorite().map { it.mediaId })
        }

    @Test
    fun `moves to the trash only the marked entries of the given media`() =
        runTest {
            dao.upsert(anEntity(mediaId = 1, decision = MARKED_FOR_DELETION))
            dao.upsert(anEntity(mediaId = 2, decision = MARKED_FOR_DELETION))
            dao.upsert(anEntity(mediaId = 3, decision = KEPT))

            dao.markTrashed(listOf(1, 3))

            assertEquals(
                listOf(TRASHED, MARKED_FOR_DELETION, KEPT),
                dao.entriesFor(2026, listOf(1, 2, 3)).sortedBy { it.mediaId }.map { it.decision },
            )
        }

    @Test
    fun `deletes every review year of the given media`() =
        runTest {
            dao.upsert(anEntity(mediaId = 1, reviewYear = 2025))
            dao.upsert(anEntity(mediaId = 1, reviewYear = 2026))
            dao.upsert(anEntity(mediaId = 2, reviewYear = 2026))

            dao.deleteAllFor(listOf(1))

            assertEquals(listOf(2L), dao.knownMediaIds())
        }

    private fun anEntity(
        mediaId: Long,
        reviewYear: Int = 2026,
        decision: ReviewDecision = KEPT,
        favoritePending: Boolean = false,
        decidedAt: Long = 1_000,
    ) = ReviewEntryEntity(
        mediaId = mediaId,
        reviewYear = reviewYear,
        decision = decision,
        favoritePending = favoritePending,
        sizeBytes = 2_000_000,
        decidedAt = decidedAt,
    )
}
