package fr.babeuloula.photoswipe.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.babeuloula.photoswipe.link.LaunchRequest
import fr.babeuloula.photoswipe.ui.crash.CrashReportPrompt
import fr.babeuloula.photoswipe.ui.navigation.HomeRoute
import fr.babeuloula.photoswipe.ui.navigation.PhotoSwipeNavHost
import fr.babeuloula.photoswipe.ui.permission.MediaAccessGate
import fr.babeuloula.photoswipe.ui.tutorial.TutorialGate

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
 * @param launchRequest écran à ouvrir dès que la navigation s'affiche, après le tutoriel et l'accès
 * aux médias (notification du rappel, lien profond, raccourci, tuile ; ADR 0038).
 * @param resolveLaunch traduit cette demande en route de navigation.
 * @param onLaunchHandled appelé une fois cet écran ouvert.
 */
@Composable
fun PhotoSwipeApp(
    modifier: Modifier = Modifier,
    launchRequest: LaunchRequest? = null,
    resolveLaunch: suspend (LaunchRequest) -> Any = { HomeRoute },
    onLaunchHandled: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        CrashReportPrompt {
            TutorialGate(modifier = Modifier.safeDrawingPadding()) {
                MediaAccessGate(modifier = Modifier.safeDrawingPadding()) {
                    PhotoSwipeNavHost(
                        launchRequest = launchRequest,
                        resolveLaunch = resolveLaunch,
                        onLaunchHandled = onLaunchHandled,
                    )
                }
            }
        }
    }
}
