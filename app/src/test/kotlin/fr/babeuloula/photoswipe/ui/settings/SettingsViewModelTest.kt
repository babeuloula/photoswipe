package fr.babeuloula.photoswipe.ui.settings

import fr.babeuloula.photoswipe.MainDispatcherRule
import fr.babeuloula.photoswipe.domain.fixtures.FakeReminderScheduler
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.FakeUsageStatisticsRepository
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import fr.babeuloula.photoswipe.domain.usecase.GetUsageStatistics
import fr.babeuloula.photoswipe.domain.usecase.UpdateReminderSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class SettingsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeSettingsRepository()
    private val scheduler = FakeReminderScheduler()

    /** Permission de notification simulée, accordée par défaut. */
    private var notificationsAllowed = true

    private val statistics = FakeUsageStatisticsRepository(LocalDate.of(2026, 3, 2))

    private fun settingsViewModel() =
        SettingsViewModel(
            settingsRepository = repository,
            updateReminderSettings = UpdateReminderSettings(repository, scheduler),
            notificationPermission = { notificationsAllowed },
            developerTools = { null },
            getUsageStatistics = GetUsageStatistics(statistics),
            exactAlarms = { true },
        )

    private fun TestScope.collectEffects(viewModel: SettingsViewModel): List<SettingsEffect> {
        val effects = mutableListOf<SettingsEffect>()
        backgroundScope.launch(
            UnconfinedTestDispatcher(testScheduler),
        ) { viewModel.uiEffects.collect { effects += it } }
        return effects
    }

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
    fun `asks for the notification permission before enabling the reminder`() =
        runTest {
            notificationsAllowed = false
            val viewModel = settingsViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SettingsEvent.ReminderToggled(enabled = true))

            assertEquals(listOf<SettingsEffect>(SettingsEffect.RequestNotificationPermission), effects)
            assertFalse(repository.reminder.enabled)
            assertNull(scheduler.scheduledTime)
        }

    @Test
    fun `enables the reminder once the notification permission is granted`() {
        notificationsAllowed = false
        val viewModel = settingsViewModel()
        viewModel.onEvent(SettingsEvent.ReminderToggled(enabled = true))

        viewModel.onEvent(SettingsEvent.NotificationPermissionAnswered(granted = true, canAskAgain = false))

        assertTrue(repository.reminder.enabled)
        assertEquals(LocalTime.of(18, 0), scheduler.scheduledTime)
    }

    @Test
    fun `keeps the reminder off and explains when notifications are refused`() =
        runTest {
            notificationsAllowed = false
            val viewModel = settingsViewModel()
            val effects = collectEffects(viewModel)
            viewModel.onEvent(SettingsEvent.ReminderToggled(enabled = true))

            viewModel.onEvent(SettingsEvent.NotificationPermissionAnswered(granted = false, canAskAgain = true))

            assertEquals(SettingsEffect.NotificationPermissionDenied(canAskAgain = true), effects.last())
            assertFalse(
                viewModel.uiState.value.reminder!!
                    .enabled,
            )
            assertNull(scheduler.scheduledTime)
        }

    @Test
    fun `opens the notification settings when Android will not ask again`() =
        runTest {
            val viewModel = settingsViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SettingsEvent.NotificationPermissionAnswered(granted = false, canAskAgain = false))
            viewModel.onEvent(SettingsEvent.OpenNotificationSettings)

            assertEquals(
                listOf(
                    SettingsEffect.NotificationPermissionDenied(canAskAgain = false),
                    SettingsEffect.OpenNotificationSettings,
                ),
                effects,
            )
        }

    @Test
    fun `disables the reminder without asking for any permission`() =
        runTest {
            repository.saveReminderSettings(ReminderSettings(enabled = true, time = LocalTime.of(18, 0)))
            notificationsAllowed = false
            val viewModel = settingsViewModel()
            val effects = collectEffects(viewModel)

            viewModel.onEvent(SettingsEvent.ReminderToggled(enabled = false))

            assertTrue(effects.isEmpty())
            assertFalse(repository.reminder.enabled)
        }

    @Test
    fun `shows the usage statistics`() =
        runTest {
            statistics.recordDeletion(12, 34_000_000)

            val state = settingsViewModel().uiState.first { it.statistics != null }

            assertEquals(UsageStatistics(LocalDate.of(2026, 3, 2), 12, 34_000_000), state.statistics)
        }

    @Test
    fun `opens the Play Store to rate the app and PayPal to donate`() =
        runTest {
            val viewModel = settingsViewModel()

            viewModel.onEvent(SettingsEvent.RateApp)
            assertEquals(SettingsEffect.OpenRating, viewModel.uiEffects.first())

            viewModel.onEvent(SettingsEvent.Donate)
            assertEquals(SettingsEffect.OpenDonation, viewModel.uiEffects.first())
        }

    @Test
    fun `opens the app language settings`() =
        runTest {
            val viewModel = settingsViewModel()

            viewModel.onEvent(SettingsEvent.OpenLanguageSettings)

            assertEquals(SettingsEffect.OpenLanguageSettings, viewModel.uiEffects.first())
        }

    @Test
    fun `opens the developer tools`() =
        runTest {
            val viewModel = settingsViewModel()

            viewModel.onEvent(SettingsEvent.OpenDeveloperTools)

            assertEquals(SettingsEffect.OpenDeveloperTools, viewModel.uiEffects.first())
        }

    @Test
    fun `opens the licenses`() =
        runTest {
            val viewModel = settingsViewModel()

            viewModel.onEvent(SettingsEvent.OpenLicenses)

            assertEquals(SettingsEffect.OpenLicenses, viewModel.uiEffects.first())
        }

    @Test
    fun `opens the tutorial again from the settings`() =
        runTest {
            val viewModel = settingsViewModel()

            viewModel.onEvent(SettingsEvent.OpenTutorial)

            assertEquals(SettingsEffect.OpenTutorial, viewModel.uiEffects.first())
        }

    @Test
    fun `goes back with the back arrow`() =
        runTest {
            val viewModel = settingsViewModel()

            viewModel.onEvent(SettingsEvent.Back)

            assertEquals(SettingsEffect.Close, viewModel.uiEffects.first())
        }

    @Test
    fun `follows the phone theme on a fresh install`() {
        assertEquals(ThemeMode.SYSTEM, settingsViewModel().uiState.value.themeMode)
    }

    @Test
    fun `saves the picked theme and shows it`() {
        val viewModel = settingsViewModel()

        viewModel.onEvent(SettingsEvent.ThemePicked(ThemeMode.DARK))

        assertEquals(ThemeMode.DARK, repository.themeMode)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }
}
