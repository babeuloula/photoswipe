package fr.babeuloula.photoswipe.ui.deck

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
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
import java.time.LocalDate

/** Photo animée sur la carte de la pile (PS-167) : pastille, bouton, lecture unique à la demande. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp")
class MotionPhotoCardTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val motionVideo = MotionVideoRange(PREVIEW_CARD.uri, 1_000, 500).toUri()
    private val motionPhoto = PREVIEW_CARD.copy(motionVideo = motionVideo)

    private val videoPlayer by lazy { RecordingVideoPlayer(composeRule.activity) }

    @After
    fun releasePlayer() {
        videoPlayer.release()
    }

    private fun show(
        card: MediaCardUiState = motionPhoto,
        player: CardVideoPlayer? = videoPlayer,
    ) {
        composeRule.activity.setContent {
            CompositionLocalProvider(
                LocalImageLoader provides ImageLoader(composeRule.activity),
                LocalCardVideoPlayer provides player,
            ) {
                PhotoSwipeTheme {
                    DeckScreen(day = LocalDate.of(2026, 5, 14), state = DeckUiState.Loaded(listOf(card)), onEvent = {})
                }
            }
        }
    }

    @Test
    fun `shows the motion photo chip and never plays on its own`() {
        show()

        composeRule.onNodeWithContentDescription("Motion photo", useUnmergedTree = true).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(AUTOPLAY_DELAY.toMillis() * 2)

        assertTrue(videoPlayer.calls.none { it.startsWith("play") })
    }

    @Test
    fun `plays the motion once with the button, then stops it to show the still photo again`() {
        show()

        composeRule.onNodeWithContentDescription("Play motion photo").performClick()
        assertEquals("play $motionVideo", videoPlayer.calls.last())

        videoPlayer.playingUri = motionVideo
        composeRule.onNodeWithContentDescription("Stop motion photo").assertIsDisplayed()

        videoPlayer.playingUri = null
        videoPlayer.endedUri = motionVideo
        composeRule.waitForIdle()
        assertEquals("stop $motionVideo", videoPlayer.calls.last())
        composeRule.onNodeWithContentDescription("Play motion photo").assertIsDisplayed()
    }

    @Test
    fun `stops the motion with the button while it plays`() {
        show()
        videoPlayer.playingUri = motionVideo

        composeRule.onNodeWithContentDescription("Stop motion photo").performClick()

        assertEquals("stop $motionVideo", videoPlayer.calls.last())
    }

    @Test
    fun `an ordinary photo has neither chip nor button`() {
        show(card = PREVIEW_CARD)

        composeRule.onNodeWithContentDescription("Motion photo", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Play motion photo").assertDoesNotExist()
    }

    @Test
    fun `without the deck player, the chip stays but there is no button`() {
        show(player = null)

        composeRule.onNodeWithContentDescription("Motion photo", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Play motion photo").assertDoesNotExist()
    }
}
