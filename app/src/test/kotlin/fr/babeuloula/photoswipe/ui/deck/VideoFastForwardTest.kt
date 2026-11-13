package fr.babeuloula.photoswipe.ui.deck

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
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
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate

/** Vidéo de la carte du dessus lue en ×2 tant que le doigt reste posé (PS-161). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp")
class VideoFastForwardTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val video =
        PREVIEW_CARD.copy(
            uri = "content://media/external/video/media/2",
            type = MediaType.VIDEO,
            duration = Duration.ofSeconds(45),
        )

    private val videoPlayer by lazy { RecordingVideoPlayer(composeRule.activity) }

    @After
    fun releasePlayer() {
        videoPlayer.release()
    }

    private fun show() {
        composeRule.activity.setContent {
            CompositionLocalProvider(
                LocalImageLoader provides ImageLoader(composeRule.activity),
                LocalCardVideoPlayer provides videoPlayer,
            ) {
                PhotoSwipeTheme {
                    DeckScreen(day = LocalDate.of(2026, 5, 14), state = DeckUiState.Loaded(listOf(video)), onEvent = {})
                }
            }
        }
    }

    @Test
    fun `plays the video twice as fast while the finger stays on it, without pausing it`() {
        show()

        composeRule.onNodeWithContentDescription("Play video").performTouchInput {
            down(center)
            advanceEventTime(LONG_PRESS_MILLIS)
            move()
        }
        // La puce est dans la zone du bouton de lecture, dont TalkBack lit aussi l'annonce.
        composeRule.onNodeWithTag(FAST_FORWARD_CHIP_TAG, useUnmergedTree = true).assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Playing twice as fast", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Play video").performTouchInput { up() }

        assertTrue("fast ${video.uri}" in videoPlayer.calls)
        assertEquals("normal speed ${video.uri}", videoPlayer.calls.last())
        assertTrue(videoPlayer.calls.none { it.startsWith("pause") || it.startsWith("play with sound") })
        composeRule.onNodeWithTag(FAST_FORWARD_CHIP_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `switches the speed with the TalkBack action`() {
        show()

        composeRule
            .onNodeWithContentDescription("Play video")
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .single { it.label == "Play twice as fast" }
            .action()

        assertEquals("fast ${video.uri}", videoPlayer.calls.last())
    }

    private companion object {
        /** Plus long que le délai d'un appui long. */
        const val LONG_PRESS_MILLIS = 1_000L
    }
}
