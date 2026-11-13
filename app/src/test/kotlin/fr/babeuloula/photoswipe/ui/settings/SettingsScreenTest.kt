package fr.babeuloula.photoswipe.ui.settings

import android.text.format.DateFormat
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
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
import fr.babeuloula.photoswipe.domain.model.CatchUpWindow
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.model.ThemeMode
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

    private fun show(
        state: SettingsUiState,
        // Sans l'autorisation par défaut : l'heure du rappel garde son texte « Vers 18:00 ».
        exactAlarmAllowed: Boolean = false,
        shakeAvailable: Boolean = false,
    ) {
        composeRule.activity.setContent {
            PhotoSwipeTheme {
                SettingsScreen(
                    environment =
                        SettingsEnvironment(
                            exactAlarmAllowed = exactAlarmAllowed,
                            shakeAvailable = shakeAvailable,
                        ),
                    state = state,
                    onEvent = { events += it },
                )
            }
        }
    }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    private fun days(count: Int): String =
        composeRule.activity.resources.getQuantityString(R.plurals.settings_catch_up_days_option, count, count)

    /** Nœud du dialogue ouvert qui porte [label]. */

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
    fun `turns off the flags on photos that may have failed`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT))

        composeRule
            .onNodeWithText(text(R.string.settings_quality_hints))
            .performScrollTo()
            .assertIsOn()
            .performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.QualityHintsToggled(false)), events)
    }

    @Test
    fun `opens the progress page from the statistics row`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT))

        composeRule.onNodeWithText(text(R.string.settings_progress)).performScrollTo().performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.OpenProgress), events)
    }

    @Test
    fun `explains how to get the reminder right on time without the exact alarm permission`() {
        show(
            SettingsUiState(reminder = ReminderSettings(enabled = true, time = LocalTime.of(18, 0))),
            exactAlarmAllowed = false,
        )

        composeRule.onNodeWithText(text(R.string.settings_reminder_exact)).performScrollTo().performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.OpenExactAlarmSettings), events)
    }

    @Test
    fun `promises the reminder right on time with the exact alarm permission`() {
        show(
            SettingsUiState(reminder = ReminderSettings(enabled = true, time = LocalTime.of(18, 0))),
            exactAlarmAllowed = true,
        )

        composeRule.onNodeWithText(text(R.string.settings_reminder_exact)).assertDoesNotExist()
        composeRule.onNodeWithText("sharp", substring = true).assertExists()
    }

    @Test
    fun `offers to rate the app and to support the developer`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT))

        composeRule.onNodeWithText(text(R.string.settings_support_section)).performScrollTo().assertExists()
        composeRule.onNodeWithText(text(R.string.settings_support_rate)).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.settings_support_donate)).performScrollTo().performClick()

        assertEquals(listOf(SettingsEvent.RateApp, SettingsEvent.Donate), events)
    }

    @Test
    fun `offers to watch the tutorial again from the about section`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT))

        composeRule.onNodeWithText(text(R.string.settings_about_tutorial)).performScrollTo().performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.OpenTutorial), events)
    }

    @Test
    fun `offers to read the news of every version again from the about section`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT))

        composeRule.onNodeWithText(text(R.string.settings_about_whats_new)).performScrollTo().performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.WhatsNewToggled(true)), events)
    }

    @Test
    fun `shows the news of every version, and closes them`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT, whatsNewOpen = true))

        composeRule.onNodeWithText(text(R.string.whats_new_1_4_0_progress_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.whats_new_close)).performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.WhatsNewToggled(false)), events)
    }

    @Test
    fun `opens the source code from the about section`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT))

        composeRule.onNodeWithText(text(R.string.settings_about_source_code)).performScrollTo().performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.OpenSourceCode), events)
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

    @Test
    fun `turns the animations off from the whole row, announced as a switch`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT, themeMode = ThemeMode.LIGHT))

        composeRule
            .onNodeWithText(text(R.string.settings_animations_summary))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_animations)).performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.AnimationsToggled(false)), events)
    }

    @Test
    fun `turns pure black on from the whole row, announced as a switch`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT, themeMode = ThemeMode.DARK))

        composeRule
            .onNodeWithText(text(R.string.settings_pure_black_summary))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(text(R.string.settings_pure_black))
            .assertIsOff()
            .performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.PureBlackToggled(true)), events)
    }

    @Test
    fun `turns the end of deck celebration on from the whole row, announced as a switch`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT, themeMode = ThemeMode.SYSTEM))

        composeRule
            .onNodeWithText(text(R.string.settings_celebration_summary))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(text(R.string.settings_celebration))
            .assertIsOff()
            .performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.CelebrationToggled(true)), events)
    }

    @Test
    fun `shows the end of deck celebration turned on`() {
        show(
            SettingsUiState(
                reminder = ReminderSettings.DEFAULT,
                themeMode = ThemeMode.SYSTEM,
                celebrationEnabled = true,
            ),
        )

        composeRule
            .onNodeWithText(text(R.string.settings_celebration))
            .performScrollTo()
            .assertIsOn()
    }

    @Test
    fun `turns shake to undo on from the whole row, announced as a switch, on a phone with an accelerometer`() {
        show(
            SettingsUiState(reminder = ReminderSettings.DEFAULT, themeMode = ThemeMode.SYSTEM),
            shakeAvailable = true,
        )

        composeRule
            .onNodeWithText(text(R.string.settings_shake_to_undo_summary))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(text(R.string.settings_shake_to_undo))
            .assertIsOff()
            .performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.ShakeToUndoToggled(true)), events)
    }

    @Test
    fun `shows shake to undo turned on`() {
        show(
            SettingsUiState(reminder = ReminderSettings.DEFAULT, themeMode = ThemeMode.SYSTEM, shakeToUndo = true),
            shakeAvailable = true,
        )

        composeRule
            .onNodeWithText(text(R.string.settings_shake_to_undo))
            .performScrollTo()
            .assertIsOn()
    }

    @Test
    fun `does not offer shake to undo on a phone without an accelerometer`() {
        show(
            SettingsUiState(reminder = ReminderSettings.DEFAULT, themeMode = ThemeMode.SYSTEM),
            shakeAvailable = false,
        )

        composeRule.onNodeWithText(text(R.string.settings_shake_to_undo)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.settings_gestures_section)).assertDoesNotExist()
    }

    @Test
    fun `offers pure black only when the app is dark`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT, themeMode = ThemeMode.LIGHT))

        composeRule
            .onNodeWithText(text(R.string.settings_animations))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_pure_black)).assertDoesNotExist()
    }

    @Test
    fun `shows pure black turned on`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT, themeMode = ThemeMode.DARK, pureBlack = true))

        composeRule
            .onNodeWithText(text(R.string.settings_pure_black))
            .performScrollTo()
            .assertIsOn()
    }

    @Test
    fun `picks how many days to catch up among 7, 14 and 30, right in the list`() {
        show(
            SettingsUiState(
                reminder = ReminderSettings.DEFAULT,
                themeMode = ThemeMode.LIGHT,
                catchUpWindow = CatchUpWindow.WEEK,
            ),
        )

        composeRule.onNodeWithText(text(R.string.settings_catch_up_days)).performScrollTo().assertExists()
        composeRule.onNodeWithText(days(7)).performScrollTo().assertIsSelected()
        composeRule.onNodeWithText(days(14)).performScrollTo().assertIsNotSelected()
        composeRule.onNodeWithText(days(30)).performScrollTo().performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.CatchUpWindowPicked(CatchUpWindow.MONTH)), events)
    }

    @Test
    fun `picks a theme among the phone one, light and dark, right in the list`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT, themeMode = ThemeMode.LIGHT))

        composeRule.onNodeWithText(text(R.string.settings_theme)).performScrollTo().assertExists()
        composeRule.onNodeWithText(text(R.string.settings_theme_system)).performScrollTo().assertIsNotSelected()
        composeRule.onNodeWithText(text(R.string.settings_theme_light)).performScrollTo().assertIsSelected()
        composeRule.onNodeWithText(text(R.string.settings_theme_dark)).performScrollTo().performClick()

        assertEquals(listOf<SettingsEvent>(SettingsEvent.ThemePicked(ThemeMode.DARK)), events)
    }

    @Test
    @Config(qualifiers = "fr")
    fun `shows the theme choice in French`() {
        show(SettingsUiState(reminder = ReminderSettings.DEFAULT, themeMode = ThemeMode.SYSTEM))

        composeRule.onNodeWithText("Celui du téléphone").performScrollTo().assertExists()
        composeRule.onNodeWithText("Clair").performScrollTo().assertExists()
        composeRule.onNodeWithText("Sombre").performScrollTo().assertExists()
    }
}
