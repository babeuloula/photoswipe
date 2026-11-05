package fr.babeuloula.photoswipe.ui.deck

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Pile composée seule dans `MainActivity`, avec des états fixés (langue anglaise de Robolectric). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class DeckScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<DeckEvent>()
    private var backs = 0

    /** Affiche [initial] ; l'état renvoyé permet de le faire évoluer sans recomposer tout l'écran. */
    private fun show(initial: DeckUiState): MutableState<DeckUiState> {
        val state = mutableStateOf(initial)
        composeRule.activity.setContent {
            CompositionLocalProvider(
                LocalImageLoader provides ImageLoader(composeRule.activity),
                LocalHapticFeedback provides recordingHaptics,
            ) {
                PhotoSwipeTheme {
                    DeckScreen(
                        day = LocalDate.of(2026, 5, 14),
                        state = state.value,
                        onEvent = { events += it },
                        onBack = { backs++ },
                    )
                }
            }
        }
        return state
    }

    /** Retours haptiques demandés pendant le test. */
    private val haptics = mutableListOf<HapticFeedbackType>()

    private val recordingHaptics =
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                haptics += hapticFeedbackType
            }
        }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    @Test
    fun `shows the memory label and the file size of the top card`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD, PREVIEW_CARD.copy(yearsAgo = 5))))

        composeRule.onNodeWithText("3 years ago · May 14, 2023").assertIsDisplayed()
        composeRule.onNodeWithText("3.5 MB").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Photo taken on May 14, 2023").assertIsDisplayed()
    }

    @Test
    fun `describes a video as a video`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(type = MediaType.VIDEO))))

        composeRule.onNodeWithContentDescription("Video taken on May 14, 2023").assertIsDisplayed()
    }

    @Test
    fun `says when the image cannot be opened`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(uri = "content://media/external/images/media/404"))))

        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.deck_media_unavailable)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("3 years ago · May 14, 2023").assertIsDisplayed()
    }

    @Test
    fun `shows the day in the title`() {
        show(DeckUiState.Loading)

        composeRule
            .onNodeWithText(
                composeRule.activity.getString(R.string.deck_title, "May 14, 2026"),
            ).assertIsDisplayed()
    }

    @Test
    fun `finishes and goes back from the top bar`() {
        show(DeckUiState.Loading)

        composeRule.onNodeWithText(text(R.string.deck_finish)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()

        assertEquals(listOf<DeckEvent>(DeckEvent.Finish), events)
        assertEquals(1, backs)
    }

    @Test
    fun `says when nothing is left to sort`() {
        show(DeckUiState.Empty())

        composeRule.onNodeWithText(text(R.string.deck_empty)).assertIsDisplayed()
    }

    @Test
    fun `retries after an error`() {
        show(DeckUiState.Error)

        composeRule.onNodeWithText(text(R.string.deck_error)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.deck_retry)).performClick()

        assertEquals(listOf<DeckEvent>(DeckEvent.Retry), events)
    }

    private fun card() = composeRule.onNodeWithContentDescription("Photo taken on May 14, 2023")

    @Test
    fun `keeps the card when swiped right past the threshold`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        card().performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assertEquals(listOf<DeckEvent>(DeckEvent.Keep), events)
    }

    @Test
    fun `brings the card back to the center after a short swipe`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))
        val start = card().fetchSemanticsNode().boundsInRoot

        card().performTouchInput { swipeRight(startX = centerX, endX = centerX + width * SWIPE_THRESHOLD / 2) }
        composeRule.waitForIdle()

        assertTrue(events.isEmpty())
        assertEquals(start, card().fetchSemanticsNode().boundsInRoot)
    }

    @Test
    fun `marks the card for deletion when swiped left past the threshold`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        card().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals(listOf<DeckEvent>(DeckEvent.MarkForDeletion), events)
    }

    @Test
    fun `brings the card back to the center after a short swipe to the left`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))
        val start = card().fetchSemanticsNode().boundsInRoot

        card().performTouchInput { swipeLeft(startX = centerX, endX = centerX - width * SWIPE_THRESHOLD / 2) }
        composeRule.waitForIdle()

        assertTrue(events.isEmpty())
        assertEquals(start, card().fetchSemanticsNode().boundsInRoot)
    }

    @Test
    fun `shows a green indicator while dragging right and vibrates once past the threshold`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        card().performTouchInput {
            down(center)
            moveBy(Offset(width * 0.2f, 0f))
        }
        composeRule.onNodeWithTag(KEEP_INDICATOR_TAG).assertExists()
        composeRule.onNodeWithTag(DELETE_INDICATOR_TAG).assertDoesNotExist()
        assertTrue(haptics.isEmpty())

        card().performTouchInput {
            moveBy(Offset(width * 0.3f, 0f))
            moveBy(Offset(width * 0.1f, 0f))
        }
        assertEquals(listOf(HapticFeedbackType.GestureThresholdActivate), haptics)

        card().performTouchInput { up() }
    }

    @Test
    fun `shows a red indicator while dragging left and vibrates past the threshold`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        card().performTouchInput {
            down(center)
            moveBy(Offset(-width * 0.6f, 0f))
        }

        composeRule.onNodeWithTag(DELETE_INDICATOR_TAG).assertExists()
        composeRule.onNodeWithTag(KEEP_INDICATOR_TAG).assertDoesNotExist()
        assertEquals(listOf(HapticFeedbackType.GestureThresholdActivate), haptics)
        card().performTouchInput { up() }
    }

    @Test
    fun `shows no indicator while the card springs back to the center`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))
        card().performTouchInput {
            down(center)
            moveBy(Offset(width * 0.25f, 0f))
        }
        composeRule.onNodeWithTag(KEEP_INDICATOR_TAG).assertExists()

        composeRule.mainClock.autoAdvance = false
        card().performTouchInput { up() }
        repeat(BOUNCE_FRAMES) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onNodeWithTag(KEEP_INDICATOR_TAG).assertDoesNotExist()
            composeRule.onNodeWithTag(DELETE_INDICATOR_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `keeps the card with the keep button, like a swipe right`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithContentDescription(text(R.string.deck_keep)).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<DeckEvent>(DeckEvent.Keep), events)
    }

    @Test
    fun `marks the card with the delete button, like a swipe left`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithContentDescription(text(R.string.deck_mark_for_deletion)).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<DeckEvent>(DeckEvent.MarkForDeletion), events)
    }

    @Test
    fun `animates the card out like a swipe before keeping it from the button`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))
        val start = card().fetchSemanticsNode().boundsInRoot
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithContentDescription(text(R.string.deck_keep)).performClick()
        repeat(EXIT_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }

        composeRule.onNodeWithTag(KEEP_INDICATOR_TAG).assertExists()
        assertTrue(card().fetchSemanticsNode().boundsInRoot.left > start.left)
        assertTrue(events.isEmpty())

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals(listOf<DeckEvent>(DeckEvent.Keep), events)
    }

    @Test
    fun `animates the card out to the left before marking it from the button`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))
        val start = card().fetchSemanticsNode().boundsInRoot
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithContentDescription(text(R.string.deck_mark_for_deletion)).performClick()
        repeat(EXIT_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }

        composeRule.onNodeWithTag(DELETE_INDICATOR_TAG).assertExists()
        assertTrue(card().fetchSemanticsNode().boundsInRoot.left < start.left)

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals(listOf<DeckEvent>(DeckEvent.MarkForDeletion), events)
    }

    @Test
    fun `ignores a second tap while the card is leaving`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithContentDescription(text(R.string.deck_keep)).performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithContentDescription(text(R.string.deck_mark_for_deletion)).performClick()

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals(listOf<DeckEvent>(DeckEvent.Keep), events)
    }

    @Test
    fun `undoes from the undo button when a decision can be undone`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD), canUndo = true))

        composeRule.onNodeWithContentDescription(text(R.string.deck_undo)).assertIsEnabled().performClick()

        assertEquals(listOf<DeckEvent>(DeckEvent.Undo), events)
    }

    @Test
    fun `disables the undo button when there is nothing to undo`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD), canUndo = false))

        composeRule.onNodeWithContentDescription(text(R.string.deck_undo)).assertIsNotEnabled()
    }

    @Test
    fun `offers undo on an emptied deck`() {
        show(DeckUiState.Empty(canUndo = true))

        composeRule.onNodeWithContentDescription(text(R.string.deck_undo)).performClick()

        assertEquals(listOf<DeckEvent>(DeckEvent.Undo), events)
    }

    @Test
    fun `brings an undone card back from the side it left`() {
        val state = show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))
        val center = card().fetchSemanticsNode().positionInRoot
        composeRule.mainClock.autoAdvance = false

        state.value = DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(id = MediaId(2))), returningFrom = CardSide.RIGHT)
        // Les bornes sont coupées au bord de l'écran : on compare la position, qui suit la translation.
        // On avance image par image jusqu'à ce que la nouvelle carte soit posée, hors du centre.
        var frames = 0
        while (card().fetchSemanticsNode().positionInRoot == center && frames < MAX_FRAMES) {
            composeRule.mainClock.advanceTimeByFrame()
            frames++
        }
        assertTrue(card().fetchSemanticsNode().positionInRoot.x > center.x)

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals(center, card().fetchSemanticsNode().positionInRoot)
    }

    @Test
    fun `opens the menu when the card is swiped up`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        card().performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        assertEquals(listOf<DeckEvent>(DeckEvent.OpenMenu), events)
    }

    @Test
    fun `shows a blue indicator while dragging up and vibrates past the menu threshold`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        card().performTouchInput {
            down(center)
            moveBy(Offset(0f, -height * 0.4f))
        }

        composeRule.onNodeWithTag(FAVORITE_INDICATOR_TAG).assertExists()
        composeRule.onNodeWithTag(KEEP_INDICATOR_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(DELETE_INDICATOR_TAG).assertDoesNotExist()
        assertEquals(listOf(HapticFeedbackType.GestureThresholdActivate), haptics)

        card().performTouchInput { up() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(FAVORITE_INDICATOR_TAG).assertDoesNotExist()
    }

    @Test
    fun `does not open the menu on a short swipe up`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        card().performTouchInput { swipeUp(startY = centerY, endY = centerY - height * MENU_THRESHOLD / 2) }
        composeRule.waitForIdle()

        assertTrue(events.isEmpty())
    }

    @Test
    fun `opens the menu from the menu button`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithContentDescription(text(R.string.deck_menu)).performClick()

        assertEquals(listOf<DeckEvent>(DeckEvent.OpenMenu), events)
    }

    @Test
    fun `sends the card up and favorites it from the menu`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD), menuOpen = true))

        composeRule.onNodeWithText(text(R.string.deck_favorite)).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(DeckEvent.CloseMenu, DeckEvent.Favorite), events)
    }

    @Test
    fun `shows no menu while it is closed`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithText(text(R.string.deck_favorite)).assertDoesNotExist()
    }

    @Test
    fun `gives the decision buttons accessible touch targets`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD), canUndo = true))

        listOf(
            R.string.deck_keep,
            R.string.deck_mark_for_deletion,
            R.string.deck_undo,
            R.string.deck_menu,
        ).forEach { label ->
            composeRule
                .onNodeWithContentDescription(text(label))
                .assertHasClickAction()
                .assertIsEnabled()
                .assertWidthIsAtLeast(MIN_TOUCH_TARGET)
                .assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        }
    }

    @Test
    @Config(qualifiers = "fr")
    fun `describes the decision buttons in French`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithContentDescription("Garder").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Marquer pour suppression").assertIsDisplayed()
    }

    @Test
    fun `hides the decision buttons when there is no card`() {
        show(DeckUiState.Empty())

        composeRule.onNodeWithContentDescription(text(R.string.deck_keep)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(text(R.string.deck_mark_for_deletion)).assertDoesNotExist()
    }

    @Test
    fun `shows no indicator when the card is at rest`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithTag(KEEP_INDICATOR_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(DELETE_INDICATOR_TAG).assertDoesNotExist()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L

        /** Cible tactile minimale de Material 3 (SPEC §8). */
        val MIN_TOUCH_TARGET = 48.dp

        /** Images observées pendant le retour au centre, rebond compris (environ une demi-seconde). */
        const val BOUNCE_FRAMES = 40

        /** Quelques images après un tap : la carte a commencé à sortir sans avoir fini. */
        const val EXIT_FRAMES = 3

        /** Nombre maximal d'images attendues pour voir apparaître une carte annulée. */
        const val MAX_FRAMES = 20
    }
}
