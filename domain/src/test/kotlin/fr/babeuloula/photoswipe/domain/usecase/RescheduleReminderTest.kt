package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeReminderScheduler
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalTime

class RescheduleReminderTest {
    private val scheduler = FakeReminderScheduler()

    @Test
    fun `schedules an enabled reminder again at its saved time`() =
        runTest {
            val repository = FakeSettingsRepository(ReminderSettings(enabled = true, time = LocalTime.of(20, 15)))

            RescheduleReminder(repository, scheduler)()

            assertEquals(LocalTime.of(20, 15), scheduler.scheduledTime)
        }

    @Test
    fun `cancels a disabled reminder`() =
        runTest {
            scheduler.schedule(LocalTime.of(18, 0))

            RescheduleReminder(FakeSettingsRepository(), scheduler)()

            assertNull(scheduler.scheduledTime)
        }
}
