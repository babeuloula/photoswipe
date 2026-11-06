package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class GetSessionSummaryTest {
    private val today = LocalDate.of(2026, 7, 15)
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
    private val repository = FakeReviewRepository()
    private val journal = FakeDecisionJournal(repository)
    private val keepMedia = KeepMedia(repository, journal, clock)
    private val markForDeletion = MarkForDeletion(repository, journal, clock)
    private val favoriteMedia = FavoriteMedia(repository, journal, clock)
    private val getSessionSummary = summaryOf(repository)

    private fun summaryOf(reviews: FakeReviewRepository): GetSessionSummary =
        GetSessionSummary(reviews, journal, ReviewWindowProvider(clock), clock)

    @Test
    fun `is empty when nothing was sorted today and nothing is pending`() =
        runTest {
            assertEquals(SessionSummary(0, 0, 0, 0), getSessionSummary())
        }

    @Test
    fun `counts the media kept and favorited today across every deck of the window`() =
        runTest {
            keepMedia(aMediaItem(id = 1), today)
            keepMedia(aMediaItem(id = 2), LocalDate.of(2026, 7, 12))
            markForDeletion(aMediaItem(id = 3), today)
            favoriteMedia(aMediaItem(id = 4), today)
            favoriteMedia(aMediaItem(id = 5), LocalDate.of(2026, 7, 10))

            val summary = getSessionSummary()

            assertEquals(2, summary.keptCount)
            assertEquals(1, summary.markedForDeletionCount)
            assertEquals(2, summary.favoriteCount)
        }

    @Test
    fun `keeps counting a favorite once the gallery applied it`() =
        runTest {
            favoriteMedia(aMediaItem(id = 1), today)

            repository.markFavoritesApplied(setOf(aMediaItem(id = 1).id))

            assertEquals(1, getSessionSummary().favoriteCount)
        }

    @Test
    fun `does not count what was sorted on an earlier day`() =
        runTest {
            clock.advanceBy(Duration.ofDays(-1))
            keepMedia(aMediaItem(id = 1), today.minusDays(1))
            favoriteMedia(aMediaItem(id = 2), today.minusDays(1))
            clock.advanceBy(Duration.ofDays(1))

            val summary = getSessionSummary()

            assertEquals(0, summary.keptCount)
            assertEquals(0, summary.favoriteCount)
        }

    @Test
    fun `estimates the freed space as the sum of the sizes of the marked media`() =
        runTest {
            markForDeletion(aMediaItem(id = 1, sizeBytes = 1_500_000), today)
            markForDeletion(aMediaItem(id = 2, sizeBytes = 25_000_000), today)
            keepMedia(aMediaItem(id = 3, sizeBytes = 9_000_000), today)

            assertEquals(26_500_000, getSessionSummary().estimatedFreedBytes)
        }

    @Test
    fun `includes the deletions still pending from an earlier session`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(
                        aReviewEntry(mediaId = 1, reviewYear = 2026, decision = MARKED_FOR_DELETION, sizeBytes = 4_000),
                        aReviewEntry(mediaId = 2, reviewYear = 2025, decision = MARKED_FOR_DELETION, sizeBytes = 6_000),
                        aReviewEntry(mediaId = 3, reviewYear = 2026, decision = KEPT, favoritePending = true),
                    ),
                )

            assertEquals(SessionSummary(0, 2, 0, 10_000), summaryOf(repository)())
        }

    @Test
    fun `counts once a media marked for two review years`() =
        runTest {
            val repository =
                FakeReviewRepository(
                    listOf(
                        aReviewEntry(mediaId = 1, reviewYear = 2025, decision = MARKED_FOR_DELETION, sizeBytes = 4_000),
                        aReviewEntry(mediaId = 1, reviewYear = 2026, decision = MARKED_FOR_DELETION, sizeBytes = 4_000),
                    ),
                )

            assertEquals(SessionSummary(0, 1, 0, 4_000), summaryOf(repository)())
        }

    @Test
    fun `does not count a decision that was undone`() =
        runTest {
            var session =
                ReviewSession()
                    .record(keepMedia(aMediaItem(id = 1), today))
                    .record(favoriteMedia(aMediaItem(id = 2), today))
            session = (UndoLastDecision(repository, journal)(session) as UndoLastDecision.Result.Undone).session

            assertEquals(SessionSummary(1, 0, 0, 0), getSessionSummary())
        }

    @Test
    fun `does not count a confirmed deletion as marked`() =
        runTest {
            markForDeletion(aMediaItem(id = 1), today)
            repository.markTrashed(setOf(aMediaItem(id = 1).id))

            assertEquals(SessionSummary(0, 0, 0, 0), getSessionSummary())
        }
}
