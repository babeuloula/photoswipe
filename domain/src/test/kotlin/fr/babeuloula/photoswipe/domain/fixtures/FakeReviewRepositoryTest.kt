package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.TRASHED
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FakeReviewRepositoryTest {
    @Test
    fun `returns the entries of one review year for the requested media only`() =
        runTest {
            val wanted = aReviewEntry(mediaId = 1, reviewYear = 2026)
            val repository =
                FakeReviewRepository(
                    listOf(
                        wanted,
                        aReviewEntry(mediaId = 1, reviewYear = 2025),
                        aReviewEntry(mediaId = 2, reviewYear = 2026),
                    ),
                )

            assertEquals(listOf(wanted), repository.entriesFor(2026, setOf(MediaId(1), MediaId(3))))
        }

    @Test
    fun `replaces the entry of the same media and year without touching other years`() =
        runTest {
            val previousYear = aReviewEntry(mediaId = 1, reviewYear = 2025)
            val repository = FakeReviewRepository(listOf(previousYear, aReviewEntry(mediaId = 1, reviewYear = 2026)))
            val marked = aReviewEntry(mediaId = 1, reviewYear = 2026, decision = MARKED_FOR_DELETION)

            repository.save(marked)

            assertEquals(listOf(previousYear, marked), repository.entries)
        }

    @Test
    fun `deletes only the entry of the given media and year`() =
        runTest {
            val previousYear = aReviewEntry(mediaId = 1, reviewYear = 2025)
            val repository = FakeReviewRepository(listOf(previousYear, aReviewEntry(mediaId = 1, reviewYear = 2026)))

            repository.delete(MediaId(1), 2026)

            assertEquals(listOf(previousYear), repository.entries)
        }

    @Test
    fun `lists entries marked for deletion and pending favorites across years`() =
        runTest {
            val marked2025 = aReviewEntry(mediaId = 1, reviewYear = 2025, decision = MARKED_FOR_DELETION)
            val marked2026 = aReviewEntry(mediaId = 2, reviewYear = 2026, decision = MARKED_FOR_DELETION)
            val favorite = aReviewEntry(mediaId = 3, favoritePending = true)
            val repository =
                FakeReviewRepository(
                    listOf(marked2025, marked2026, favorite, aReviewEntry(mediaId = 4, decision = TRASHED)),
                )

            assertEquals(listOf(marked2025, marked2026), repository.entriesMarkedForDeletion())
            assertEquals(listOf(favorite), repository.entriesWithPendingFavorite())
        }

    @Test
    fun `trashes only the marked entries of the given media, in every year`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(
                        aReviewEntry(mediaId = 1, reviewYear = 2025, decision = MARKED_FOR_DELETION),
                        aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT),
                        aReviewEntry(mediaId = 2, reviewYear = 2026, decision = MARKED_FOR_DELETION),
                    ),
                )

            repository.markTrashed(setOf(MediaId(1)))

            assertEquals(
                listOf(
                    aReviewEntry(mediaId = 1, reviewYear = 2025, decision = TRASHED),
                    aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT),
                    aReviewEntry(mediaId = 2, reviewYear = 2026, decision = MARKED_FOR_DELETION),
                ),
                repository.entries,
            )
        }

    @Test
    fun `knows every media with at least one entry`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(
                        aReviewEntry(mediaId = 1, reviewYear = 2025),
                        aReviewEntry(mediaId = 1, reviewYear = 2026),
                        aReviewEntry(mediaId = 2),
                    ),
                )

            assertEquals(setOf(MediaId(1), MediaId(2)), repository.knownMediaIds())
        }

    @Test
    fun `deletes every entry of the given media whatever the year`() =
        runTest {
            val kept = aReviewEntry(mediaId = 2)
            val repository =
                FakeReviewRepository(
                    listOf(
                        aReviewEntry(mediaId = 1, reviewYear = 2025),
                        aReviewEntry(mediaId = 1, reviewYear = 2026),
                        kept,
                    ),
                )

            repository.deleteAllFor(setOf(MediaId(1)))

            assertEquals(listOf(kept), repository.entries)
        }
}
