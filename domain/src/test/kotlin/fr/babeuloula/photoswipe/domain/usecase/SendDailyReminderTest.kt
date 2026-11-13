package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeDeletionHistory
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeReminderNotifier
import fr.babeuloula.photoswipe.domain.fixtures.FakeReviewRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FixedClock
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aReviewEntry
import fr.babeuloula.photoswipe.domain.model.CatchUpWindow
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.service.ReviewWindowProvider
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime.NOON

class SendDailyReminderTest {
    private val today = LocalDate.of(2026, 7, 15)
    private val notifier = FakeReminderNotifier()

    private fun sendDailyReminder(
        media: List<MediaItem>,
        entries: List<ReviewEntry> = emptyList(),
        window: CatchUpWindow = CatchUpWindow.DEFAULT,
    ): SendDailyReminder {
        val clock = FixedClock.at(today.atTime(NOON))
        val getReviewDays =
            GetReviewDays(
                ReviewWindowProvider(clock, FakeSettingsRepository(catchUpWindow = window)),
                SameDayMatcher(),
                FakeMediaSource(media),
                FakeReviewRepository(entries),
                FakeDeletionHistory(),
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
            assertEquals(emptyList<FakeReminderNotifier.Sent>(), notifier.sent)
        }

    @Test
    fun `sends nothing when every memory of the window is already sorted`() =
        runTest {
            val media = listOf(takenOn(1, LocalDate.of(2025, 7, 15)))

            sendDailyReminder(media, entries = listOf(aReviewEntry(mediaId = 1, reviewYear = 2026)))()

            assertEquals(emptyList<FakeReminderNotifier.Sent>(), notifier.sent)
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
            val notification = notifier.sent.single()
            assertEquals(3, notification.toSortCount)
            assertEquals(today, notification.dayToSort)
            // Le texte cite les années des souvenirs du jour, et la vignette est le plus récent.
            assertEquals(2020..2025, notification.years)
            assertEquals(MediaId(1), notification.preview?.id)
        }

    @Test
    fun `opens the most recent day left to sort when today is done`() =
        runTest {
            val media = listOf(takenOn(1, LocalDate.of(2024, 7, 12)), takenOn(2, LocalDate.of(2023, 7, 10)))

            sendDailyReminder(media)()

            assertEquals(listOf(2 to LocalDate.of(2026, 7, 12)), notifier.sent.map { it.toSortCount to it.dayToSort })
        }

    @ParameterizedTest
    @EnumSource(CatchUpWindow::class)
    fun `counts the media left to sort on the chosen catch-up window`(window: CatchUpWindow) =
        runTest {
            // Un souvenir à la limite de chaque durée : le dernier jour de la fenêtre de 7, 14 et 30 jours.
            val media =
                CatchUpWindow.entries.mapIndexed { index, each ->
                    takenOn(index + 1L, today.minusDays(each.days - 1L).minusYears(1))
                }

            val sent = sendDailyReminder(media, window = window)()

            assertEquals(CatchUpWindow.entries.count { it.days <= window.days }, sent)
        }
}
