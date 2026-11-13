package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.model.RatingInvitation
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import fr.babeuloula.photoswipe.domain.model.Tutorial
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** `FakeSettingsRepository`, `FakeReminderScheduler` et `FakeReminderNotifier` respectent le contrat de leurs ports. */
class SettingsFakesTest {
    @Test
    fun `gives the default reminder settings until something is saved, then the saved ones`() =
        runTest {
            val repository = FakeSettingsRepository()
            assertEquals(ReminderSettings.DEFAULT, repository.reminderSettings())

            val saved = ReminderSettings(enabled = true, time = LocalTime.of(20, 30))
            repository.saveReminderSettings(saved)

            assertEquals(saved, repository.reminderSettings())
            assertEquals(saved, repository.observeReminderSettings().first())
        }

    @Test
    fun `gives the phone theme until another theme is saved, then the saved one`() =
        runTest {
            val repository = FakeSettingsRepository()
            assertEquals(ThemeMode.SYSTEM, repository.observeThemeMode().first())

            repository.saveThemeMode(ThemeMode.DARK)

            assertEquals(ThemeMode.DARK, repository.observeThemeMode().first())
            assertEquals(ThemeMode.DARK, repository.themeMode)
        }

    @Test
    fun `keeps the animations on until they are turned off`() =
        runTest {
            val repository = FakeSettingsRepository()
            assertEquals(true, repository.observeAnimationsEnabled().first())

            repository.saveAnimationsEnabled(false)

            assertEquals(false, repository.observeAnimationsEnabled().first())
            assertEquals(false, repository.animationsEnabled)
        }

    @Test
    fun `keeps the dark theme in grays until pure black is turned on`() =
        runTest {
            val repository = FakeSettingsRepository()
            assertEquals(false, repository.observePureBlack().first())

            repository.savePureBlack(true)

            assertEquals(true, repository.observePureBlack().first())
            assertEquals(true, repository.pureBlack)
        }

    @Test
    fun `gives the media management tip as never offered until another state is saved`() =
        runTest {
            val repository = FakeSettingsRepository()
            assertEquals(ManageMediaTip.NOT_OFFERED, repository.observeManageMediaTip().first())

            repository.saveManageMediaTip(ManageMediaTip.OFFERED)

            assertEquals(ManageMediaTip.OFFERED, repository.observeManageMediaTip().first())
        }

    @Test
    fun `gives the tutorial as never seen until a version is saved`() =
        runTest {
            val repository = FakeSettingsRepository()
            assertEquals(Tutorial.NEVER_SEEN, repository.tutorialVersionSeen())

            repository.saveTutorialVersionSeen(2)

            assertEquals(2, repository.tutorialVersionSeen())
        }

    @Test
    fun `stores the rating invitation, nothing sorted by default`() =
        runTest {
            val repository = FakeSettingsRepository()
            assertEquals(RatingInvitation.DEFAULT, repository.ratingInvitation())

            val saved = RatingInvitation(sortedDays = 1, lastSortedDay = LocalDate.of(2026, 11, 7))
            repository.saveRatingInvitation(saved)

            assertEquals(saved, repository.ratingInvitation())
        }

    @Test
    fun `replaces the scheduled reminder and forgets it when cancelled`() {
        val scheduler = FakeReminderScheduler()
        scheduler.schedule(LocalTime.of(18, 0))
        scheduler.schedule(LocalTime.of(9, 15))
        assertEquals(LocalTime.of(9, 15), scheduler.scheduledTime)

        scheduler.cancel()
        scheduler.cancel()

        assertNull(scheduler.scheduledTime)
    }

    @Test
    fun `keeps the one-off reminder beside the daily one and cancels both`() {
        val scheduler = FakeReminderScheduler()
        scheduler.schedule(LocalTime.of(18, 0))
        scheduler.scheduleOnce(LocalDateTime.of(2026, 11, 7, 20, 0))
        scheduler.scheduleOnce(LocalDateTime.of(2026, 11, 7, 21, 30))

        assertEquals(LocalTime.of(18, 0), scheduler.scheduledTime)
        assertEquals(LocalDateTime.of(2026, 11, 7, 21, 30), scheduler.onceAt)

        scheduler.cancel()

        assertNull(scheduler.onceAt)
    }

    @Test
    fun `remembers each reminder notification and refuses an empty one`() {
        val notifier = FakeReminderNotifier()

        val day = LocalDate.of(2026, 7, 15)
        notifier.notify(3, day)
        notifier.notify(1, day.minusDays(1))

        assertEquals(
            listOf(FakeReminderNotifier.Sent(3, day), FakeReminderNotifier.Sent(1, day.minusDays(1))),
            notifier.sent,
        )
        assertThrows<IllegalArgumentException> { notifier.notify(0, day) }
    }
}
