package fr.babeuloula.photoswipe.ui.summary

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Résumé composé seul dans `MainActivity`, avec des états fixés. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class SummaryScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<SummaryEvent>()

    private fun show(state: SummaryUiState) {
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                PhotoSwipeTheme { SummaryScreen(state = state, onEvent = { events += it }) }
            }
        }
    }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    private val summary =
        SessionSummary(keptCount = 12, markedForDeletionCount = 4, favoriteCount = 1, estimatedFreedBytes = 48_000_000)

    @Test
    fun `puts the estimated freed space first`() {
        show(SummaryUiState.Loaded(summary))

        composeRule.onNodeWithText(text(R.string.summary_freed_space)).assertIsDisplayed()
        composeRule.onNodeWithText("48 MB").assertIsDisplayed()
    }

    @Test
    fun `shows what was kept and favorited today, then what waits for confirmation`() {
        show(SummaryUiState.Loaded(summary))

        composeRule.onNodeWithText("12 items kept").assertIsDisplayed()
        composeRule.onNodeWithText("4 items marked for deletion").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1 favorite").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_sorted_today)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_to_confirm)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `shows the summary in French`() {
        show(SummaryUiState.Loaded(summary.copy(keptCount = 1, markedForDeletionCount = 2, favoriteCount = 0)))

        composeRule.onNodeWithText("1 média gardé").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2 médias marqués pour suppression").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("0 favori").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Trié aujourd'hui").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Espace libéré estimé").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `previews the last marked media and says how many others wait, and what the freed space depends on`() {
        val previews = (1L..5L).map { aMediaItem(id = it) }
        show(SummaryUiState.Loaded(summary.copy(markedForDeletionCount = 12), markedPreviews = previews))

        composeRule.onNodeWithText("If you confirm the 12 marked items").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("and 7 more").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `says nothing is deleted yet`() {
        show(SummaryUiState.Loaded(summary))

        composeRule.onNodeWithText(text(R.string.summary_nothing_deleted_yet)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `goes home later or back with the arrow`() {
        show(SummaryUiState.Loaded(summary))

        composeRule.onNodeWithText(text(R.string.summary_later)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()

        assertEquals(listOf(SummaryEvent.Later, SummaryEvent.Back), events)
    }

    @Test
    fun `confirms the deletion from its button`() {
        show(SummaryUiState.Loaded(summary))

        composeRule
            .onNodeWithText(text(R.string.summary_confirm))
            .assertIsEnabled()
            .performClick()

        assertEquals(listOf<SummaryEvent>(SummaryEvent.Confirm), events)
    }

    @Test
    fun `cannot confirm when nothing is marked`() {
        show(
            SummaryUiState.Loaded(
                SessionSummary(keptCount = 3, markedForDeletionCount = 0, favoriteCount = 0, estimatedFreedBytes = 0),
            ),
        )

        composeRule.onNodeWithText(text(R.string.summary_confirm)).assertIsNotEnabled()
    }

    @Test
    fun `cannot confirm twice while the system dialogs are shown`() {
        show(SummaryUiState.Loaded(summary, confirming = true))

        composeRule.onNodeWithText(text(R.string.summary_confirm)).assertIsNotEnabled()
    }

    @Test
    fun `retries after an error`() {
        show(SummaryUiState.Error)

        composeRule.onNodeWithText(text(R.string.summary_error)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_retry)).performClick()

        assertEquals(listOf<SummaryEvent>(SummaryEvent.Retry), events)
    }

    @Test
    fun `asks its own confirmation before trashing with media management`() {
        show(
            SummaryUiState.Loaded(
                SessionSummary(
                    keptCount = 0,
                    markedForDeletionCount = 3,
                    favoriteCount = 0,
                    estimatedFreedBytes = 9_000_000,
                ),
                askingTrashConfirmation = true,
            ),
        )

        composeRule.onNode(isDialog()).assertExists()
        composeRule
            .onNodeWithText(
                composeRule.activity.resources.getQuantityString(R.plurals.summary_trash_confirm_title, 3, 3),
            ).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_trash_confirm_text)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_trash_confirm_action)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(android.R.string.cancel)).performClick()

        assertEquals(listOf(SummaryEvent.TrashConfirmationAccepted, SummaryEvent.TrashConfirmationDismissed), events)
    }
}
