package fr.babeuloula.photoswipe

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import fr.babeuloula.photoswipe.ui.PhotoSwipeApp
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import java.time.LocalDate
import javax.inject.Inject

/**
 * Point d'entrée de l'application, branché sur Hilt, qui affiche l'interface Compose.
 *
 * `enableEdgeToEdge()` dessine l'application sous les barres système (transparentes) et adapte
 * la couleur de leurs icônes au thème clair ou sombre ; les insets sont gérés côté Compose.
 * Le chargeur d'images injecté est transmis aux écrans par [LocalImageLoader] (ADR 0016).
 *
 * Lancée par la notification du rappel ([openDeckIntent]), elle ouvre directement la pile du jour
 * à trier. Le jour n'est lu qu'au premier lancement : une activité recréée garde sa navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var imageLoader: ImageLoader

    /** Pile à ouvrir dès que la navigation est affichée, `null` une fois ouverte. */
    private var deckToOpen by mutableStateOf<LocalDate?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) deckToOpen = deckDayOf(intent)
        setContent {
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                PhotoSwipeTheme {
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
