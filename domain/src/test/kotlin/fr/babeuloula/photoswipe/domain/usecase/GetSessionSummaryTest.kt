package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class GetSessionSummaryTest {
    private val today = LocalDate.of(2026, 7, 15)
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
    private val repository = FakeReviewRepository()
    private val keepMedia = KeepMedia(repository, clock)
    private val markForDeletion = MarkForDeletion(repository, clock)
    private val favoriteMedia = FavoriteMedia(repository, clock)
    private val getSessionSummary = GetSessionSummary(repository)

    @Test
    fun `is empty for an empty session with nothing pending`() =
        runTest {
            assertEquals(SessionSummary(0, 0, 0, 0), getSessionSummary(ReviewSession()))
        }

    @Test
    fun `counts kept, marked and favorite media of the session`() =
        runTest {
            val session =
                ReviewSession()
                    .record(keepMedia(aMediaItem(id = 1), today))
                    .record(keepMedia(aMediaItem(id = 2), LocalDate.of(2026, 7, 12)))
                    .record(markForDeletion(aMediaItem(id = 3), today))
                    .record(favoriteMedia(aMediaItem(id = 4), today))

            val summary = getSessionSummary(session)

            assertEquals(2, summary.keptCount)
            assertEquals(1, summary.markedForDeletionCount)
            assertEquals(1, summary.favoriteCount)
        }

    @Test
    fun `estimates the freed space as the sum of the sizes of the marked media`() =
        runTest {
            val session =
                ReviewSession()
                    .record(markForDeletion(aMediaItem(id = 1, sizeBytes = 1_500_000), today))
                    .record(markForDeletion(aMediaItem(id = 2, sizeBytes = 25_000_000), today))
                    .record(keepMedia(aMediaItem(id = 3, sizeBytes = 9_000_000), today))

            assertEquals(26_500_000, getSessionSummary(session).estimatedFreedBytes)
        }

    @Test
    fun `includes the deletions and favorites still pending from an earlier session`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(
                        aReviewEntry(mediaId = 1, reviewYear = 2026, decision = MARKED_FOR_DELETION, sizeBytes = 4_000),
                        aReviewEntry(mediaId = 2, reviewYear = 2025, decision = MARKED_FOR_DELETION, sizeBytes = 6_000),
                        aReviewEntry(mediaId = 3, reviewYear = 2026, decision = KEPT, favoritePending = true),
                        aReviewEntry(mediaId = 4, reviewYear = 2026, decision = KEPT),
                    ),
                )

            assertEquals(SessionSummary(0, 2, 1, 10_000), GetSessionSummary(repository)(ReviewSession()))
        }

    @Test
    fun `counts once a media pending for two review years`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(
                        aReviewEntry(mediaId = 1, reviewYear = 2025, decision = MARKED_FOR_DELETION, sizeBytes = 4_000),
                        aReviewEntry(mediaId = 1, reviewYear = 2026, decision = MARKED_FOR_DELETION, sizeBytes = 4_000),
                        aReviewEntry(mediaId = 2, reviewYear = 2025, decision = KEPT, favoritePending = true),
                        aReviewEntry(mediaId = 2, reviewYear = 2026, decision = KEPT, favoritePending = true),
                    ),
                )

            assertEquals(SessionSummary(0, 1, 1, 4_000), GetSessionSummary(repository)(ReviewSession()))
        }

    @Test
    fun `does not count a decision that was undone or replaced`() =
        runTest {
            var session =
                ReviewSession()
                    .record(keepMedia(aMediaItem(id = 1), today))
                    .record(markForDeletion(aMediaItem(id = 1), today))
                    .record(keepMedia(aMediaItem(id = 2), today))
            session = (UndoLastDecision(repository)(session) as UndoLastDecision.Result.Undone).session

            assertEquals(SessionSummary(0, 1, 0, aMediaItem().sizeBytes), getSessionSummary(session))
        }

    @Test
    fun `does not count a confirmed deletion as marked`() =
        runTest {
            val session = ReviewSession().record(markForDeletion(aMediaItem(id = 1), today))
            repository.markTrashed(setOf(aMediaItem(id = 1).id))

            assertEquals(SessionSummary(0, 0, 0, 0), getSessionSummary(session))
        }
}
