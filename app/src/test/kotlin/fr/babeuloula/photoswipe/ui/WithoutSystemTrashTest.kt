package fr.babeuloula.photoswipe.ui

import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.platform.PlatformFeatures
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.common.LocalPlatformFeatures
import fr.babeuloula.photoswipe.ui.home.HomeScreen
import fr.babeuloula.photoswipe.ui.home.HomeUiState
import fr.babeuloula.photoswipe.ui.settings.SettingsScreen
import fr.babeuloula.photoswipe.ui.settings.SettingsUiState
import fr.babeuloula.photoswipe.ui.summary.SummaryScreen
import fr.babeuloula.photoswipe.ui.summary.SummaryUiState
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Écrans sur Android 10, qui n'a pas de corbeille dans sa galerie (PS-185) : ni page « Corbeille »
 * ni promesse de restauration, et une suppression annoncée comme définitive.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class WithoutSystemTrashTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int): String = composeRule.activity.getString(id)

    private fun plural(
        id: Int,
        count: Int,
        vararg args: Any,
    ): String = composeRule.activity.resources.getQuantityString(id, count, count, *args)

    private fun showOnAndroid10(content: @Composable () -> Unit) {
        composeRule.activity.setContent {
            CompositionLocalProvider(
                LocalImageLoader provides ImageLoader(composeRule.activity),
                LocalPlatformFeatures provides PlatformFeatures.forSdk(Build.VERSION_CODES.Q),
            ) {
                PhotoSwipeTheme { content() }
            }
        }
    }

    @Test
    fun `leaves the trash out of the home top bar`() {
        showOnAndroid10 { HomeScreen(state = HomeUiState.Loading, onEvent = {}) }

        composeRule.onNodeWithContentDescription(text(R.string.home_open_progress)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.home_open_trash)).assertDoesNotExist()
    }

    @Test
    fun `leaves the trash out of the activity settings, keeping the progress page`() {
        showOnAndroid10 { SettingsScreen(state = SettingsUiState(reminder = ReminderSettings.DEFAULT), onEvent = {}) }

        composeRule.onNodeWithText(text(R.string.settings_progress)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.settings_trash_summary)).assertDoesNotExist()
    }

    @Test
    fun `frees the space right after confirming, without any trash to open`() {
        val marked =
            SessionSummary(
                keptCount = 2,
                markedForDeletionCount = 3,
                favoriteCount = 0,
                estimatedFreedBytes = 9_000_000,
            )

        showOnAndroid10 { SummaryScreen(state = SummaryUiState.Loaded(marked), onEvent = {}) }

        composeRule.onNodeWithText(plural(R.plurals.summary_if_confirmed_now, 3)).assertIsDisplayed()
        composeRule.onNodeWithText(plural(R.plurals.summary_if_confirmed, 3)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.summary_open_trash)).assertDoesNotExist()
    }

    @Test
    fun `says what was deleted for good today`() {
        val confirmed =
            SessionSummary(
                keptCount = 2,
                markedForDeletionCount = 0,
                favoriteCount = 0,
                estimatedFreedBytes = 0,
                trashedTodayCount = 10,
                trashedTodayBytes = 25_000_000,
            )

        showOnAndroid10 { SummaryScreen(state = SummaryUiState.Loaded(confirmed), onEvent = {}) }

        composeRule.onNodeWithText(text(R.string.summary_deleted_for_good_today)).assertIsDisplayed()
        composeRule.onNodeWithText(plural(R.plurals.summary_deleted_for_good_detail, 10)).assertIsDisplayed()
        composeRule
            .onNodeWithText(plural(R.plurals.summary_deleted_for_good_today, 10, "25 MB"))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_freed_today)).assertDoesNotExist()
    }

    @Test
    fun `asks to delete a single media for good`() {
        val oneMarked =
            SessionSummary(keptCount = 0, markedForDeletionCount = 1, favoriteCount = 0, estimatedFreedBytes = 1_000)

        showOnAndroid10 {
            SummaryScreen(state = SummaryUiState.Loaded(oneMarked, askingTrashConfirmation = true), onEvent = {})
        }

        composeRule.onNodeWithText(plural(R.plurals.summary_delete_confirm_title, 1)).assertIsDisplayed()
        composeRule
            .onNodeWithText(text(R.string.summary_delete_confirm_text))
            .assertIsDisplayed()
    }
}
