package fr.babeuloula.photoswipe.ui.deck

import android.content.pm.ActivityInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.common.RotationPolicy
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate

/** Photo ouverte en grand depuis la pile (PS-117). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class PhotoViewerTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<DeckEvent>()

    private val restorer = SavedStateRestorer()

    private fun show(card: MediaCardUiState) {
        composeRule.activity.setContent {
            restorer.Restorable {
                RotationPolicy {
                    CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                        PhotoSwipeTheme {
                            DeckScreen(
                                day = LocalDate.of(2026, 5, 14),
                                state = DeckUiState.Loaded(listOf(card)),
                                onEvent = { events += it },
                            )
                        }
                    }
                }
            }
        }
    }

    private val orientation get() = composeRule.activity.requestedOrientation

    /** Nœud dont l'action d'appui porte le libellé [label]. */
    private fun hasClickLabel(label: String) =
        SemanticsMatcher("click label $label") { it.config.getOrNull(SemanticsActions.OnClick)?.label == label }

    @Test
    fun `opens the top photo full screen on tap and closes it without deciding`() {
        show(PREVIEW_CARD)

        composeRule.onNode(hasClickLabel("View full screen")).performClick()

        composeRule.onNodeWithTag(PHOTO_VIEWER_TAG).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close").performClick()
        composeRule.onNodeWithTag(PHOTO_VIEWER_TAG).assertDoesNotExist()
        assertTrue(events.isEmpty())
    }

    @Test
    fun `lets the phone rotate only while the photo is open`() {
        show(PREVIEW_CARD)
        composeRule.waitForIdle()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, orientation)

        composeRule.onNode(hasClickLabel("View full screen")).performClick()
        composeRule.waitForIdle()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER, orientation)

        composeRule.onNodeWithContentDescription("Close").performClick()
        composeRule.waitForIdle()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, orientation)
    }

    @Test
    fun `keeps the photo open when a rotation recreates the screen`() {
        show(PREVIEW_CARD)
        composeRule.onNode(hasClickLabel("View full screen")).performClick()

        restorer.saveAndRestore(composeRule)

        composeRule.onNodeWithTag(PHOTO_VIEWER_TAG).assertIsDisplayed()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER, orientation)
    }

    @Test
    fun `does not open a video full screen on tap`() {
        show(PREVIEW_CARD.copy(type = MediaType.VIDEO, duration = Duration.ofSeconds(5)))

        composeRule.onNode(hasClickLabel("View full screen")).assertDoesNotExist()
    }
}
