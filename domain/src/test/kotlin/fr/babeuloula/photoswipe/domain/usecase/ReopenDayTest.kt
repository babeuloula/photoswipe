package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.TRASHED
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.service.RecentYearsFirstOrdering
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Retrier un jour déjà trié (PS-108, ADR 0026) : ses médias reviennent à trier, leurs décisions
 * restent lisibles pour la pile jusqu'à une nouvelle décision.
 */
class ReopenDayTest {
    private val today = LocalDate.of(2026, 7, 15)
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
    private val memories = (1L..4L).map { memory(it) }
    private val gallery = FakeMediaSource(memories + memory(9, LocalDate.of(2025, 7, 14)))
    private val reviews = FakeReviewRepository()
    private val journal = FakeDecisionJournal(reviews)

    private fun memory(
        id: Long,
        date: LocalDate = LocalDate.of(2025, 7, 15),
    ): MediaItem =
        aMediaItem(
            id = id,
            takenAt = LocalDateTime.of(date, LocalTime.of(id.toInt(), 0)).atZone(FixedClock.PARIS).toInstant(),
        )

    private fun reopenDay() = ReopenDay(SameDayMatcher(), gallery, reviews, journal, clock)

    private fun deck() = GetDeckForDay(SameDayMatcher(), gallery, reviews, RecentYearsFirstOrdering(), clock)

    private suspend fun keep(id: Long) = KeepMedia(reviews, journal, clock)(memory(id), today)

    private suspend fun mark(id: Long) = MarkForDeletion(reviews, journal, clock)(memory(id), today)

    private suspend fun favorite(id: Long) = FavoriteMedia(reviews, journal, clock)(memory(id), today)

    /** Jour entièrement trié : 1 gardé, 2 en favori, 3 marqué, 4 déjà envoyé à la corbeille. */
    private suspend fun sortWholeDay() {
        keep(1)
        favorite(2)
        mark(3)
        reviews.save(aReviewEntry(mediaId = 4, reviewYear = 2026, decision = TRASHED))
    }

    @Test
    fun `offers again the kept, favorited and marked media of the day`() =
        runTest {
            sortWholeDay()
            assertEquals(emptyList<MediaItem>(), deck()(today))

            assertEquals(3, reopenDay()(today))

            assertEquals(listOf(1L, 2L, 3L), deck()(today).map { it.id.value }.sorted())
        }

    @Test
    fun `keeps the previous decisions readable for the deck`() =
        runTest {
            sortWholeDay()

            reopenDay()(today)

            val previous = GetPreviousDecisions(reviews)(today, setOf(MediaId(1), MediaId(2), MediaId(3), MediaId(4)))
            assertEquals(KEPT, previous.getValue(MediaId(1)).decision)
            assertTrue(previous.getValue(MediaId(2)).favoritePending)
            assertEquals(MARKED_FOR_DELETION, previous.getValue(MediaId(3)).decision)
            assertFalse(MediaId(4) in previous)
        }

    @Test
    fun `leaves the media sent to the trash and the other days untouched`() =
        runTest {
            sortWholeDay()
            KeepMedia(reviews, journal, clock)(memory(9, LocalDate.of(2025, 7, 14)), today.minusDays(1))

            reopenDay()(today)

            assertEquals(
                listOf(aReviewEntry(mediaId = 4, reviewYear = 2026, decision = TRASHED)),
                reviews.entries.filter { it.mediaId == MediaId(4) },
            )
            assertFalse(reviews.entries.single { it.mediaId == MediaId(9) }.reopened)
        }

    @Test
    fun `withdraws the reopened deletions from the pending ones`() =
        runTest {
            sortWholeDay()

            reopenDay()(today)

            assertEquals(emptyList<Any>(), reviews.entriesMarkedForDeletion())
            assertEquals(
                0,
                GetSessionSummary(reviews, journal, ReviewWindowProvider(clock), clock)().markedForDeletionCount,
            )
        }

    @Test
    fun `forgets the journal of the day, so nothing is left to undo or to count`() =
        runTest {
            sortWholeDay()

            reopenDay()(today)

            assertEquals(ReviewSession(), GetDaySession(journal, reviews, gallery, ReviewWindowProvider(clock))(today))
            val summary = GetSessionSummary(reviews, journal, ReviewWindowProvider(clock), clock)()
            assertEquals(0, summary.keptCount)
            assertEquals(0, summary.favoriteCount)
        }

    @Test
    fun `brings the count of the day back on the home screen`() =
        runTest {
            sortWholeDay()
            val days = GetReviewDays(ReviewWindowProvider(clock), SameDayMatcher(), gallery, reviews, clock)

            reopenDay()(today)

            assertEquals(3, days().first().first { it.date == today }.remainingCount)
        }

    @Test
    fun `replaces the previous decision when the media is sorted again, and restores it on undo`() =
        runTest {
            sortWholeDay()
            reopenDay()(today)

            val decision = keep(3)

            assertEquals(KEPT, reviews.entries.single { it.mediaId == MediaId(3) }.decision)
            assertFalse(MediaId(3) in GetPreviousDecisions(reviews)(today, setOf(MediaId(3))))

            UndoLastDecision(reviews, journal)(ReviewSession(listOf(decision)))

            val restored = reviews.entries.single { it.mediaId == MediaId(3) }
            assertEquals(MARKED_FOR_DELETION, restored.decision)
            assertTrue(restored.reopened)
            assertTrue(deck()(today).any { it.id == MediaId(3) })
        }

    @Test
    fun `reopens nothing on a day without memories`() =
        runTest {
            assertEquals(0, reopenDay()(today.minusDays(3)))
        }
}
