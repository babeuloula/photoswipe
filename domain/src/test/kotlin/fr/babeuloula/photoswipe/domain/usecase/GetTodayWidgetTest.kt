package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeDeletionHistory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aConfirmedDeletion
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.ConfirmedDeletion
import fr.babeuloula.photoswipe.domain.model.MediaAccess
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.model.TodayWidget
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime.NOON

class GetTodayWidgetTest {
    private val today = LocalDate.of(2026, 7, 15)

    private fun getTodayWidget(
        media: List<MediaItem>,
        entries: List<ReviewEntry> = emptyList(),
        deletions: List<ConfirmedDeletion> = emptyList(),
    ): GetTodayWidget {
        val clock = FixedClock.at(today.atTime(NOON))
        return GetTodayWidget(
            GetReviewDays(
                ReviewWindowProvider(clock),
                SameDayMatcher(),
                FakeMediaSource(media),
                FakeReviewRepository(entries),
                FakeDeletionHistory(deletions),
                clock,
            ),
        )
    }

    private fun takenOn(
        id: Long,
        date: LocalDate,
    ): MediaItem = aMediaItem(id = id, takenAt = LocalDateTime.of(date, NOON).atZone(FixedClock.PARIS).toInstant())

    @Test
    fun `invites to open the app without media access`() =
        runTest {
            val widget = getTodayWidget(media = listOf(takenOn(1, LocalDate.of(2025, 7, 15))))

            assertEquals(TodayWidget.NoAccess, widget(MediaAccess.NONE))
        }

    @Test
    fun `shows today's memories left to sort with a preview`() =
        runTest {
            val recent = takenOn(1, LocalDate.of(2025, 7, 15))
            val old = takenOn(2, LocalDate.of(2019, 7, 15))
            val media = listOf(recent, old, takenOn(3, LocalDate.of(2024, 7, 14)))

            val state = getTodayWidget(media)(MediaAccess.FULL)

            assertEquals(TodayWidget.ToSort(today, 2, 2019..2025, recent), state)
        }

    @Test
    fun `previews a memory still to sort before a sorted one`() =
        runTest {
            val sorted = takenOn(1, LocalDate.of(2025, 7, 15))
            val toSort = takenOn(2, LocalDate.of(2019, 7, 15))

            val state =
                getTodayWidget(
                    listOf(sorted, toSort),
                    entries = listOf(aReviewEntry(mediaId = 1, reviewYear = 2026)),
                )(MediaAccess.PARTIAL)

            assertEquals(TodayWidget.ToSort(today, 1, 2019..2025, toSort), state)
        }

    @Test
    fun `says everything is sorted once today's memories are sorted`() =
        runTest {
            val state =
                getTodayWidget(
                    listOf(takenOn(1, LocalDate.of(2025, 7, 15))),
                    entries = listOf(aReviewEntry(mediaId = 1, reviewYear = 2026)),
                )(MediaAccess.FULL)

            assertEquals(TodayWidget.AllSorted(today), state)
        }

    @Test
    fun `counts a day emptied by its deletions as sorted`() =
        runTest {
            val state =
                getTodayWidget(
                    media = emptyList(),
                    deletions = listOf(aConfirmedDeletion(mediaId = 1, day = today)),
                )(MediaAccess.FULL)

            assertEquals(TodayWidget.AllSorted(today), state)
        }

    @Test
    fun `says there is no memory when nothing was taken on this day`() =
        runTest {
            val state = getTodayWidget(listOf(takenOn(1, LocalDate.of(2025, 7, 10))))(MediaAccess.FULL)

            assertEquals(TodayWidget.NoMemories(today), state)
        }
}
