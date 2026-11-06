package fr.babeuloula.photoswipe.ui.deck

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.time.Duration

/**
 * Lecteur qui note les demandes ; la surface vidéo reçoit un vrai ExoPlayer, jamais préparé.
 * [playingUri] et [position] se règlent à la main : la lecture n'avance pas seule.
 */
internal class RecordingVideoPlayer(
    context: Context,
) : CardVideoPlayer {
    val calls = mutableListOf<String>()
    override val player: Player = ExoPlayer.Builder(context).build()
    override var playingUri: String? by mutableStateOf(null)
    override var endedUri: String? by mutableStateOf(null)
    override var muted: Boolean by mutableStateOf(true)

    override fun toggleSound() {
        calls += "sound"
        muted = !muted
    }

    override fun resumesImmediately(uri: String): Boolean = false

    var position: Duration = Duration.ZERO

    override fun play(uri: String) {
        calls += "play $uri"
    }

    override fun playByUser(uri: String) {
        calls += "play with sound $uri"
        muted = false
    }

    override fun position(uri: String): Duration = position

    override fun pauseByUser(uri: String) {
        calls += "pause $uri"
    }

    override fun stop(uri: String) {
        calls += "stop $uri"
    }

    override fun pause() {
        calls += "pause"
    }

    override fun resume() {
        calls += "resume"
    }

    override fun release() {
        player.release()
    }
}
