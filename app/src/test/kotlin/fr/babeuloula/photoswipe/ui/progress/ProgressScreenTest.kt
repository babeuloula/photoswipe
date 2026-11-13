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
import fr.babeuloula.photoswipe.domain.model.ActivityReport
import fr.babeuloula.photoswipe.domain.model.MonthlyActivity
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
import java.time.YearMonth

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

    private companion object {
        /** Six mois sans aucun tri, historique vide. */
        val EMPTY_ACTIVITY = ActivityReport((6..11).map { MonthlyActivity(YearMonth.of(2026, it)) })
    }

    @Test
    fun `shows how many media were deleted and the space freed since the first use`() {
        show(
            ProgressUiState.Loaded(
                UsageStatistics(LocalDate.of(2026, 3, 2), deletedCount = 128, freedBytes = 1_200_000_000),
                EMPTY_ACTIVITY,
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
        show(ProgressUiState.Loaded(UsageStatistics(LocalDate.of(2026, 3, 2)), EMPTY_ACTIVITY))

        composeRule.onNodeWithText("Progression").assertIsDisplayed()
        composeRule.onNodeWithText("2 mars 2026").performScrollTo().assertExists()
        composeRule.onNodeWithText("Aucun média supprimé pour l'instant").performScrollTo().assertExists()
        composeRule.onNodeWithText(text(R.string.settings_statistics_freed)).assertDoesNotExist()
    }

    @Test
    fun `explains where the charts will be while nothing was sorted in the last months`() {
        show(ProgressUiState.Loaded(UsageStatistics(LocalDate.of(2026, 3, 2)), EMPTY_ACTIVITY))

        composeRule.onNodeWithText("The last 6 months").performScrollTo().assertExists()
        composeRule.onNodeWithText(text(R.string.progress_activity_empty)).performScrollTo().assertExists()
        composeRule.onNodeWithText(text(R.string.progress_activity_sorted)).assertDoesNotExist()
    }

    @Test
    fun `charts the media sorted and the space freed per month, read as a whole by TalkBack`() {
        show(ProgressUiState.Loaded(UsageStatistics(LocalDate.of(2026, 3, 2)), PREVIEW_ACTIVITY))

        composeRule.onNodeWithText("70% kept, 30% deleted").performScrollTo().assertExists()
        composeRule
            .onNodeWithContentDescription("June: kept 12, deleted 3.", substring = true)
            .performScrollTo()
            .assertExists()
        composeRule
            .onNodeWithContentDescription("July: ${fileSizeLabel(composeRule.activity, 0)}.", substring = true)
            .performScrollTo()
            .assertExists()
        composeRule
            .onNodeWithText("${fileSizeLabel(composeRule.activity, 33 * 40_000_000L)} freed in 6 months")
            .performScrollTo()
            .assertExists()
        composeRule.onNodeWithText("History since Jun 4, 2026").performScrollTo().assertExists()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `charts the activity in French`() {
        show(ProgressUiState.Loaded(UsageStatistics(LocalDate.of(2026, 3, 2)), PREVIEW_ACTIVITY))

        composeRule.onNodeWithText("Les 6 derniers mois").performScrollTo().assertExists()
        composeRule.onNodeWithText("70 % gardés, 30 % supprimés").performScrollTo().assertExists()
        composeRule
            .onNodeWithContentDescription("août : gardés 30, supprimés 18.", substring = true)
            .performScrollTo()
            .assertExists()
        composeRule.onNodeWithText("Historique depuis le 4 juin 2026").performScrollTo().assertExists()
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
