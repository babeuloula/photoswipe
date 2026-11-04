package fr.babeuloula.photoswipe.data.review

import androidx.room.Room
import fr.babeuloula.photoswipe.data.database.PhotoSwipeDatabase
import fr.babeuloula.photoswipe.data.database.ReviewEntryDao
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.TRASHED
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** [RoomReviewRepository] sur une base Room en mémoire (Robolectric). */
@RunWith(RobolectricTestRunner::class)
class RoomReviewRepositoryTest {
    private val database =
        Room
            .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PhotoSwipeDatabase::class.java)
            .build()
    private val repository = RoomReviewRepository(database.reviewEntryDao())

    /** Plus d'identifiants qu'une requête n'en accepte : oblige le dépôt à découper. */
    private val manyIds = (1L..(ReviewEntryDao.MAX_IDS_PER_QUERY * 2L + 1)).toList()

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `saves an entry and reads it back for its review year`() =
        runTest {
            val entry = aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT, favoritePending = true)

            repository.save(entry)

            assertEquals(listOf(entry), repository.entriesFor(2026, setOf(MediaId(1), MediaId(2))))
            assertEquals(emptyList<ReviewEntry>(), repository.entriesFor(2025, setOf(MediaId(1))))
        }

    @Test
    fun `replaces the entry of the same media and review year`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT))
            val updated = aReviewEntry(mediaId = 1, reviewYear = 2026, decision = MARKED_FOR_DELETION)

            repository.save(updated)

            assertEquals(listOf(updated), repository.entriesFor(2026, setOf(MediaId(1))))
        }

    @Test
    fun `deletes only the entry of the given media and review year`() =
        runTest {
            val lastYear = aReviewEntry(mediaId = 1, reviewYear = 2025)
            repository.save(lastYear)
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026))

            repository.delete(MediaId(1), 2026)

            assertEquals(emptyList<ReviewEntry>(), repository.entriesFor(2026, setOf(MediaId(1))))
            assertEquals(listOf(lastYear), repository.entriesFor(2025, setOf(MediaId(1))))
        }

    @Test
    fun `reads more media than a single query accepts`() =
        runTest {
            manyIds.forEach { repository.save(aReviewEntry(mediaId = it, reviewYear = 2026)) }

            val entries = repository.entriesFor(2026, manyIds.mapTo(HashSet()) { MediaId(it) })

            assertEquals(manyIds, entries.map { it.mediaId.value }.sorted())
        }

    @Test
    fun `emits the entries of the observed years again after each write`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026))
            repository.save(aReviewEntry(mediaId = 2, reviewYear = 2024))
            val entries = repository.observeEntries(setOf(2026, 2025))

            assertEquals(listOf(MediaId(1)), entries.first().map { it.mediaId })

            repository.save(aReviewEntry(mediaId = 3, reviewYear = 2025))

            assertEquals(setOf(MediaId(1), MediaId(3)), entries.first { it.size == 2 }.mapTo(HashSet()) { it.mediaId })
        }

    @Test
    fun `lists pending deletions and favorites of every review year`() =
        runTest {
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2025, decision = MARKED_FOR_DELETION))
            repository.save(aReviewEntry(mediaId = 2, reviewYear = 2026, decision = MARKED_FOR_DELETION))
            repository.save(aReviewEntry(mediaId = 3, decision = KEPT, favoritePending = true))
            repository.save(aReviewEntry(mediaId = 4, decision = TRASHED))

            assertEquals(setOf(1L, 2L), repository.entriesMarkedForDeletion().mapTo(HashSet()) { it.mediaId.value })
            assertEquals(listOf(3L), repository.entriesWithPendingFavorite().map { it.mediaId.value })
        }

    @Test
    fun `moves the marked entries of many media to the trash`() =
        runTest {
            manyIds.forEach { repository.save(aReviewEntry(mediaId = it, decision = MARKED_FOR_DELETION)) }
            repository.save(aReviewEntry(mediaId = 9_999, decision = KEPT))

            repository.markTrashed(manyIds.mapTo(HashSet()) { MediaId(it) } + MediaId(9_999))

            assertEquals(emptyList<ReviewEntry>(), repository.entriesMarkedForDeletion())
            assertEquals(KEPT, repository.entriesFor(2026, setOf(MediaId(9_999))).single().decision)
        }

    @Test
    fun `lists known media and deletes every entry of many media`() =
        runTest {
            manyIds.forEach { repository.save(aReviewEntry(mediaId = it, reviewYear = 2025)) }
            repository.save(aReviewEntry(mediaId = 1, reviewYear = 2026))
            repository.save(aReviewEntry(mediaId = 9_999))

            assertEquals(manyIds.size + 1, repository.knownMediaIds().size)

            repository.deleteAllFor(manyIds.mapTo(HashSet()) { MediaId(it) })

            assertEquals(setOf(MediaId(9_999)), repository.knownMediaIds())
        }
}
