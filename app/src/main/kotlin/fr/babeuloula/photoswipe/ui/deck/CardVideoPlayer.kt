package fr.babeuloula.photoswipe.ui.deck

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
@Suppress("TooManyFunctions") // Une fonction par action de l'utilisateur ou du cycle de vie de l'écran.
interface CardVideoPlayer {
    /** Lecteur à afficher dans la surface vidéo de la carte. */
    val player: Player

    /**
     * Vidéo en lecture, `null` si aucune ou en pause. Lue par Compose : la pastille de la carte se
     * met à jour quand elle change.
     */
    val playingUri: String?

    /** Vidéo arrivée à la fin et pas encore relancée, `null` si aucune. Lue par Compose. */
    val endedUri: String?

    /** Vrai tant que le son est coupé ; il l'est au départ (SPEC §4.3). Lu par Compose. */
    val muted: Boolean

    /** Vrai pendant la lecture accélérée du maintien du doigt (PS-161). Lu par Compose. */
    val fastForward: Boolean

    /** Active ou coupe le son ; le réglage vaut pour les vidéos suivantes de la pile. */
    fun toggleSound()

    /**
     * Lit [uri] en ×2 tant que [fast] est vrai (maintien du doigt, PS-161) : une vidéo en pause ou
     * pas encore démarrée se lance, sans toucher au son ; [fast] faux revient à la vitesse normale.
     */
    fun setFastForward(
        uri: String,
        fast: Boolean,
    )

    /**
     * Vrai si [uri] était en lecture avant que l'écran soit recréé : elle reprend tout de suite,
     * à sa position, sans attendre le délai de lecture automatique.
     */
    fun resumesImmediately(uri: String): Boolean

    /**
     * Lit [uri] une fois, lecture automatique comprise : muette au départ, puis avec le dernier
     * réglage du son choisi par l'utilisateur. Une vidéo en pause reprend là où elle était ; une vidéo arrivée
     * à la fin repart du début.
     */
    fun play(uri: String)

    /**
     * Lecture lancée par l'utilisateur (bouton ▶) : comme [play], mais avec le son, qu'il peut
     * encore couper. Le son reste activé pour les vidéos suivantes (SPEC §4.3).
     */
    fun playByUser(uri: String)

    /** Position de lecture de [uri], `null` si ce n'est pas la vidéo chargée. */
    fun position(uri: String): Duration?

    /** Pause demandée par l'utilisateur : la vidéo ne reprend pas seule au retour au premier plan. */
    fun pauseByUser(uri: String)

    /**
     * Déplace la lecture de [uri] à [position] (barre de progression) : une vidéo en lecture
     * continue de là ; en pause ou terminée, elle y attend que l'utilisateur la relance. Une vidéo
     * pas encore chargée l'est, en pause, à cette position.
     */
    fun seekTo(
        uri: String,
        position: Duration,
    )

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
 * [CardVideoPlayer] sur Media3 ExoPlayer, muet et lu une seule fois : à la fin, la vidéo s'arrête
 * et ne repart que si l'utilisateur la relance.
 *
 * Certains décodeurs matériels échouent sur des vidéos que le décodeur logiciel d'Android lit sans
 * peine (décodeur de l'émulateur, appareils capricieux). Au premier échec de décodage, le lecteur
 * bascule [codecSelector] sur les décodeurs logiciels et relance la vidéo, une seule fois.
 */
class ExoCardVideoPlayer(
    private val exoPlayer: ExoPlayer,
    private val codecSelector: SoftwareFallbackCodecSelector = SoftwareFallbackCodecSelector(),
    restoredState: VideoPlaybackState? = null,
) : CardVideoPlayer {
    /**
     * Réactions aux événements du lecteur. Après un échec de décodage, la vidéo est relancée sur un
     * décodeur logiciel ; à la fin de la vidéo, elle s'arrête et la carte propose de la relancer.
     */
    internal val events =
        object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (error.errorCode !in DECODER_ERRORS || codecSelector.preferSoftware || currentUri == null) return
                codecSelector.preferSoftware = true
                exoPlayer.prepare()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState != Player.STATE_ENDED) return
                endedUri = currentUri
                exoPlayer.playWhenReady = false
                playingUri = null
            }
        }

    init {
        exoPlayer.volume = 0f
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
        exoPlayer.addListener(events)
    }

    override val player: Player get() = exoPlayer

    /** URI de la vidéo chargée, `null` si aucune. */
    private var currentUri: String? = null

    /** Vrai après une pause demandée par l'utilisateur, jusqu'à ce qu'il relance la vidéo. */
    private var pausedByUser = false

    override var endedUri: String? by mutableStateOf(null)
        private set

    override var muted: Boolean by mutableStateOf(true)
        private set

    /** Lecture à reprendre après la recréation de l'écran, consommée par le prochain [play] de sa vidéo. */
    private var pendingResume: VideoPlaybackState? = null

    override var fastForward: Boolean by mutableStateOf(false)
        private set

    override fun setFastForward(
        uri: String,
        fast: Boolean,
    ) {
        if (fast && playingUri != uri) play(uri)
        if (!fast && uri != currentUri) return
        exoPlayer.setPlaybackSpeed(if (fast) FAST_FORWARD_SPEED else 1f)
        fastForward = fast
    }

    override fun toggleSound() {
        muted = !muted
        exoPlayer.volume = if (muted) 0f else 1f
    }

    override fun resumesImmediately(uri: String): Boolean = pendingResume?.let { it.uri == uri && it.playing } ?: false

    /** État à garder si l'écran est recréé (changement de thème, de langue…). */
    val playbackState: VideoPlaybackState
        get() =
            VideoPlaybackState(
                uri = currentUri,
                positionMillis = if (ended) 0L else exoPlayer.currentPosition,
                muted = muted,
                playing = playingUri != null,
            )

    // Reprise de l'état d'un lecteur précédent : le son tout de suite, la vidéo et sa position au
    // prochain play de cette vidéo.
    init {
        restoredState?.let { state ->
            if (muted != state.muted) toggleSound()
            pendingResume = state.takeIf { it.uri != null }
        }
    }

    /** Vrai quand la vidéo chargée est arrivée à la fin, jusqu'à ce qu'elle soit relancée. */
    private val ended: Boolean get() = endedUri != null && endedUri == currentUri

    override var playingUri: String? by mutableStateOf(null)
        private set

    override fun play(uri: String) {
        if (uri != currentUri) {
            currentUri = uri
            val resumeAt = pendingResume?.takeIf { it.uri == uri }?.positionMillis ?: 0L
            pendingResume = null
            exoPlayer.setMediaItem(MediaItem.fromUri(uri), resumeAt)
            exoPlayer.prepare()
        } else if (ended) {
            exoPlayer.seekTo(0)
        }
        endedUri = null
        pausedByUser = false
        exoPlayer.playWhenReady = true
        playingUri = uri
    }

    override fun playByUser(uri: String) {
        if (muted) toggleSound()
        play(uri)
    }

    override fun position(uri: String): Duration? =
        when {
            uri != currentUri -> null
            ended -> Duration.ZERO
            else -> Duration.ofMillis(exoPlayer.currentPosition)
        }

    override fun pauseByUser(uri: String) {
        if (uri != currentUri) return
        pausedByUser = true
        exoPlayer.playWhenReady = false
        playingUri = null
    }

    override fun seekTo(
        uri: String,
        position: Duration,
    ) {
        if (uri != currentUri) {
            currentUri = uri
            pendingResume = null
            exoPlayer.setMediaItem(MediaItem.fromUri(uri), position.toMillis())
            exoPlayer.prepare()
            exoPlayer.playWhenReady = false
        } else {
            exoPlayer.seekTo(position.toMillis())
        }
        endedUri = null
    }

    override fun stop(uri: String) {
        if (uri != currentUri) return
        exoPlayer.setPlaybackSpeed(1f)
        fastForward = false
        currentUri = null
        pausedByUser = false
        endedUri = null
        playingUri = null
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
    }

    override fun pause() {
        exoPlayer.playWhenReady = false
        playingUri = null
    }

    override fun resume() {
        if (currentUri == null || pausedByUser || ended) return
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

        /** Lecteur neuf, à libérer avec [release] ; il reprend [restoredState] s'il est donné. */
        @OptIn(UnstableApi::class)
        fun create(
            context: Context,
            restoredState: VideoPlaybackState? = null,
        ): ExoCardVideoPlayer {
            val codecSelector = SoftwareFallbackCodecSelector()
            val renderers =
                DefaultRenderersFactory(context)
                    .setMediaCodecSelector(codecSelector)
                    .setEnableDecoderFallback(true)
            return ExoCardVideoPlayer(ExoPlayer.Builder(context, renderers).build(), codecSelector, restoredState)
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
 * plan, libéré quand l'écran est quitté ou recréé.
 */
@Composable
fun rememberCardVideoPlayer(): CardVideoPlayer {
    val context = LocalContext.current
    // Recréé avec l'écran (le lecteur précédent est libéré), mais la vidéo, sa position et le son
    // sont repris : rien n'est perdu si l'écran est recréé (SPEC §14).
    val videoPlayer =
        rememberSaveable(
            saver =
                listSaver(
                    save = { it.playbackState.toList() },
                    restore = { ExoCardVideoPlayer.create(context, VideoPlaybackState.fromList(it)) },
                ),
        ) { ExoCardVideoPlayer.create(context) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { videoPlayer.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { videoPlayer.resume() }
    DisposableEffect(videoPlayer) { onDispose { videoPlayer.release() } }
    KeepScreenOnWhilePlaying(videoPlayer)
    return videoPlayer
}

/**
 * Garde l'écran allumé tant que [videoPlayer] lit une vidéo (SPEC §4.3) : en pause, à la fin de la
 * vidéo, sur une photo ou en quittant la pile, la mise en veille normale reprend. Passe par
 * `keepScreenOn` de la vue, sans permission ni `WakeLock`.
 */
@Composable
fun KeepScreenOnWhilePlaying(videoPlayer: CardVideoPlayer) {
    val view = LocalView.current
    val playing = videoPlayer.playingUri != null
    DisposableEffect(view, playing) {
        view.keepScreenOn = playing
        onDispose { view.keepScreenOn = false }
    }
}

/**
 * Ce qu'il faut garder du lecteur quand l'écran est recréé.
 *
 * @property uri vidéo chargée, `null` si aucune.
 * @property positionMillis position de lecture.
 * @property muted son coupé.
 * @property playing vidéo en lecture (et non en pause ou terminée).
 */
data class VideoPlaybackState(
    val uri: String?,
    val positionMillis: Long,
    val muted: Boolean,
    val playing: Boolean,
) {
    /** Forme enregistrable dans l'état sauvegardé de l'écran. */
    fun toList(): List<Any?> = listOf(uri, positionMillis, muted, playing)

    companion object {
        /** Relit la forme produite par [toList]. */
        fun fromList(values: List<Any?>): VideoPlaybackState =
            VideoPlaybackState(
                uri = values[0] as String?,
                positionMillis = values[1] as Long,
                muted = values[2] as Boolean,
                playing = values[3] as Boolean,
            )
    }
}

/** Vitesse de la lecture accélérée du maintien du doigt (PS-161). */
internal const val FAST_FORWARD_SPEED: Float = 2f
