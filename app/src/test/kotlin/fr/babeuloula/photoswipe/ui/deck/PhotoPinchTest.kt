package fr.babeuloula.photoswipe.ui.deck

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Pincement sur la photo de la carte du dessus, qui l'ouvre en grand en continuant le zoom (PS-190). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class PhotoPinchTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<DeckEvent>()

    private fun showDeck() {
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                PhotoSwipeTheme {
                    DeckScreen(
                        day = LocalDate.of(2026, 5, 14),
                        state = DeckUiState.Loaded(listOf(PREVIEW_CARD)),
                        onEvent = { events += it },
                    )
                }
            }
        }
    }

    /** Photo de la carte du dessus : le nœud qui l'ouvre en grand d'un appui. */
    private val topPhoto =
        SemanticsMatcher("opens full screen") {
            it.config.getOrNull(SemanticsActions.OnClick)?.label == "View full screen"
        }

    /** Pincement de [from] à [to] pixels d'écart entre les deux doigts, autour du centre. */
    private fun pinchOn(
        matcher: SemanticsMatcher,
        from: Float,
        to: Float,
    ) {
        composeRule.onNode(matcher).performTouchInput {
            pinch(
                start0 = center - Offset(from / 2, 0f),
                end0 = center - Offset(to / 2, 0f),
                start1 = center + Offset(from / 2, 0f),
                end1 = center + Offset(to / 2, 0f),
            )
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `pinching the top photo opens it full screen without deciding`() {
        showDeck()

        pinchOn(topPhoto, from = 100f, to = 400f)

        composeRule.onNodeWithTag(PHOTO_VIEWER_TAG).assertIsDisplayed()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `pinching the top photo smaller neither opens it nor decides`() {
        showDeck()

        pinchOn(topPhoto, from = 400f, to = 100f)

        composeRule.onNodeWithTag(PHOTO_VIEWER_TAG).assertDoesNotExist()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `keeps zooming the full screen photo until the fingers lift`() {
        val zoom = ZoomState()
        var started = 0
        var ended = 0
        val pinch = PhotoPinch(zoom = zoom, onStart = { started++ }, onEnd = { ended++ })
        composeRule.activity.setContent {
            Box(Modifier.size(300.dp).testTag(PINCH_AREA).pinchToOpen(pinch))
        }

        pinchOn(hasTestTag(PINCH_AREA), from = 100f, to = 400f)

        assertEquals(1, started)
        assertEquals(1, ended)
        // De 100 à 400 pixels d'écart : la photo est agrandie de près de 4 fois, sans rien perdre au départ.
        assertTrue("scale ${zoom.scale}", zoom.scale > 3.5f)
        assertFalse(zoom.offset.x.isNaN())
    }

    private companion object {
        const val PINCH_AREA = "pinch_area"
    }
}
