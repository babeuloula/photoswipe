package fr.babeuloula.photoswipe.ui.deck

import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.platform.PlatformFeatures
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.common.LocalPlatformFeatures
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Pile sur un téléphone sans favoris de la galerie (Android 10, PS-183) : ni « Ajouter aux favoris »
 * ni « Retirer des favoris », et le glissement vers le haut annonce seulement « Plus d'actions ».
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp")
class DeckWithoutFavoritesTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val android10 = PlatformFeatures.forSdk(Build.VERSION_CODES.Q)

    private fun text(id: Int): String = composeRule.activity.getString(id)

    @Test
    fun `offers neither favorite nor unfavorite in the menu on android 10`() {
        val state = mutableStateOf(DeckUiState.Loaded(listOf(PREVIEW_CARD), menuOpen = true))
        composeRule.activity.setContent {
            CompositionLocalProvider(
                LocalImageLoader provides ImageLoader(composeRule.activity),
                LocalPlatformFeatures provides android10,
            ) {
                PhotoSwipeTheme { DeckScreen(day = LocalDate.of(2026, 5, 14), state = state.value, onEvent = {}) }
            }
        }

        composeRule.onNodeWithText(text(R.string.deck_favorite)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.deck_share)).assertExists()
        composeRule.onNodeWithText(text(R.string.deck_open_with)).assertExists()

        state.value = DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(favorite = true)), menuOpen = true)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(R.string.deck_unfavorite)).assertDoesNotExist()
    }

    @Test
    fun `shows more actions instead of the favorite veil when swiping up on android 10`() {
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalPlatformFeatures provides android10) {
                PhotoSwipeTheme { SwipeIndicator(SwipeIndication(CardSide.TOP, 1f)) }
            }
        }

        composeRule.onNodeWithTag(MENU_INDICATOR_TAG).assertExists()
        composeRule.onNodeWithTag(FAVORITE_INDICATOR_TAG).assertDoesNotExist()
    }

    @Test
    fun `shows favorite or share when swiping up where the gallery has favorites`() {
        composeRule.activity.setContent {
            PhotoSwipeTheme { SwipeIndicator(SwipeIndication(CardSide.TOP, 1f)) }
        }

        composeRule.onNodeWithTag(FAVORITE_INDICATOR_TAG).assertExists()
        composeRule.onNodeWithTag(MENU_INDICATOR_TAG).assertDoesNotExist()
    }
}
