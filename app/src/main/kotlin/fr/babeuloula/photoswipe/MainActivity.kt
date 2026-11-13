package fr.babeuloula.photoswipe

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.link.DeepLinks
import fr.babeuloula.photoswipe.link.LaunchRequest
import fr.babeuloula.photoswipe.link.LaunchRequestResolver
import fr.babeuloula.photoswipe.ui.PhotoSwipeApp
import fr.babeuloula.photoswipe.ui.common.CelebrationHost
import fr.babeuloula.photoswipe.ui.common.LocalAnimationsDisabled
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.common.RotationPolicy
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import fr.babeuloula.photoswipe.ui.theme.isDark
import fr.babeuloula.photoswipe.widget.WidgetRefresher
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import javax.inject.Inject

/**
 * Point d'entrée de l'application, branché sur Hilt, qui affiche l'interface Compose.
 *
 * `enableEdgeToEdge()` dessine l'application sous les barres système (transparentes) ; les insets
 * sont gérés côté Compose. Le thème clair ou sombre suit le choix des réglages ([ThemeMode]) : il
 * s'applique dès qu'il change, sans redémarrer, et la couleur des icônes des barres système suit le
 * thème choisi, pas celui du téléphone. Le noir pur du thème sombre (PS-176) suit le même chemin,
 * jusqu'à la barre de navigation. Le choix enregistré est lu une première fois avant
 * d'afficher l'interface, pour qu'elle n'apparaisse pas un instant dans le mauvais thème.
 * Le chargeur d'images injecté est transmis aux écrans par [LocalImageLoader] (ADR 0016).
 *
 * Lancée par la notification du rappel ([openDeckIntent]), elle ouvre directement la pile du jour
 * à trier ; lancée par un lien profond `photoswipe://…` ([DeepLinks]) ou un raccourci de l'icône,
 * l'écran qu'il demande ([launchRequestOf], ADR 0038). La demande n'est lue qu'au premier
 * lancement ou à la réception d'un nouvel intent : une activité recréée garde sa navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    /** Chargeur d'images de l'application, fourni aux écrans par [LocalImageLoader]. */
    @Inject
    lateinit var imageLoader: ImageLoader

    /** Réglages, lus pour le thème de l'application. */
    @Inject
    lateinit var settingsRepository: SettingsRepository

    /** Mise à jour du widget « Ce jour-là » en quittant l'application, après un tri (PS-170). */
    @Inject
    lateinit var widgetRefresher: WidgetRefresher

    /** Traduit l'écran demandé au lancement en route de navigation. */
    @Inject
    lateinit var launchRequestResolver: LaunchRequestResolver

    /** Écran à ouvrir dès que la navigation est affichée, `null` une fois ouvert. */
    private var launchRequest by mutableStateOf<LaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) launchRequest = launchRequestOf(intent)
        val themeModes = settingsRepository.observeThemeMode().catch { emit(ThemeMode.DEFAULT) }
        // Lecture courte d'un petit fichier local, une fois au démarrage : évite un éclair du mauvais thème.
        val savedThemeMode = runBlocking { themeModes.first() }
        val pureBlackChoices = settingsRepository.observePureBlack().catch { emit(false) }
        // Lu avec le thème, pour que le noir pur s'affiche dès la première image (PS-176).
        val savedPureBlack = runBlocking { pureBlackChoices.first() }
        val animations = settingsRepository.observeAnimationsEnabled().catch { emit(true) }
        val celebrations = settingsRepository.observeCelebrationEnabled().catch { emit(false) }
        setContent {
            val themeMode by remember { themeModes }.collectAsStateWithLifecycle(savedThemeMode)
            val pureBlack by remember { pureBlackChoices }.collectAsStateWithLifecycle(savedPureBlack)
            val animationsEnabled by remember { animations }.collectAsStateWithLifecycle(true)
            val celebrationEnabled by remember { celebrations }.collectAsStateWithLifecycle(false)
            val darkTheme = themeMode.isDark(isSystemInDarkTheme())
            val pureBlackBars = darkTheme && pureBlack
            DisposableEffect(darkTheme, pureBlackBars) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    // En noir pur, la barre de navigation à trois boutons est noire, comme le fond.
                    navigationBarStyle =
                        if (pureBlackBars) {
                            SystemBarStyle.dark(Color.BLACK)
                        } else {
                            SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme }
                        },
                )
                onDispose {}
            }
            CompositionLocalProvider(
                LocalImageLoader provides imageLoader,
                // Animations coupées dans les réglages, pour les téléphones anciens (PS-146).
                LocalAnimationsDisabled provides !animationsEnabled,
            ) {
                PhotoSwipeTheme(darkTheme = darkTheme, pureBlack = pureBlack) {
                    // Portrait, sauf le plein écran vidéo et la photo en grand (ADR 0028).
                    RotationPolicy {
                        // Confettis de fin de pile par-dessus tous les écrans, si activés (PS-174).
                        CelebrationHost(enabled = celebrationEnabled) {
                            PhotoSwipeApp(
                                launchRequest = launchRequest,
                                resolveLaunch = launchRequestResolver::routeFor,
                                onLaunchHandled = { launchRequest = null },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // L'écran d'accueil du téléphone va réapparaître : le widget y montre les tris qui viennent d'être faits.
        widgetRefresher.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        launchRequestOf(intent)?.let { launchRequest = it }
    }

    companion object {
        /** Voiles de la barre de navigation à trois boutons, ceux d'`enableEdgeToEdge()` par défaut. */
        private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

        /** Jour de la pile à ouvrir, en jours depuis l'epoch. */
        private const val EXTRA_DECK_EPOCH_DAY = "fr.babeuloula.photoswipe.extra.DECK_EPOCH_DAY"

        /** Intent qui ouvre l'application sur la pile du jour [day], l'accueil restant derrière. */
        fun openDeckIntent(
            context: Context,
            day: LocalDate,
        ): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_DECK_EPOCH_DAY, day.toEpochDay())

        /**
         * Écran demandé par [intent] : la pile du rappel, ou celui d'un lien `photoswipe://` (l'accueil
         * s'il est invalide) ; `null` pour un lancement ordinaire.
         */
        fun launchRequestOf(intent: Intent?): LaunchRequest? =
            when {
                intent == null -> {
                    null
                }

                intent.hasExtra(EXTRA_DECK_EPOCH_DAY) -> {
                    LaunchRequest.Deck(LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DECK_EPOCH_DAY, 0)))
                }

                else -> {
                    intent.data
                        ?.takeIf { it.scheme.equals(DeepLinks.SCHEME, ignoreCase = true) }
                        ?.let { DeepLinks.parse(it.toString()) }
                }
            }
    }
}
