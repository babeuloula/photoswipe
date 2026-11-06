package fr.babeuloula.photoswipe.ui.settings

import android.text.format.DateFormat
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date

/** Réglages composés seuls dans `MainActivity`, avec des états fixés. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class SettingsScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<SettingsEvent>()

    private fun show(state: SettingsUiState) {
        composeRule.activity.setContent {
            PhotoSwipeTheme {
                SettingsScreen(
                    state = state,
                    onEvent = { events += it },
                )
            }
        }
    }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    @Test
    fun `switches the reminder on from its whole row`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT))

        composeRule.onNodeWithText(text(R.string.settings_reminder_enabled)).assertIsOff().performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.ReminderToggled(enabled = true)), events)
    }

    @Test
    fun `shows an enabled reminder and its time`() {
        show(SettingsUiState(reminder = ReminderSettings(enabled = true, time = LocalTime.of(18, 0))))

        composeRule.onNodeWithText(text(R.string.settings_reminder_enabled)).assertIsOn()
        val expected =
            DateFormat
                .getTimeFormat(composeRule.activity)
                .format(
                    Date.from(
                        LocalDate
                            .now()
                            .atTime(18, 0)
                            .atZone(ZoneId.systemDefault())
                            .toInstant(),
                    ),
                )
        composeRule.onNodeWithText(expected, substring = true).assertExists()
    }

    @Test
    fun `opens the time picker from the reminder time`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT))

        composeRule.onNodeWithText(text(R.string.settings_reminder_time)).performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.PickReminderTime), events)
    }

    @Test
    fun `confirms the time chosen in the picker`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT, timePickerOpen = true))

        composeRule.onNode(isDialog()).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(android.R.string.ok)).performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.ReminderTimePicked(LocalTime.of(18, 0))), events)
    }

    @Test
    @Config(qualifiers = "fr")
    fun `shows the settings in French`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT))

        composeRule.onNodeWithText("Rappel quotidien").assertExists()
        composeRule.onNodeWithText("Me le rappeler chaque jour").assertExists()
    }

    @Test
    fun `shows the app version and opens the licenses from the about section`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT))
        val version =
            composeRule.activity.packageManager
                .getPackageInfo(composeRule.activity.packageName, 0)
                .versionName!!

        composeRule.onNodeWithText(text(R.string.settings_about_section)).performScrollTo().assertExists()
        composeRule.onNodeWithText(version).performScrollTo().assertExists()
        composeRule.onNodeWithText(text(R.string.settings_about_licenses)).performScrollTo().performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.OpenLicenses), events)
    }

    @Test
    fun `shows the app language and opens its system setting`() {
        composeRule.activity.setContent {
            PhotoSwipeTheme {
                SettingsScreen(
                    state = SettingsUiState(reminder = ReminderSettings.DEFAULT),
                    onEvent = { events += it },
                    environment = SettingsEnvironment(languageLabel = "Français"),
                )
            }
        }

        composeRule.onNodeWithText("Français").performScrollTo().assertExists()
        composeRule.onNodeWithText(text(R.string.settings_language)).performScrollTo().performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.OpenLanguageSettings), events)
    }

    @Test
    fun `shows the developer tools entry only when the variant has them`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT))
        composeRule.onNodeWithText(text(R.string.settings_developer_tools)).assertDoesNotExist()

        composeRule.activity.setContent {
            PhotoSwipeTheme {
                SettingsScreen(
                    state = SettingsUiState(reminder = ReminderSettings.DEFAULT),
                    onEvent = { events += it },
                    environment = SettingsEnvironment(showDeveloperTools = true),
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.settings_developer_tools)).performScrollTo().performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.OpenDeveloperTools), events)
    }
}
