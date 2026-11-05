package fr.babeuloula.photoswipe.ui.deck

import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Cycle de vie du lecteur de la pile : créé avec l'écran, libéré quand il disparaît. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class RememberCardVideoPlayerTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `releases the player when the deck leaves the screen`() {
        val shown = mutableStateOf(true)
        var videoPlayer: CardVideoPlayer? = null
        composeRule.activity.setContent {
            if (shown.value) videoPlayer = rememberCardVideoPlayer()
        }
        composeRule.waitForIdle()
        val player = videoPlayer!!.player as ExoPlayer
        assertFalse(player.isReleased)

        composeRule.runOnUiThread { shown.value = false }
        composeRule.waitForIdle()

        assertTrue(player.isReleased)
    }
}
