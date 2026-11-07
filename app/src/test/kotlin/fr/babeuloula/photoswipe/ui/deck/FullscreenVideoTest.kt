package fr.babeuloula.photoswipe.ui.deck

import android.content.pm.ActivityInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
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

/** Vidéo du dessus en plein écran (PS-119). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp")
class FullscreenVideoTest {
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

    /** Délai d'effacement recommandé par l'accessibilité : celui demandé, ou jamais avec TalkBack. */
    private var talkBack = false

    private val accessibility =
        object : AccessibilityManager {
            override fun calculateRecommendedTimeoutMillis(
                originalTimeoutMillis: Long,
                containsIcons: Boolean,
                containsText: Boolean,
                containsControls: Boolean,
            ): Long = if (talkBack) Long.MAX_VALUE else originalTimeoutMillis
        }

    private fun show() {
        composeRule.activity.setContent {
            CompositionLocalProvider(
                LocalImageLoader provides ImageLoader(composeRule.activity),
                LocalCardVideoPlayer provides videoPlayer,
                LocalAccessibilityManager provides accessibility,
            ) {
                PhotoSwipeTheme {
                    RotationPolicy {
                        DeckScreen(
                            day = LocalDate.of(2026, 5, 14),
                            state = DeckUiState.Loaded(listOf(video)),
                            onEvent = { events += it },
                        )
                    }
                }
            }
        }
    }

    /** Nœud décrit par [description] dans le plein écran (la carte, dessous, a les mêmes boutons). */
    private fun inFullscreen(description: String) =
        hasContentDescription(description) and hasAnyAncestor(hasTestTag(FULLSCREEN_VIDEO_TAG))

    /** La vidéo démarre (le faux lecteur ne lit rien de lui-même). */
    private fun startPlaying() {
        composeRule.runOnUiThread {
            videoPlayer.playingUri = video.uri
            Snapshot.sendApplyNotifications()
        }
        composeRule.waitForIdle()
    }

    private fun openFullscreen() {
        show()
        composeRule.onNodeWithContentDescription("Full screen").performClick()
    }

    @Test
    fun `opens the video full screen and lets the phone rotate only there`() {
        openFullscreen()

        composeRule.onNodeWithTag(FULLSCREEN_VIDEO_TAG).assertIsDisplayed()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER, composeRule.activity.requestedOrientation)

        composeRule.onNodeWithContentDescription("Exit full screen").performClick()

        composeRule.onNodeWithTag(FULLSCREEN_VIDEO_TAG).assertDoesNotExist()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, composeRule.activity.requestedOrientation)
        assertTrue(events.isEmpty())
    }

    @Test
    fun `plays, pauses and seeks from the full screen controls`() {
        openFullscreen()

        composeRule.onNodeWithTag(FULLSCREEN_VIDEO_TAG).assertIsDisplayed()
        composeRule.onNode(inFullscreen("Play video")).performClick()
        startPlaying()
        composeRule.onNode(inFullscreen("Pause video")).performClick()

        assertEquals(listOf("play with sound ${video.uri}", "pause ${video.uri}"), videoPlayer.calls)
        composeRule
            .onNode(
                hasTestTag(VIDEO_PROGRESS_TAG) and hasAnyAncestor(hasTestTag(FULLSCREEN_VIDEO_TAG)),
            ).assertIsDisplayed()
    }

    @Test
    fun `hides the controls a few seconds into playback and shows them again on tap`() {
        openFullscreen()
        startPlaying()
        composeRule.mainClock.autoAdvance = false

        composeRule.mainClock.advanceTimeBy(4_000)
        composeRule.onNodeWithContentDescription("Exit full screen").assertDoesNotExist()

        composeRule.onNodeWithTag(FULLSCREEN_VIDEO_TAG).performClick()
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithContentDescription("Exit full screen").assertIsDisplayed()
    }

    @Test
    fun `keeps the controls on screen with TalkBack`() {
        talkBack = true
        openFullscreen()
        startPlaying()
        composeRule.mainClock.autoAdvance = false

        composeRule.mainClock.advanceTimeBy(10_000)

        composeRule.onNodeWithContentDescription("Exit full screen").assertIsDisplayed()
    }

    @Test
    fun `stays full screen when the screen is recreated by a rotation`() {
        val restorer = SavedStateRestorer()
        composeRule.activity.setContent {
            restorer.Restorable {
                RotationPolicy {
                    CompositionLocalProvider(
                        LocalImageLoader provides ImageLoader(composeRule.activity),
                        LocalCardVideoPlayer provides videoPlayer,
                    ) {
                        PhotoSwipeTheme {
                            DeckScreen(
                                day = LocalDate.of(2026, 5, 14),
                                state = DeckUiState.Loaded(listOf(video)),
                                onEvent = { events += it },
                            )
                        }
                    }
                }
            }
        }
        composeRule.onNodeWithContentDescription("Full screen").performClick()
        composeRule.onNodeWithTag(FULLSCREEN_VIDEO_TAG).assertIsDisplayed()

        restorer.saveAndRestore(composeRule)

        composeRule.onNodeWithTag(FULLSCREEN_VIDEO_TAG).assertIsDisplayed()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER, composeRule.activity.requestedOrientation)
        composeRule.onNodeWithContentDescription("Exit full screen").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(FULLSCREEN_VIDEO_TAG).assertDoesNotExist()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, composeRule.activity.requestedOrientation)
    }
}
