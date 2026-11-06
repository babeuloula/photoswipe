package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalTime

/** `FakeSettingsRepository` et `FakeReminderScheduler` respectent le contrat de leurs ports. */
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
    fun `replaces the scheduled reminder and forgets it when cancelled`() {
        val scheduler = FakeReminderScheduler()
        scheduler.schedule(LocalTime.of(18, 0))
        scheduler.schedule(LocalTime.of(9, 15))
        assertEquals(LocalTime.of(9, 15), scheduler.scheduledTime)

        scheduler.cancel()
        scheduler.cancel()

        assertNull(scheduler.scheduledTime)
    }
}
