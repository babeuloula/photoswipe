package fr.babeuloula.photoswipe.ui.deck

import android.view.View
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Écran gardé allumé pendant la lecture d'une vidéo de la pile, et seulement pendant la lecture. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class KeepScreenOnWhilePlayingTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val videoPlayer by lazy { RecordingVideoPlayer(composeRule.activity) }

    private val shown = mutableStateOf(true)

    private lateinit var view: View

    @After
    fun releasePlayer() {
        composeRule.runOnUiThread { videoPlayer.release() }
    }

    private fun show() {
        composeRule.activity.setContent {
            view = LocalView.current
            if (shown.value) KeepScreenOnWhilePlaying(videoPlayer)
        }
        composeRule.waitForIdle()
    }

    private fun update(block: () -> Unit) {
        composeRule.runOnUiThread(block)
        composeRule.waitForIdle()
    }

    @Test
    fun `lets the screen sleep while no video plays`() {
        show()

        assertFalse(view.keepScreenOn)
    }

    @Test
    fun `keeps the screen on while a video plays`() {
        show()

        update { videoPlayer.playingUri = VIDEO }

        assertTrue(view.keepScreenOn)
    }

    @Test
    fun `lets the screen sleep again when the video pauses or ends`() {
        show()
        update { videoPlayer.playingUri = VIDEO }

        update { videoPlayer.playingUri = null }

        assertFalse(view.keepScreenOn)
    }

    @Test
    fun `lets the screen sleep again when the deck leaves the screen`() {
        show()
        update { videoPlayer.playingUri = VIDEO }

        update { shown.value = false }

        assertFalse(view.keepScreenOn)
    }

    private companion object {
        const val VIDEO = "content://media/external/video/media/1"
    }
}
