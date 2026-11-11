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
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import fr.babeuloula.photoswipe.ui.deck.fileSizeLabel
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
    ) {
        composeRule.activity.setContent {
            PhotoSwipeTheme {
                SettingsScreen(
                    environment = SettingsEnvironment(exactAlarmAllowed = exactAlarmAllowed),
                    state = state,
                    onEvent = { events += it },
                )
            }
        }
    }

    private fun text(id: Int): String = composeRule.activity.getString(id)

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
    fun `shows how many media were deleted and the space freed since the first use`() {
        show(
            SettingsUiState(
                reminder = ReminderSettings.DEFAULT,
                statistics = UsageStatistics(LocalDate.of(2026, 3, 2), deletedCount = 128, freedBytes = 1_200_000_000),
            ),
        )

        composeRule.onNodeWithText("March 2, 2026").performScrollTo().assertExists()
        composeRule.onNodeWithText("128 photos and videos").performScrollTo().assertExists()
        composeRule.onNodeWithText(fileSizeLabel(composeRule.activity, 1_200_000_000)).performScrollTo().assertExists()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `says in French that nothing was deleted yet`() {
        show(
            SettingsUiState(
                reminder = ReminderSettings.DEFAULT,
                statistics = UsageStatistics(LocalDate.of(2026, 3, 2)),
            ),
        )

        composeRule.onNodeWithText("2 mars 2026").performScrollTo().assertExists()
        composeRule.onNodeWithText("Aucun média supprimé pour l'instant").performScrollTo().assertExists()
        composeRule.onNodeWithText(text(R.string.settings_statistics_freed)).assertDoesNotExist()
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
