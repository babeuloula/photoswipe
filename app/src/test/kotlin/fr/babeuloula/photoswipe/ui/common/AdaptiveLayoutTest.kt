package fr.babeuloula.photoswipe.ui.common

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.ui.deck.DeckScreen
import fr.babeuloula.photoswipe.ui.deck.DeckUiState
import fr.babeuloula.photoswipe.ui.deck.PREVIEW_CARD
import fr.babeuloula.photoswipe.ui.home.HomeScreen
import fr.babeuloula.photoswipe.ui.home.HomeUiState
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Mises en page adaptatives (SPEC §8) selon la taille de la fenêtre, simulée par les qualificateurs
 * Robolectric : téléphone en portrait, tablette en portrait (largeur moyenne), tablette en paysage.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class AdaptiveLayoutTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int): String = composeRule.activity.getString(id)

    private fun layout(): WindowLayout {
        var layout: WindowLayout? = null
        composeRule.activity.setContent { layout = currentWindowLayout() }
        composeRule.waitForIdle()
        return layout!!
    }

    private val home =
        HomeUiState.Loaded(
            today = ReviewDay(LocalDate.of(2026, 7, 15), remainingCount = 2),
            previousDays = (1L..6L).map { ReviewDay(LocalDate.of(2026, 7, 15).minusDays(it), remainingCount = 1) },
        )

    private fun showHome() {
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                PhotoSwipeTheme { HomeScreen(state = home, onEvent = {}) }
            }
        }
    }

    private fun showDeck() {
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                PhotoSwipeTheme {
                    DeckScreen(
                        day = LocalDate.of(2026, 5, 14),
                        state = DeckUiState.Loaded(listOf(PREVIEW_CARD)),
                        onEvent = {},
                    )
                }
            }
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-port")
    fun `keeps the phone layout on a phone in portrait`() {
        assertEquals(WindowLayout.COMPACT, layout())

        showDeck()
        val keep = composeRule.onNodeWithContentDescription(text(R.string.deck_keep)).getBoundsInRoot()
        val delete = composeRule.onNodeWithContentDescription(text(R.string.deck_mark_for_deletion)).getBoundsInRoot()
        assertEquals("buttons stay on one row under the card", delete.top, keep.top)
    }

    @Test
    @Config(qualifiers = "w700dp-h1000dp-port")
    fun `centres the content on a tablet in portrait`() {
        assertEquals(WindowLayout.MEDIUM, layout())
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    fun `shows two panes on a tablet in landscape`() {
        assertEquals(WindowLayout.EXPANDED, layout())

        showHome()
        val today = composeRule.onNodeWithText(text(R.string.home_today)).getBoundsInRoot()
        val previousDays = composeRule.onNodeWithText(text(R.string.home_previous_days)).getBoundsInRoot()
        val start = composeRule.onNodeWithText(text(R.string.home_start_sorting)).getBoundsInRoot()
        assertTrue("previous days listed under today", previousDays.top > today.bottom)
        assertTrue("detail of the day on the right of the list", start.left > previousDays.right)

        showDeck()
        val keep = composeRule.onNodeWithContentDescription(text(R.string.deck_keep)).getBoundsInRoot()
        val delete = composeRule.onNodeWithContentDescription(text(R.string.deck_mark_for_deletion)).getBoundsInRoot()
        assertEquals("buttons stacked in a column beside the card", delete.left, keep.left)
        assertTrue(keep.top > delete.top)
    }

    @Test
    @Config(qualifiers = "w900dp-h420dp-land")
    fun `uses two panes in a short landscape window`() {
        assertEquals(WindowLayout.SHORT, layout())
    }
}
