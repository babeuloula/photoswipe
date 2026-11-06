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
import fr.babeuloula.photoswipe.ui.PhotoSwipeApp
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import fr.babeuloula.photoswipe.ui.theme.isDark
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
 * thème choisi, pas celui du téléphone. Le choix enregistré est lu une première fois avant
 * d'afficher l'interface, pour qu'elle n'apparaisse pas un instant dans le mauvais thème.
 * Le chargeur d'images injecté est transmis aux écrans par [LocalImageLoader] (ADR 0016).
 *
 * Lancée par la notification du rappel ([openDeckIntent]), elle ouvre directement la pile du jour
 * à trier. Le jour n'est lu qu'au premier lancement : une activité recréée garde sa navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var settingsRepository: SettingsRepository

    /** Pile à ouvrir dès que la navigation est affichée, `null` une fois ouverte. */
    private var deckToOpen by mutableStateOf<LocalDate?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) deckToOpen = deckDayOf(intent)
        val themeModes = settingsRepository.observeThemeMode().catch { emit(ThemeMode.DEFAULT) }
        // Lecture courte d'un petit fichier local, une fois au démarrage : évite un éclair du mauvais thème.
        val savedThemeMode = runBlocking { themeModes.first() }
        setContent {
            val themeMode by remember { themeModes }.collectAsStateWithLifecycle(savedThemeMode)
            val darkTheme = themeMode.isDark(isSystemInDarkTheme())
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { darkTheme },
                )
                onDispose {}
            }
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                PhotoSwipeTheme(darkTheme = darkTheme) {
                    PhotoSwipeApp(deckToOpen = deckToOpen, onDeckOpened = { deckToOpen = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        deckDayOf(intent)?.let { deckToOpen = it }
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

        /** Jour de pile porté par [intent], `null` pour un lancement ordinaire. */
        fun deckDayOf(intent: Intent?): LocalDate? =
            intent
                ?.takeIf { it.hasExtra(EXTRA_DECK_EPOCH_DAY) }
                ?.let { LocalDate.ofEpochDay(it.getLongExtra(EXTRA_DECK_EPOCH_DAY, 0)) }
    }
}
