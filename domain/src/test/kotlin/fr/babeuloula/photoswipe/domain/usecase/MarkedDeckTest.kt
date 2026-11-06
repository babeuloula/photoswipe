package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeDecisionJournal
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Pile des médias marqués, ouverte depuis le résumé (PS-112). */
class MarkedDeckTest {
    private val clock = FixedClock.at(LocalDateTime.of(2026, 7, 15, 21, 0))
    private val reviews = FakeReviewRepository()
    private val journal = FakeDecisionJournal(reviews)

    private fun media(
        id: Long,
        takenOn: LocalDate,
    ): MediaItem =
        aMediaItem(id = id, takenAt = LocalDateTime.of(takenOn, LocalTime.NOON).atZone(FixedClock.PARIS).toInstant())

    /** Photo du 15 juillet 2020, marquée le 15 juillet 2025 puis le 15 juillet 2026. */
    private val twice = media(1, LocalDate.of(2020, 7, 15))
    private val once = media(2, LocalDate.of(2022, 7, 14))
    private val leapDay = media(3, LocalDate.of(2024, 2, 29))
    private val gallery = FakeMediaSource(listOf(twice, once, leapDay))

    private fun deck() = GetMarkedDeck(reviews, gallery, SameDayMatcher(), clock)

    private fun review() =
        ReviewMarkedMedia(
            KeepMedia(reviews, journal, FixedClock.at(LocalDateTime.of(2026, 7, 15, 22, 0))),
            MarkForDeletion(reviews, journal, clock),
            FavoriteMedia(reviews, journal, clock),
        )

    private suspend fun mark(
        item: MediaItem,
        day: LocalDate,
        at: LocalDateTime,
    ) = MarkForDeletion(reviews, journal, FixedClock.at(at))(item, day)

    private suspend fun markAll() {
        mark(twice, LocalDate.of(2025, 7, 15), LocalDateTime.of(2025, 7, 15, 10, 0))
        mark(once, LocalDate.of(2026, 7, 14), LocalDateTime.of(2026, 7, 14, 10, 0))
        mark(twice, LocalDate.of(2026, 7, 15), LocalDateTime.of(2026, 7, 15, 10, 0))
        mark(leapDay, LocalDate.of(2025, 2, 28), LocalDateTime.of(2025, 2, 28, 10, 0))
    }

    @Test
    fun `lists each marked media once, the most recently marked first, with all its review days`() =
        runTest {
            markAll()

            val marked = deck()()

            assertEquals(listOf(1L, 2L, 3L), marked.map { it.id.value })
            assertEquals(listOf(LocalDate.of(2026, 7, 15), LocalDate.of(2025, 7, 15)), marked[0].reviewDays)
            assertEquals(listOf(LocalDate.of(2025, 2, 28)), marked[2].reviewDays)
        }

    @Test
    fun `starts with the media that was tapped`() =
        runTest {
            markAll()

            assertEquals(listOf(2L, 1L, 3L), deck()(first = MediaId(2)).map { it.id.value })
        }

    @Test
    fun `leaves out the media no longer in the gallery`() =
        runTest {
            markAll()
            gallery.remove(MediaId(2))

            assertEquals(listOf(1L, 3L), deck()().map { it.id.value })
        }

    @Test
    fun `keeping a media marked for two years withdraws it from the deletions, and undo marks it again`() =
        runTest {
            markAll()
            val marked = deck()().first()

            val decisions = review().keep(marked)

            assertEquals(listOf(2L, 3L), reviews.entriesMarkedForDeletion().map { it.mediaId.value }.sorted())
            assertTrue(reviews.entries.filter { it.mediaId == MediaId(1) }.all { it.decision == KEPT })

            var session = ReviewSession(decisions)
            repeat(decisions.size) {
                session = (UndoLastDecision(reviews, journal)(session) as UndoLastDecision.Result.Undone).session
            }
            assertEquals(
                listOf(MARKED_FOR_DELETION, MARKED_FOR_DELETION),
                reviews.entries.filter { it.mediaId == MediaId(1) }.map { it.decision },
            )
        }

    @Test
    fun `favoriting a media marked for two years keeps it for both, favorite on the latest`() =
        runTest {
            markAll()
            val marked = deck()().first()

            review().favorite(marked)

            val entries = reviews.entries.filter { it.mediaId == MediaId(1) }.sortedByDescending { it.reviewYear }
            assertEquals(listOf(true, false), entries.map { it.favoritePending })
            assertTrue(entries.all { it.decision == KEPT })
        }

    @Test
    fun `leaving a media marked keeps it in the deletions`() =
        runTest {
            markAll()

            review().markForDeletion(deck()().first())

            assertEquals(4, reviews.entriesMarkedForDeletion().size)
        }

    @Test
    fun `shows a February 29 memory on February 28 of a non-leap review year`() {
        assertEquals(LocalDate.of(2025, 2, 28), SameDayMatcher().reviewDay(leapDay, 2025, FixedClock.PARIS))
        assertEquals(LocalDate.of(2028, 2, 29), SameDayMatcher().reviewDay(leapDay, 2028, FixedClock.PARIS))
    }
}
