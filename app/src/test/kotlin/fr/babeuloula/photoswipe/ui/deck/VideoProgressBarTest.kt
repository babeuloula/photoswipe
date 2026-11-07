package fr.babeuloula.photoswipe.ui.deck

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate

/** Barre de progression de la vidéo du dessus (PS-118). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp")
class VideoProgressBarTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<DeckEvent>()
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

    private fun show(card: MediaCardUiState) {
        composeRule.activity.setContent {
            CompositionLocalProvider(
                LocalImageLoader provides ImageLoader(composeRule.activity),
                LocalCardVideoPlayer provides videoPlayer,
            ) {
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

    @Test
    fun `shows the progress of the top video with its time for TalkBack`() {
        videoPlayer.position = Duration.ofSeconds(10)
        show(video)

        composeRule
            .onNodeWithTag(VIDEO_PROGRESS_TAG)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "00:10 of 00:40"))
    }

    @Test
    fun `seeks where the bar is dragged without sorting the card`() {
        show(video)

        composeRule.onNodeWithTag(VIDEO_PROGRESS_TAG).performTouchInput {
            down(centerLeft + Offset(20f, 0f))
            repeat(10) { moveBy(Offset(width / 25f, 0f)) }
            up()
        }
        composeRule.waitForIdle()

        val seek = videoPlayer.calls.single { it.startsWith("seek ") }
        val millis = seek.substringAfterLast(' ').toLong()
        assertTrue("seek near the middle: $millis", millis in 15_000L..25_000L)
        assertTrue(events.none { it == DeckEvent.Keep || it == DeckEvent.MarkForDeletion })
    }

    @Test
    fun `shows no progress bar on a photo`() {
        show(PREVIEW_CARD)

        composeRule.onNodeWithTag(VIDEO_PROGRESS_TAG).assertDoesNotExist()
    }
}
