package fr.babeuloula.photoswipe.ui.summary

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.SessionSummary
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
        composeRule.activity.setContent { PhotoSwipeTheme { SummaryScreen(state = state, onEvent = { events += it }) } }
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
    fun `shows the kept, marked and favorite counts`() {
        show(SummaryUiState.Loaded(summary))

        composeRule.onNodeWithText("12 items kept").assertIsDisplayed()
        composeRule.onNodeWithText("4 items marked for deletion").assertIsDisplayed()
        composeRule.onNodeWithText("1 favorite").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `shows the summary in French`() {
        show(SummaryUiState.Loaded(summary.copy(keptCount = 1, markedForDeletionCount = 2, favoriteCount = 0)))

        composeRule.onNodeWithText("1 média gardé").assertIsDisplayed()
        composeRule.onNodeWithText("2 médias marqués pour suppression").assertIsDisplayed()
        composeRule.onNodeWithText("0 favori").assertIsDisplayed()
        composeRule.onNodeWithText("Espace libéré estimé").assertIsDisplayed()
    }

    @Test
    fun `says nothing is deleted yet`() {
        show(SummaryUiState.Loaded(summary))

        composeRule.onNodeWithText(text(R.string.summary_nothing_deleted_yet)).assertIsDisplayed()
    }

    @Test
    fun `goes home later or back with the arrow`() {
        show(SummaryUiState.Loaded(summary))

        composeRule.onNodeWithText(text(R.string.summary_later)).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()

        assertEquals(listOf(SummaryEvent.Later, SummaryEvent.Back), events)
    }

    @Test
    fun `confirms the deletion from its button`() {
        show(SummaryUiState.Loaded(summary))

        composeRule
            .onNodeWithText(text(R.string.summary_confirm))
            .performScrollTo()
            .assertIsEnabled()
            .performClick()

        assertEquals(listOf<SummaryEvent>(SummaryEvent.Confirm), events)
    }

    @Test
    fun `cannot confirm when nothing is marked`() {
        show(SummaryUiState.Loaded(summary.copy(markedForDeletionCount = 0, estimatedFreedBytes = 0)))

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
}
