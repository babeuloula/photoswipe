package fr.babeuloula.photoswipe.ui.deck

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import java.time.Duration

/**
 * Lecteur vidéo de la pile (SPEC §4.3, §7) : une seule instance, passée de carte en carte. La
 * carte du dessus d'une vidéo le démarre après un court délai ; elle l'arrête en quittant la pile.
 */
interface CardVideoPlayer {
    /** Lecteur à afficher dans la surface vidéo de la carte. */
    val player: Player

    /**
     * Vidéo en lecture, `null` si aucune ou en pause. Lue par Compose : la pastille de la carte se
     * met à jour quand elle change.
     */
    val playingUri: String?

    /** Lit [uri] en boucle et en muet ; une vidéo déjà en cours continue sans recommencer. */
    fun play(uri: String)

    /** Position de lecture de [uri], `null` si ce n'est pas la vidéo chargée. */
    fun position(uri: String): Duration?

    /** Pause demandée par l'utilisateur : la vidéo ne reprend pas seule au retour au premier plan. */
    fun pauseByUser(uri: String)

    /** Arrête la lecture si c'est [uri] qui est en cours : la carte suivante a pu déjà démarrer. */
    fun stop(uri: String)

    /** Met en pause, quand l'écran passe en arrière-plan. */
    fun pause()

    /** Reprend la vidéo en cours, au retour au premier plan. */
    fun resume()

    /** Libère le lecteur, en quittant la pile. */
    fun release()
}

/**
 * [CardVideoPlayer] sur Media3 ExoPlayer, muet et en boucle.
 *
 * Certains décodeurs matériels échouent sur des vidéos que le décodeur logiciel d'Android lit sans
 * peine (décodeur de l'émulateur, appareils capricieux). Au premier échec de décodage, le lecteur
 * bascule [codecSelector] sur les décodeurs logiciels et relance la vidéo, une seule fois.
 */
class ExoCardVideoPlayer(
    private val exoPlayer: ExoPlayer,
    private val codecSelector: SoftwareFallbackCodecSelector = SoftwareFallbackCodecSelector(),
) : CardVideoPlayer {
    init {
        exoPlayer.volume = 0f
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        exoPlayer.addListener(
            object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    onError(error)
                }
            },
        )
    }

    /** Relance la vidéo sur un décodeur logiciel après un échec de décodage ; sinon, laisse la miniature. */
    internal fun onError(error: PlaybackException) {
        if (error.errorCode !in DECODER_ERRORS || codecSelector.preferSoftware || currentUri == null) return
        codecSelector.preferSoftware = true
        exoPlayer.prepare()
    }

    override val player: Player get() = exoPlayer

    /** URI de la vidéo chargée, `null` si aucune. */
    private var currentUri: String? = null

    /** Vrai après une pause demandée par l'utilisateur, jusqu'à ce qu'il relance la vidéo. */
    private var pausedByUser = false

    override var playingUri: String? by mutableStateOf(null)
        private set

    override fun play(uri: String) {
        if (uri != currentUri) {
            currentUri = uri
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
        }
        pausedByUser = false
        exoPlayer.playWhenReady = true
        playingUri = uri
    }

    override fun position(uri: String): Duration? =
        if (uri == currentUri) Duration.ofMillis(exoPlayer.currentPosition) else null

    override fun pauseByUser(uri: String) {
        if (uri != currentUri) return
        pausedByUser = true
        exoPlayer.playWhenReady = false
        playingUri = null
    }

    override fun stop(uri: String) {
        if (uri != currentUri) return
        currentUri = null
        pausedByUser = false
        playingUri = null
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    override fun pause() {
        exoPlayer.playWhenReady = false
        playingUri = null
    }

    override fun resume() {
        if (currentUri == null || pausedByUser) return
        exoPlayer.playWhenReady = true
        playingUri = currentUri
    }

    override fun release() {
        currentUri = null
        playingUri = null
        exoPlayer.release()
    }

    companion object {
        /** Erreurs de décodeur qui justifient un nouvel essai sur un décodeur logiciel. */
        private val DECODER_ERRORS =
            setOf(PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, PlaybackException.ERROR_CODE_DECODING_FAILED)

        /** Lecteur neuf, à libérer avec [release]. */
        @OptIn(UnstableApi::class)
        fun create(context: Context): ExoCardVideoPlayer {
            val codecSelector = SoftwareFallbackCodecSelector()
            val renderers =
                DefaultRenderersFactory(context)
                    .setMediaCodecSelector(codecSelector)
                    .setEnableDecoderFallback(true)
            return ExoCardVideoPlayer(ExoPlayer.Builder(context, renderers).build(), codecSelector)
        }
    }
}

/**
 * Choix des décodeurs de Media3, avec les décodeurs logiciels en tête une fois [preferSoftware]
 * activé. Par défaut, l'ordre de Media3 est gardé (matériel d'abord, plus économe).
 */
@OptIn(UnstableApi::class)
class SoftwareFallbackCodecSelector : MediaCodecSelector {
    /** Vrai après un échec de décodage : les décodeurs logiciels passent devant. */
    @Volatile
    var preferSoftware: Boolean = false

    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean,
    ): List<MediaCodecInfo> {
        val decoders =
            MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder,
            )
        return order(decoders, preferSoftware)
    }

    companion object {
        /** [decoders] dans l'ordre de Media3, ou décodeurs logiciels d'abord si [preferSoftware]. */
        fun order(
            decoders: List<MediaCodecInfo>,
            preferSoftware: Boolean,
        ): List<MediaCodecInfo> = if (preferSoftware) decoders.sortedByDescending { it.softwareOnly } else decoders
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
