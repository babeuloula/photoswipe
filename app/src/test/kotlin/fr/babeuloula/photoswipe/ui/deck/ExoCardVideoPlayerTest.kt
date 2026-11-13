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
    fun `plays twice as fast while held, starting a paused video without turning the sound on`() {
        videoPlayer.setFastForward(first, fast = true)

        assertEquals(FAST_FORWARD_SPEED, player.playbackParameters.speed)
        assertTrue(player.playWhenReady)
        assertEquals(first, videoPlayer.playingUri)
        assertTrue(videoPlayer.muted)
        assertTrue(videoPlayer.fastForward)

        videoPlayer.setFastForward(first, fast = false)

        assertEquals(1f, player.playbackParameters.speed)
        assertTrue(player.playWhenReady)
        assertFalse(videoPlayer.fastForward)
    }

    @Test
    fun `goes back to normal speed when the video stops`() {
        videoPlayer.setFastForward(first, fast = true)

        videoPlayer.stop(first)

        assertEquals(1f, player.playbackParameters.speed)
        assertFalse(videoPlayer.fastForward)
    }

    @Test
    fun `stops at the end and offers to play from the start`() {
        videoPlayer.play(first)

        videoPlayer.events.onPlaybackStateChanged(Player.STATE_ENDED)

        assertNull(videoPlayer.playingUri)
        assertFalse(player.playWhenReady)
        assertEquals(Duration.ZERO, videoPlayer.position(first))
    }

    @Test
    fun `does not restart an ended video back in the foreground`() {
        videoPlayer.play(first)
        videoPlayer.events.onPlaybackStateChanged(Player.STATE_ENDED)

        videoPlayer.resume()

        assertFalse(player.playWhenReady)
    }

    @Test
    fun `replays an ended video from the badge`() {
        videoPlayer.play(first)
        videoPlayer.events.onPlaybackStateChanged(Player.STATE_ENDED)

        videoPlayer.play(first)

        assertTrue(player.playWhenReady)
        assertEquals(first, videoPlayer.playingUri)
        assertEquals(0L, player.currentPosition)
    }

    @Test
    fun `seeks within the playing video and keeps playing`() {
        videoPlayer.play(first)

        videoPlayer.seekTo(first, Duration.ofSeconds(12))

        assertEquals(12_000L, player.currentPosition)
        assertTrue(player.playWhenReady)
        assertEquals(first, videoPlayer.playingUri)
    }

    @Test
    fun `seeks within an ended video, which waits there to be played again`() {
        videoPlayer.play(first)
        videoPlayer.events.onPlaybackStateChanged(Player.STATE_ENDED)

        videoPlayer.seekTo(first, Duration.ofSeconds(3))

        assertNull(videoPlayer.endedUri)
        assertFalse(player.playWhenReady)
        assertEquals(Duration.ofSeconds(3), videoPlayer.position(first))
    }

    @Test
    fun `loads a video not yet played, paused at the chosen position`() {
        videoPlayer.seekTo(first, Duration.ofSeconds(7))

        assertEquals(
            first,
            player.currentMediaItem
                ?.localConfiguration
                ?.uri
                .toString(),
        )
        assertEquals(7_000L, player.currentPosition)
        assertFalse(player.playWhenReady)
        assertNull(videoPlayer.playingUri)
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

    @Test
    fun `starts muted and turns the sound on and off`() {
        assertTrue(videoPlayer.muted)

        videoPlayer.toggleSound()
        assertFalse(videoPlayer.muted)
        assertEquals(1f, player.volume)

        videoPlayer.toggleSound()
        assertTrue(videoPlayer.muted)
        assertEquals(0f, player.volume)
    }

    @Test
    fun `turns the sound on when the user plays a video`() {
        videoPlayer.playByUser(first)

        assertFalse(videoPlayer.muted)
        assertEquals(1f, player.volume)
        assertTrue(player.playWhenReady)
        assertEquals(first, videoPlayer.playingUri)
    }

    @Test
    fun `keeps the automatic playback muted`() {
        videoPlayer.play(first)

        assertTrue(videoPlayer.muted)
        assertEquals(0f, player.volume)
    }

    @Test
    fun `keeps the sound on for the next videos after the user played one`() {
        videoPlayer.playByUser(first)

        videoPlayer.play(second)

        assertEquals(1f, player.volume)
    }

    @Test
    fun `lets the sound be cut after the user played a video`() {
        videoPlayer.playByUser(first)

        videoPlayer.toggleSound()

        assertTrue(videoPlayer.muted)
        assertEquals(0f, player.volume)
    }

    @Test
    fun `keeps the sound setting for the next videos`() {
        videoPlayer.play(first)
        videoPlayer.toggleSound()

        videoPlayer.play(second)

        assertEquals(1f, player.volume)
    }

    @Test
    fun `resumes a recreated screen where it was, with the same sound`() {
        videoPlayer.play(first)
        videoPlayer.toggleSound()
        player.seekTo(SAVED_POSITION)
        val saved = VideoPlaybackState.fromList(videoPlayer.playbackState.toList())

        val restored = restoredFrom(saved)

        assertTrue(restored.resumesImmediately(first))
        assertFalse(restored.resumesImmediately(second))
        restored.play(first)
        assertEquals(SAVED_POSITION, restored.player.currentPosition)
        assertFalse(restored.muted)
        assertEquals(1f, restored.player.volume)
        assertFalse(restored.resumesImmediately(first))
        restored.release()
    }

    @Test
    fun `waits again for a video that was paused before the screen was recreated`() {
        videoPlayer.play(first)
        videoPlayer.pauseByUser(first)

        val restored = restoredFrom(videoPlayer.playbackState)

        assertFalse(restored.resumesImmediately(first))
        restored.release()
    }

    @Test
    fun `restores nothing when no video was loaded`() {
        val restored = restoredFrom(videoPlayer.playbackState)

        restored.play(first)

        assertEquals(0L, restored.player.currentPosition)
        assertTrue(restored.muted)
        restored.release()
    }

    private fun error(code: Int) = PlaybackException("test", null, code)

    private fun restoredFrom(state: VideoPlaybackState) =
        ExoCardVideoPlayer(ExoPlayer.Builder(RuntimeEnvironment.getApplication()).build(), restoredState = state)

    private companion object {
        /** Position enregistrée avant la recréation de l'écran. */
        const val SAVED_POSITION = 5_000L
    }

    @Test
    fun `retries on a software decoder after a decoding failure`() {
        videoPlayer.play(first)

        videoPlayer.events.onPlayerError(error(PlaybackException.ERROR_CODE_DECODING_FAILED))

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

        videoPlayer.events.onPlayerError(error(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED))

        assertTrue(codecSelector.preferSoftware)
    }

    @Test
    fun `does not retry for other errors`() {
        videoPlayer.play(first)

        videoPlayer.events.onPlayerError(error(PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND))

        assertFalse(codecSelector.preferSoftware)
    }

    @Test
    fun `stops showing the video as playing when it cannot be read`() {
        videoPlayer.play(first)

        videoPlayer.events.onPlayerError(error(PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED))

        assertNull(videoPlayer.playingUri)
        assertFalse(player.playWhenReady)
    }

    @Test
    fun `does not retry without a video`() {
        videoPlayer.events.onPlayerError(error(PlaybackException.ERROR_CODE_DECODING_FAILED))

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
