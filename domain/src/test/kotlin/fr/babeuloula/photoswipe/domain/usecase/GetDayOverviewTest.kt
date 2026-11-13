package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.DayOverview
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.KEPT
import fr.babeuloula.photoswipe.domain.model.ReviewDecision.MARKED_FOR_DELETION
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
import fr.babeuloula.photoswipe.domain.service.RecentYearsFirstOrdering
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.LocalTime.NOON

class GetDayOverviewTest {
    private val today = LocalDate.of(2026, 7, 15)

    @Test
    fun `is empty when the day has no memory`() =
        runTest {
            assertEquals(DayOverview(today, emptyList()), getDayOverview()(today))
        }

    @Test
    fun `groups every memory of the day by year, most recent year first, in the order of the deck`() =
        runTest {
            val media =
                listOf(
                    takenOn(1, LocalDate.of(2020, 7, 15)),
                    takenOn(2, LocalDate.of(2024, 7, 15), LocalTime.of(18, 0)),
                    takenOn(3, LocalDate.of(2024, 7, 15), LocalTime.of(9, 0)),
                    takenOn(4, LocalDate.of(2024, 7, 14)),
                    takenOn(5, LocalDate.of(2026, 7, 15)),
                )

            val overview = getDayOverview(media)(today)

            assertEquals(listOf(2024, 2020), overview.years.map { it.year })
            assertEquals(listOf(3L, 2L), overview.years[0].memories.map { it.media.id.value })
            assertEquals(3, overview.memoryCount)
        }

    @Test
    fun `gives the decision of the review year, a reopened day counting as to sort`() =
        runTest {
            val media = (1L..4L).map { takenOn(it, LocalDate.of(2024, 7, 15), LocalTime.of(it.toInt(), 0)) }
            val entries =
                listOf(
                    aReviewEntry(mediaId = 1, reviewYear = 2026, decision = KEPT),
                    aReviewEntry(mediaId = 2, reviewYear = 2026, decision = MARKED_FOR_DELETION),
                    aReviewEntry(mediaId = 3, reviewYear = 2026, decision = KEPT, reopened = true),
                    aReviewEntry(mediaId = 4, reviewYear = 2025, decision = KEPT),
                )

            val memories = getDayOverview(media, entries)(today).years.single().memories

            assertEquals(listOf(KEPT, MARKED_FOR_DELETION, null, null), memories.map { it.decision })
        }

    @Test
    fun `shows as favorite a gallery favorite and a favorite still pending`() =
        runTest {
            val media =
                listOf(
                    takenOn(1, LocalDate.of(2024, 7, 15), LocalTime.of(8, 0)).copy(isFavorite = true),
                    takenOn(2, LocalDate.of(2024, 7, 15), LocalTime.of(9, 0)),
                    takenOn(3, LocalDate.of(2024, 7, 15), LocalTime.of(10, 0)),
                )
            val entries = listOf(aReviewEntry(mediaId = 2, reviewYear = 2026, favoritePending = true))

            val memories = getDayOverview(media, entries)(today).years.single().memories

            assertEquals(listOf(true, true, false), memories.map { it.favorite })
        }

    @Test
    fun `marks the photos taken one after another`() =
        runTest {
            val media =
                listOf(
                    takenOn(1, LocalDate.of(2024, 7, 15), LocalTime.of(9, 0, 0)),
                    takenOn(2, LocalDate.of(2024, 7, 15), LocalTime.of(9, 0, 10)),
                    takenOn(3, LocalDate.of(2024, 7, 15), LocalTime.of(15, 0)),
                )

            val memories = getDayOverview(media)(today).years.single().memories

            assertEquals(
                listOf(1 to 2, 2 to 2, null),
                memories.map { memory -> memory.similar?.let { it.position to it.size } },
            )
            assertEquals(memories[0].media.uri, memories[1].similar?.previousUri)
        }

    private fun getDayOverview(
        media: List<MediaItem> = emptyList(),
        entries: List<ReviewEntry> = emptyList(),
    ): GetDayOverview =
        GetDayOverview(
            sameDayMatcher = SameDayMatcher(),
            mediaSource = FakeMediaSource(media),
            reviewRepository = FakeReviewRepository(entries),
            deckOrdering = RecentYearsFirstOrdering(),
            clock = FixedClock.at(today.atTime(NOON)),
        )

    private fun takenOn(
        id: Long,
        date: LocalDate,
        time: LocalTime = NOON,
    ): MediaItem = aMediaItem(id = id, takenAt = LocalDateTime.of(date, time).atZone(FixedClock.PARIS).toInstant())
}
