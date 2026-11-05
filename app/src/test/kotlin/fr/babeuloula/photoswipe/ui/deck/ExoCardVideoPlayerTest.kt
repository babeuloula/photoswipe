package fr.babeuloula.photoswipe.ui.deck

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Duration

/** Lecteur vidéo unique de la pile, sur un vrai ExoPlayer. */
@RunWith(RobolectricTestRunner::class)
class ExoCardVideoPlayerTest {
    private val codecSelector = SoftwareFallbackCodecSelector()
    private val videoPlayer =
        ExoCardVideoPlayer(ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build(), codecSelector)
    private val player get() = videoPlayer.player
    private val first = "content://media/external/video/media/1"
    private val second = "content://media/external/video/media/2"

    @After
    fun release() {
        videoPlayer.release()
    }

    @Test
    fun `plays muted and only once`() {
        assertEquals(0f, player.volume)
        assertEquals(Player.REPEAT_MODE_OFF, player.repeatMode)
    }

    @Test
    fun `stops at the end and offers to play from the start`() {
        videoPlayer.play(first)

        videoPlayer.onEnded()

        assertNull(videoPlayer.playingUri)
        assertFalse(player.playWhenReady)
        assertEquals(Duration.ZERO, videoPlayer.position(first))
    }

    @Test
    fun `does not restart an ended video back in the foreground`() {
        videoPlayer.play(first)
        videoPlayer.onEnded()

        videoPlayer.resume()

        assertFalse(player.playWhenReady)
    }

    @Test
    fun `replays an ended video from the badge`() {
        videoPlayer.play(first)
        videoPlayer.onEnded()

        videoPlayer.play(first)

        assertTrue(player.playWhenReady)
        assertEquals(first, videoPlayer.playingUri)
        assertEquals(0L, player.currentPosition)
    }

    @Test
    fun `plays the video of the top card`() {
        videoPlayer.play(first)

        assertEquals(
            first,
            player.currentMediaItem
                ?.localConfiguration
                ?.uri
                .toString(),
        )
        assertTrue(player.playWhenReady)
    }

    @Test
    fun `reuses the same player from one video to the next`() {
        videoPlayer.play(first)
        videoPlayer.play(second)

        assertEquals(1, player.mediaItemCount)
        assertEquals(
            second,
            player.currentMediaItem
                ?.localConfiguration
                ?.uri
                .toString(),
        )
    }

    @Test
    fun `keeps a video going when it is played again`() {
        videoPlayer.play(first)
        val item = player.currentMediaItem

        videoPlayer.play(first)

        assertTrue(item === player.currentMediaItem)
    }

    @Test
    fun `stops when the playing card leaves`() {
        videoPlayer.play(first)

        videoPlayer.stop(first)

        assertNull(player.currentMediaItem)
    }

    @Test
    fun `ignores the stop of a card that is no longer playing`() {
        videoPlayer.play(first)
        videoPlayer.play(second)

        videoPlayer.stop(first)

        assertEquals(
            second,
            player.currentMediaItem
                ?.localConfiguration
                ?.uri
                .toString(),
        )
    }

    @Test
    fun `pauses in the background and resumes in the foreground`() {
        videoPlayer.play(first)

        videoPlayer.pause()
        assertFalse(player.playWhenReady)

        videoPlayer.resume()
        assertTrue(player.playWhenReady)
    }

    @Test
    fun `does not resume when nothing was playing`() {
        videoPlayer.resume()

        assertFalse(player.playWhenReady)
    }

    @Test
    fun `tells which video is playing`() {
        assertNull(videoPlayer.playingUri)

        videoPlayer.play(first)
        assertEquals(first, videoPlayer.playingUri)

        videoPlayer.stop(first)
        assertNull(videoPlayer.playingUri)
    }

    @Test
    fun `stays paused after a pause from the badge, even back in the foreground`() {
        videoPlayer.play(first)

        videoPlayer.pauseByUser(first)
        videoPlayer.pause()
        videoPlayer.resume()

        assertFalse(player.playWhenReady)
        assertNull(videoPlayer.playingUri)
    }

    @Test
    fun `plays again from the badge after a pause`() {
        videoPlayer.play(first)
        videoPlayer.pauseByUser(first)

        videoPlayer.play(first)

        assertTrue(player.playWhenReady)
        assertEquals(first, videoPlayer.playingUri)
    }

    @Test
    fun `gives the position of the loaded video only`() {
        videoPlayer.play(first)

        assertEquals(Duration.ZERO, videoPlayer.position(first))
        assertNull(videoPlayer.position(second))
    }

    private fun error(code: Int) = PlaybackException("test", null, code)

    @Test
    fun `retries on a software decoder after a decoding failure`() {
        videoPlayer.play(first)

        videoPlayer.onError(error(PlaybackException.ERROR_CODE_DECODING_FAILED))

        assertTrue(codecSelector.preferSoftware)
        assertEquals(
            first,
            player.currentMediaItem
                ?.localConfiguration
                ?.uri
                .toString(),
        )
    }

    @Test
    fun `retries when the hardware decoder cannot start`() {
        videoPlayer.play(first)

        videoPlayer.onError(error(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED))

        assertTrue(codecSelector.preferSoftware)
    }

    @Test
    fun `does not retry for other errors`() {
        videoPlayer.play(first)

        videoPlayer.onError(error(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND))

        assertFalse(codecSelector.preferSoftware)
    }

    @Test
    fun `does not retry without a video`() {
        videoPlayer.onError(error(PlaybackException.ERROR_CODE_DECODING_FAILED))

        assertFalse(codecSelector.preferSoftware)
    }

    @Test
    fun `puts software decoders first only after a failure`() {
        val hardware = decoder("c2.vendor.avc.decoder", softwareOnly = false)
        val software = decoder("c2.android.avc.decoder", softwareOnly = true)
        val decoders = listOf(hardware, software)

        assertEquals(decoders, SoftwareFallbackCodecSelector.order(decoders, preferSoftware = false))
        assertEquals(listOf(software, hardware), SoftwareFallbackCodecSelector.order(decoders, preferSoftware = true))
    }

    @OptIn(UnstableApi::class)
    private fun decoder(
        name: String,
        softwareOnly: Boolean,
    ): MediaCodecInfo =
        MediaCodecInfo.newInstance(
            name,
            "video/avc",
            "video/avc",
            null,
            !softwareOnly,
            softwareOnly,
            !softwareOnly,
            false,
            false,
        )
}
