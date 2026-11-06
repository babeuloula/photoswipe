package fr.babeuloula.photoswipe.ui.settings

import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeReminderScheduler
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.usecase.UpdateReminderSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalTime

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeSettingsRepository()
    private val scheduler = FakeReminderScheduler()

    private fun settingsViewModel() = SettingsViewModel(repository, UpdateReminderSettings(repository, scheduler))

    @Test
    fun `shows the reminder disabled at 18 00 on a fresh install`() {
        assertEquals(ReminderSettings.DEFAULT, settingsViewModel().uiState.value.reminder)
    }

    @Test
    fun `enables the reminder, saves it and schedules it`() {
        val viewModel = settingsViewModel()

        viewModel.onEvent(SettingsEvent.ReminderToggled(enabled = true))

        assertEquals(ReminderSettings(enabled = true, time = LocalTime.of(18, 0)), repository.reminder)
        assertEquals(LocalTime.of(18, 0), scheduler.scheduledTime)
        assertTrue(
            viewModel.uiState.value.reminder!!
                .enabled,
        )
    }

    @Test
    fun `disables the reminder and cancels it`() =
        runTest {
            repository.saveReminderSettings(ReminderSettings(enabled = true, time = LocalTime.of(9, 0)))
            scheduler.schedule(LocalTime.of(9, 0))
            val viewModel = settingsViewModel()

            viewModel.onEvent(SettingsEvent.ReminderToggled(enabled = false))

            assertFalse(repository.reminder.enabled)
            assertNull(scheduler.scheduledTime)
        }

    @Test
    fun `picks a new time, saves it and reschedules the enabled reminder`() =
        runTest {
            repository.saveReminderSettings(ReminderSettings(enabled = true, time = LocalTime.of(18, 0)))
            val viewModel = settingsViewModel()

            viewModel.onEvent(SettingsEvent.PickReminderTime)
            assertTrue(viewModel.uiState.value.timePickerOpen)
            viewModel.onEvent(SettingsEvent.ReminderTimePicked(LocalTime.of(20, 30)))

            assertFalse(viewModel.uiState.value.timePickerOpen)
            assertEquals(LocalTime.of(20, 30), repository.reminder.time)
            assertEquals(LocalTime.of(20, 30), scheduler.scheduledTime)
        }

    @Test
    fun `keeps the chosen time of a disabled reminder without scheduling it`() {
        val viewModel = settingsViewModel()

        viewModel.onEvent(SettingsEvent.ReminderTimePicked(LocalTime.of(7, 0)))

        assertEquals(ReminderSettings(enabled = false, time = LocalTime.of(7, 0)), repository.reminder)
        assertNull(scheduler.scheduledTime)
    }

    @Test
    fun `closes the time picker without saving`() {
        val viewModel = settingsViewModel()
        viewModel.onEvent(SettingsEvent.PickReminderTime)

        viewModel.onEvent(SettingsEvent.TimePickerDismissed)

        assertFalse(viewModel.uiState.value.timePickerOpen)
        assertEquals(ReminderSettings.DEFAULT, repository.reminder)
    }

    @Test
    fun `goes back with the back arrow`() =
        runTest {
            val viewModel = settingsViewModel()

            viewModel.onEvent(SettingsEvent.Back)

            assertEquals(SettingsEffect.Close, viewModel.uiEffects.first())
        }
}
