package fr.babeuloula.photoswipe.ui.deck

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Pile de cartes sous celle du dessus (PS-147). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp")
class DeckStackTest {
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

    private val stack =
        listOf(
            PREVIEW_CARD,
            PREVIEW_CARD.copy(id = MediaId(2), uri = "content://media/external/images/media/2"),
            PREVIEW_CARD.copy(id = MediaId(3), uri = "content://media/external/images/media/3"),
        )

    @Test
    fun `lays the following cards under the top one, as a stack`() {
        show(DeckUiState.Loaded(stack))

        composeRule.onNodeWithTag(NEXT_CARD_TAG).assertExists()
        composeRule.onNodeWithTag("${STACK_CARD_TAG}2").assertExists()
    }

    @Test
    fun `shows the date and size of the top card only`() {
        show(DeckUiState.Loaded(stack))

        // Les cartes du dessous ne montrent ni repère ni pastilles : un seul repère à l'écran.
        assertEquals(1, composeRule.onAllNodesWithText("years ago", substring = true).fetchSemanticsNodes().size)
    }
}
