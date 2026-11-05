package fr.babeuloula.photoswipe.ui.deck

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Lecteur vidéo de la pile (SPEC §4.3, §7) : une seule instance, passée de carte en carte. La
 * carte du dessus d'une vidéo le démarre ; elle l'arrête en quittant la pile.
 */
interface CardVideoPlayer {
    /** Lecteur à afficher dans la surface vidéo de la carte. */
    val player: Player

    /** Lit [uri] en boucle et en muet ; une vidéo déjà en cours continue sans recommencer. */
    fun play(uri: String)

    /** Arrête la lecture si c'est [uri] qui est en cours : la carte suivante a pu déjà démarrer. */
    fun stop(uri: String)

    /** Met en pause, quand l'écran passe en arrière-plan. */
    fun pause()

    /** Reprend la vidéo en cours, au retour au premier plan. */
    fun resume()

    /** Libère le lecteur, en quittant la pile. */
    fun release()
}

/** [CardVideoPlayer] sur Media3 ExoPlayer, muet et en boucle. */
class ExoCardVideoPlayer(
    private val exoPlayer: ExoPlayer,
) : CardVideoPlayer {
    init {
        exoPlayer.volume = 0f
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
    }

    override val player: Player get() = exoPlayer

    /** URI de la vidéo chargée, `null` si aucune. */
    private var currentUri: String? = null

    override fun play(uri: String) {
        if (uri != currentUri) {
            currentUri = uri
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
        }
        exoPlayer.playWhenReady = true
    }

    override fun stop(uri: String) {
        if (uri != currentUri) return
        currentUri = null
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    override fun pause() {
        exoPlayer.playWhenReady = false
    }

    override fun resume() {
        if (currentUri != null) exoPlayer.playWhenReady = true
    }

    override fun release() {
        currentUri = null
        exoPlayer.release()
    }

    companion object {
        /** Lecteur neuf, à libérer avec [release]. */
        fun create(context: Context): ExoCardVideoPlayer = ExoCardVideoPlayer(ExoPlayer.Builder(context).build())
    }
}

/**
 * Lecteur de la pile, fourni par `DeckRoute`. `null` dans les aperçus et les tests d'écran : les
 * vidéos y restent sur leur miniature.
 */
val LocalCardVideoPlayer = staticCompositionLocalOf<CardVideoPlayer?> { null }

/**
 * Lecteur de la pile, créé une fois pour l'écran : mis en pause en arrière-plan, repris au premier
 * plan, libéré quand l'écran est quitté.
 */
@Composable
fun rememberCardVideoPlayer(): CardVideoPlayer {
    val context = LocalContext.current
    val videoPlayer = remember { ExoCardVideoPlayer.create(context) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { videoPlayer.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { videoPlayer.resume() }
    DisposableEffect(videoPlayer) { onDispose { videoPlayer.release() } }
    return videoPlayer
}
