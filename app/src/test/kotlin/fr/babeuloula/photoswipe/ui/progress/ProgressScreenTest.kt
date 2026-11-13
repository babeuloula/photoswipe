package fr.babeuloula.photoswipe.ui.progress

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
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

/** Page « Progression » composée seule dans `MainActivity`, avec des états fixés. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class ProgressScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<ProgressEvent>()

    private fun show(state: ProgressUiState) {
        composeRule.activity.setContent {
            PhotoSwipeTheme { ProgressScreen(state = state, onEvent = { events += it }) }
        }
    }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    @Test
    fun `shows how many media were deleted and the space freed since the first use`() {
        show(
            ProgressUiState.Loaded(
                UsageStatistics(LocalDate.of(2026, 3, 2), deletedCount = 128, freedBytes = 1_200_000_000),
            ),
        )

        composeRule.onNodeWithText(text(R.string.progress_title)).assertIsDisplayed()
        composeRule.onNodeWithText("March 2, 2026").performScrollTo().assertExists()
        composeRule.onNodeWithText("128 photos and videos").performScrollTo().assertExists()
        composeRule.onNodeWithText(fileSizeLabel(composeRule.activity, 1_200_000_000)).performScrollTo().assertExists()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `says in French that nothing was deleted yet`() {
        show(ProgressUiState.Loaded(UsageStatistics(LocalDate.of(2026, 3, 2))))

        composeRule.onNodeWithText("Progression").assertIsDisplayed()
        composeRule.onNodeWithText("2 mars 2026").performScrollTo().assertExists()
        composeRule.onNodeWithText("Aucun média supprimé pour l'instant").performScrollTo().assertExists()
        composeRule.onNodeWithText(text(R.string.settings_statistics_freed)).assertDoesNotExist()
    }

    @Test
    fun `offers to try again when the statistics cannot be read`() {
        show(ProgressUiState.Error)

        composeRule.onNodeWithText(text(R.string.progress_error)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_retry)).performClick()

        assertEquals(listOf<ProgressEvent>(ProgressEvent.Retry), events)
    }

    @Test
    fun `goes back from the top bar`() {
        show(ProgressUiState.Loading)

        composeRule.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()

        assertEquals(listOf<ProgressEvent>(ProgressEvent.Back), events)
    }
}
