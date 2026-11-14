package fr.babeuloula.photoswipe.ui.deck

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate

/** Repère, pastilles et boutons de la carte effacés pendant la lecture de sa vidéo. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp")
class VideoCardDetailsTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val videoPlayer by lazy { RecordingVideoPlayer(composeRule.activity) }

    private val video =
        PREVIEW_CARD.copy(
            type = MediaType.VIDEO,
            uri = "content://media/external/video/media/1",
            duration = Duration.ofSeconds(40),
        )

    @After
    fun release() {
        videoPlayer.release()
    }

    private fun show() {
        composeRule.activity.setContent {
            CompositionLocalProvider(
                LocalImageLoader provides ImageLoader(composeRule.activity),
                LocalCardVideoPlayer provides videoPlayer,
            ) {
                PhotoSwipeTheme {
                    DeckScreen(
                        day = LocalDate.of(2026, 5, 14),
                        state = DeckUiState.Loaded(listOf(video)),
                        onEvent = {},
                    )
                }
            }
        }
    }

    @Test
    fun `keeps the details shown while the video waits to play`() {
        composeRule.mainClock.autoAdvance = false
        show()

        composeRule.mainClock.advanceTimeBy(AUTOPLAY_DELAY.toMillis() - 500)

        composeRule.onNodeWithTag(VIDEO_PROGRESS_TAG).assertIsDisplayed()
    }

    @Test
    fun `hides the details a few seconds into the playback, and shows them again on pause`() {
        composeRule.mainClock.autoAdvance = false
        show()
        composeRule.mainClock.advanceTimeBy(AUTOPLAY_DELAY.toMillis() + 500)
        videoPlayer.playingUri = video.uri
        composeRule.mainClock.advanceTimeBy(500)
        // Lecture en cours : tout reste affiché quelques secondes.
        composeRule.onNodeWithTag(VIDEO_PROGRESS_TAG).assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(CONTROLS_VISIBLE_MILLIS + 1_000)
        composeRule.onNodeWithTag(VIDEO_PROGRESS_TAG).assertDoesNotExist()

        videoPlayer.playingUri = null
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.onNodeWithTag(VIDEO_PROGRESS_TAG).assertIsDisplayed()
    }
}
