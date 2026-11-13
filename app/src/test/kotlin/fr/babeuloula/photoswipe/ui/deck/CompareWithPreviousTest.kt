package fr.babeuloula.photoswipe.ui.deck

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Comparer avec la photo précédente d'un groupe en maintenant le doigt (PS-160). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp")
class CompareWithPreviousTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<DeckEvent>()

    private val second =
        PREVIEW_CARD.copy(similar = SimilarGroup(2, 3, previousUri = "content://media/external/images/media/9"))

    private fun show(card: MediaCardUiState) {
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                PhotoSwipeTheme {
                    DeckScreen(day = LocalDate.of(2026, 5, 14), state = DeckUiState.Loaded(listOf(card)), onEvent = {
                        events +=
                            it
                    })
                }
            }
        }
    }

    /** Photo de la carte du dessus, repérée par le libellé de son appui. */
    private fun photo() =
        composeRule.onNode(
            SemanticsMatcher("click label View full screen") {
                it.config.getOrNull(SemanticsActions.OnClick)?.label == "View full screen"
            },
        )

    @Test
    fun `shows the previous photo while the finger stays down, then comes back on release`() {
        show(second)

        photo().performTouchInput {
            down(center)
            advanceEventTime(LONG_PRESS_MILLIS)
            move()
        }
        composeRule.onNodeWithTag(COMPARED_PHOTO_TAG).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Previous photo · 1 of 3").assertIsDisplayed()

        photo().performTouchInput { up() }

        composeRule.onNodeWithTag(COMPARED_PHOTO_TAG).assertDoesNotExist()
        // Le maintien n'a ni ouvert la photo en grand ni décidé de la carte.
        assertTrue(events.isEmpty())
        composeRule.onNodeWithTag(PHOTO_VIEWER_TAG).assertDoesNotExist()
    }

    @Test
    fun `switches between the two photos with the TalkBack action`() {
        show(second)

        photo()
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .single { it.label == "Compare with the previous photo" }
            .action()
        composeRule.onNodeWithTag(COMPARED_PHOTO_TAG).assertIsDisplayed()

        photo()
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .single { it.label == "Back to this photo" }
            .action()
        composeRule.onNodeWithTag(COMPARED_PHOTO_TAG).assertDoesNotExist()
    }

    @Test
    fun `offers nothing to compare on the first photo of a group, nor outside a group`() {
        show(PREVIEW_CARD.copy(similar = SimilarGroup(1, 3)))
        photo().performTouchInput {
            down(center)
            advanceEventTime(LONG_PRESS_MILLIS)
            up()
        }
        composeRule.onNodeWithTag(COMPARED_PHOTO_TAG).assertDoesNotExist()

        show(PREVIEW_CARD)
        assertTrue(SemanticsActions.CustomActions !in photo().fetchSemanticsNode().config)
    }

    private companion object {
        /** Plus long que le délai d'un appui long. */
        const val LONG_PRESS_MILLIS = 1_000L
    }
}
