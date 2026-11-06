package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeReminderScheduler
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalTime

class UpdateReminderSettingsTest {
    private val repository = FakeSettingsRepository()
    private val scheduler = FakeReminderScheduler()
    private val update = UpdateReminderSettings(repository, scheduler)

    @Test
    fun `saves an enabled reminder and schedules it at its time`() =
        runTest {
            val settings = ReminderSettings(enabled = true, time = LocalTime.of(18, 0))

            update(settings)

            assertEquals(settings, repository.reminder)
            assertEquals(LocalTime.of(18, 0), scheduler.scheduledTime)
        }

    @Test
    fun `reschedules the reminder when its time changes`() =
        runTest {
            update(ReminderSettings(enabled = true, time = LocalTime.of(18, 0)))

            update(ReminderSettings(enabled = true, time = LocalTime.of(7, 45)))

            assertEquals(LocalTime.of(7, 45), scheduler.scheduledTime)
        }

    @Test
    fun `cancels the reminder when it is disabled and keeps its time`() =
        runTest {
            update(ReminderSettings(enabled = true, time = LocalTime.of(21, 0)))

            update(ReminderSettings(enabled = false, time = LocalTime.of(21, 0)))

            assertNull(scheduler.scheduledTime)
            assertEquals(ReminderSettings(enabled = false, time = LocalTime.of(21, 0)), repository.reminder)
        }
}
