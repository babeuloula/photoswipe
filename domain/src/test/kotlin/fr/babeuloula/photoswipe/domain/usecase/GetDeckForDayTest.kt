package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.TRASHED
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.service.DeckOrdering
import fr.babeuloula.photoswipe.domain.service.RecentYearsFirstOrdering
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.LocalTime.NOON

class GetDeckForDayTest {
    private val today = LocalDate.of(2026, 7, 15)

    @Test
    fun `returns an empty deck when the day has no memory`() =
        runTest {
            assertEquals(emptyList<MediaItem>(), getDeckForDay()(today))
        }

    @Test
    fun `orders the memories of several years from the most recent year`() =
        runTest {
            val media =
                listOf(
                    takenOn(id = 1, LocalDate.of(2019, 7, 15), LocalTime.of(9, 0)),
                    takenOn(id = 2, LocalDate.of(2025, 7, 15), LocalTime.of(20, 0)),
                    takenOn(id = 3, LocalDate.of(2022, 7, 15), LocalTime.of(10, 0)),
                    takenOn(id = 4, LocalDate.of(2025, 7, 15), LocalTime.of(7, 30)),
                    takenOn(id = 5, LocalDate.of(2025, 7, 14), LocalTime.of(7, 30)),
                    takenOn(id = 6, LocalDate.of(2026, 7, 15), LocalTime.of(7, 30)),
                )

            assertEquals(listOf(4L, 2L, 3L, 1L), ids(getDeckForDay(media)(today)))
        }

    @Test
    fun `leaves out media already decided for the review year whatever the decision`() =
        runTest {
            val media = (1L..4L).map { takenOn(id = it, LocalDate.of(2025, 7, 15), LocalTime.of(it.toInt(), 0)) }
            val entries =
                listOf(
                    aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT),
                    aReviewEntry(mediaId = 2, reviewYear = 2026, decision = MARKED_FOR_DELETION),
                    aReviewEntry(mediaId = 3, reviewYear = 2026, decision = TRASHED),
                )

            assertEquals(listOf(4L), ids(getDeckForDay(media, entries)(today)))
        }

    @Test
    fun `offers again the media of a reopened day`() =
        runTest {
            val media = (1L..3L).map { takenOn(id = it, LocalDate.of(2025, 7, 15), LocalTime.of(it.toInt(), 0)) }
            val entries =
                listOf(
                    aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT, reopened = true),
                    aReviewEntry(mediaId = 2, reviewYear = 2026, decision = MARKED_FOR_DELETION, reopened = true),
                    aReviewEntry(mediaId = 3, reviewYear = 2026, decision = KEPT),
                )

            assertEquals(listOf(1L, 2L), ids(getDeckForDay(media, entries)(today)))
        }

    @Test
    fun `offers again a media kept the previous year`() =
        runTest {
            val media = listOf(takenOn(id = 1, LocalDate.of(2024, 7, 15)))
            val entries = listOf(aReviewEntry(mediaId = 1, reviewYear = 2025, decision = KEPT))

            assertEquals(listOf(1L), ids(getDeckForDay(media, entries)(today)))
        }

    @Test
    fun `includes February 29 memories in the February 28 deck of a non-leap year`() =
        runTest {
            val media =
                listOf(
                    takenOn(id = 1, LocalDate.of(2024, 2, 29), LocalTime.of(8, 0)),
                    takenOn(id = 2, LocalDate.of(2024, 2, 28), LocalTime.of(20, 0)),
                    takenOn(id = 3, LocalDate.of(2025, 2, 28)),
                )

            assertEquals(listOf(3L, 2L, 1L), ids(getDeckForDay(media)(LocalDate.of(2027, 2, 28))))
        }

    @Test
    fun `returns the media in the order chosen by the injected strategy`() =
        runTest {
            val media =
                listOf(
                    takenOn(id = 1, LocalDate.of(2020, 7, 15)),
                    takenOn(id = 2, LocalDate.of(2025, 7, 15)),
                    takenOn(id = 3, LocalDate.of(2023, 7, 15)),
                )
            val oldestFirst = DeckOrdering { deck, _ -> deck.sortedBy { it.takenAt } }

            assertEquals(listOf(1L, 3L, 2L), ids(getDeckForDay(media, ordering = oldestFirst)(today)))
        }

    @Test
    fun `holds as many media as the remaining count shown for each day of the window`() =
        runTest {
            val media =
                listOf(
                    takenOn(id = 1, LocalDate.of(2025, 7, 15)),
                    takenOn(id = 2, LocalDate.of(2024, 7, 15)),
                    takenOn(id = 3, LocalDate.of(2023, 7, 14)),
                    takenOn(id = 4, LocalDate.of(2022, 7, 12)),
                    takenOn(id = 5, LocalDate.of(2021, 7, 12)),
                )
            val entries =
                listOf(aReviewEntry(mediaId = 1, reviewYear = 2026), aReviewEntry(mediaId = 5, reviewYear = 2025))
            val clock = FixedClock.at(today.atTime(NOON))
            val repository = FakeReviewRepository(entries)
            val reviewDays =
                GetReviewDays(
                    ReviewWindowProvider(clock),
                    SameDayMatcher(),
                    FakeMediaSource(media),
                    repository,
                    clock,
                )()
                    .first()

            reviewDays.forEach { day ->
                assertEquals(day.remainingCount, getDeckForDay(media, entries)(day.date).size, "deck of ${day.date}")
            }
        }

    private fun getDeckForDay(
        media: List<MediaItem> = emptyList(),
        entries: List<ReviewEntry> = emptyList(),
        ordering: DeckOrdering = RecentYearsFirstOrdering(),
    ): GetDeckForDay =
        GetDeckForDay(
            sameDayMatcher = SameDayMatcher(),
            mediaSource = FakeMediaSource(media),
            reviewRepository = FakeReviewRepository(entries),
            deckOrdering = ordering,
            clock = FixedClock.at(today.atTime(NOON)),
        )

    private fun takenOn(
        id: Long,
        date: LocalDate,
        time: LocalTime = NOON,
    ): MediaItem = aMediaItem(id = id, takenAt = LocalDateTime.of(date, time).atZone(FixedClock.PARIS).toInstant())

    private fun ids(media: List<MediaItem>): List<Long> = media.map { it.id.value }
}
