package fr.babeuloula.photoswipe.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.babeuloula.photoswipe.ui.crash.CrashReportPrompt
import fr.babeuloula.photoswipe.ui.navigation.PhotoSwipeNavHost
import fr.babeuloula.photoswipe.ui.permission.MediaAccessGate
import fr.babeuloula.photoswipe.ui.tutorial.TutorialGate
import java.time.LocalDate

/**
 * Racine de l'interface Compose.
 *
 * L'application est dessinée bord à bord (edge-to-edge). Les écrans du parcours d'accès reçoivent
 * les marges des barres système (`safeDrawingPadding`) ; ceux de la navigation ont chacun leur
 * `Scaffold`, qui les gère lui-même. Au premier lancement, le tutoriel passe d'abord ([TutorialGate]) ;
 * la navigation n'apparaît ensuite qu'avec l'accès aux photos et vidéos ([MediaAccessGate]). Après un
 * plantage, [CrashReportPrompt] propose par-dessus d'envoyer son rapport (ADR 0033). Les aperçus vivent
 * au niveau de chaque écran : la racine dépend de Hilt.
 *
 * @param deckToOpen pile à ouvrir dès que la navigation s'affiche (notification du rappel).
 * @param onDeckOpened appelé une fois cette pile ouverte.
 */
@Composable
fun PhotoSwipeApp(
    modifier: Modifier = Modifier,
    deckToOpen: LocalDate? = null,
    onDeckOpened: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        CrashReportPrompt {
            TutorialGate(modifier = Modifier.safeDrawingPadding()) {
                MediaAccessGate(modifier = Modifier.safeDrawingPadding()) {
                    PhotoSwipeNavHost(deckToOpen = deckToOpen, onDeckOpened = onDeckOpened)
                }
            }
        }
    }
}
