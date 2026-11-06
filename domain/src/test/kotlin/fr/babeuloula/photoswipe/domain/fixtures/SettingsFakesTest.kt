package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
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
    fun `gives the media management tip as never offered until another state is saved`() =
        runTest {
            val repository = FakeSettingsRepository()
            assertEquals(ManageMediaTip.NOT_OFFERED, repository.observeManageMediaTip().first())

            repository.saveManageMediaTip(ManageMediaTip.OFFERED)

            assertEquals(ManageMediaTip.OFFERED, repository.observeManageMediaTip().first())
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
