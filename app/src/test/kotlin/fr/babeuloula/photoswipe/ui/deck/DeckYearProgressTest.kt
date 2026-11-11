package fr.babeuloula.photoswipe.ui.deck

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Ligne de l'année de la carte du dessus, sous la progression de la pile (PS-138). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp")
class DeckYearProgressTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun show(state: DeckUiState) {
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                PhotoSwipeTheme { DeckScreen(day = LocalDate.of(2026, 5, 14), state = state, onEvent = {}) }
            }
        }
    }

    @Test
    fun `says which year the top card comes from and where it stands in that year`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD), remainingCount = 20, yearPosition = 2, yearTotal = 5))

        composeRule.onNodeWithText("2023 · 2 of 5 from that year").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `says which year the top card comes from in French`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD), yearPosition = 1, yearTotal = 3))

        composeRule.onNodeWithText("2023 · 1 sur 3 de cette année").assertIsDisplayed()
    }

    @Test
    fun `cuts the progress bar into one piece per year and says how many years there are`() {
        val segments = listOf(YearSegment(2025, 1, 1), YearSegment(2024, 0, 4), YearSegment(2021, 0, 2))
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD), remainingCount = 6, sortedCount = 1, yearSegments = segments))

        composeRule.onNodeWithTag(YEAR_SEGMENTS_TAG).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("2 of 7, over 3 years").assertIsDisplayed()
    }

    @Test
    fun `keeps a single bar when the deck has one year`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD), yearSegments = listOf(YearSegment(2023, 0, 1))))

        composeRule.onNodeWithTag(YEAR_SEGMENTS_TAG).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("1 of 1").assertIsDisplayed()
    }
}
