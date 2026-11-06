package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReminderNotifier
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime.NOON

class SendDailyReminderTest {
    private val today = LocalDate.of(2026, 7, 15)
    private val notifier = FakeReminderNotifier()

    private fun sendDailyReminder(
        media: List<MediaItem>,
        entries: List<ReviewEntry> = emptyList(),
    ): SendDailyReminder {
        val clock = FixedClock.at(today.atTime(NOON))
        val getReviewDays =
            GetReviewDays(
                ReviewWindowProvider(clock),
                SameDayMatcher(),
                FakeMediaSource(media),
                FakeReviewRepository(entries),
                clock,
            )
        return SendDailyReminder(getReviewDays, notifier)
    }

    private fun takenOn(
        id: Long,
        date: LocalDate,
    ): MediaItem = aMediaItem(id = id, takenAt = LocalDateTime.of(date, NOON).atZone(FixedClock.PARIS).toInstant())

    @Test
    fun `sends nothing when no media is left to sort`() =
        runTest {
            val sent = sendDailyReminder(media = emptyList())()

            assertEquals(0, sent)
            assertEquals(emptyList<Int>(), notifier.sent)
        }

    @Test
    fun `sends nothing when every memory of the window is already sorted`() =
        runTest {
            val media = listOf(takenOn(1, LocalDate.of(2025, 7, 15)))

            sendDailyReminder(media, entries = listOf(aReviewEntry(mediaId = 1, reviewYear = 2026)))()

            assertEquals(emptyList<Int>(), notifier.sent)
        }

    @Test
    fun `announces every media left to sort on the whole window`() =
        runTest {
            val media =
                listOf(
                    takenOn(1, LocalDate.of(2025, 7, 15)),
                    takenOn(2, LocalDate.of(2020, 7, 15)),
                    takenOn(3, LocalDate.of(2024, 7, 10)),
                    takenOn(4, LocalDate.of(2024, 7, 1)),
                )

            val sent = sendDailyReminder(media)()

            assertEquals(3, sent)
            assertEquals(listOf(3), notifier.sent)
        }
}
