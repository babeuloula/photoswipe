package fr.babeuloula.photoswipe.ui.deck

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.domain.model.QualityHint
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Pastille des photos prises à la suite sur la carte du dessus (PS-140). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp")
class DeckSimilarPhotosTest {
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
    fun `says the photo is one of several taken one after another`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(similar = SimilarGroup(1, 3)))))

        composeRule.onNodeWithContentDescription("Photo 1 of 3 taken one after another").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `says the photo is one of several taken one after another, in French`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(similar = SimilarGroup(2, 2)))))

        composeRule.onNodeWithContentDescription("Photo 2 sur 2 prises à la suite").assertIsDisplayed()
    }

    @Test
    fun `says a photo may be blurry or very dark, without deciding`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(qualityHint = QualityHint.BLURRY))))
        composeRule.onNodeWithContentDescription("This photo may be blurry").assertIsDisplayed()

        show(DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(qualityHint = QualityHint.DARK))))
        composeRule.onNodeWithContentDescription("This photo is very dark").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `says a photo may be blurry in French`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(qualityHint = QualityHint.BLURRY))))

        composeRule.onNodeWithText("Floue ?", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `says a raw and jpeg pair is sorted as one photo`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(raw = true))))

        composeRule
            .onNodeWithContentDescription("RAW and JPEG files of the same photo, sorted together")
            .assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `says a raw and jpeg pair is sorted as one photo, in French`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(raw = true))))

        composeRule
            .onNodeWithContentDescription("Fichiers RAW et JPEG d'une même photo, triés ensemble")
            .assertIsDisplayed()
    }

    @Test
    fun `shows no raw chip on a single file photo`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithText("RAW + JPEG", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `says the photo is a duplicate of another one of the day`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(similar = SimilarGroup(2, 2, duplicate = true)))))

        composeRule.onNodeWithContentDescription("Duplicate: photo 2 of 2 of the same image").assertIsDisplayed()
    }

    @Test
    fun `shows no such chip for a photo taken alone`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithText("1/3").assertDoesNotExist()
    }
}
