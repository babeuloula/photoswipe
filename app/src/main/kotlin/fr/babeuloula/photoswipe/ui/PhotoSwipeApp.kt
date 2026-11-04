package fr.babeuloula.photoswipe.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.Wallpapers
import fr.babeuloula.photoswipe.ui.navigation.PhotoSwipeNavHost
import fr.babeuloula.photoswipe.ui.permission.MediaAccessGate
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * Racine de l'interface Compose.
 *
 * L'application est dessinée bord à bord (edge-to-edge). Les écrans du parcours d'accès reçoivent
 * les marges des barres système (`safeDrawingPadding`) ; ceux de la navigation ont chacun leur
 * `Scaffold`, qui les gère lui-même. La navigation n'apparaît qu'avec l'accès aux photos et vidéos
 * ([MediaAccessGate]).
 *
 * @param clock horloge de l'appareil, injectée dans `MainActivity`.
 */
@Composable
fun PhotoSwipeApp(
    clock: Clock,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        MediaAccessGate(modifier = Modifier.safeDrawingPadding()) {
            PhotoSwipeNavHost(clock = clock)
        }
    }
}

/** Horloge figée des aperçus. */
private val PREVIEW_CLOCK: Clock = Clock.fixed(Instant.parse("2026-07-15T10:00:00Z"), ZoneOffset.UTC)

/** Aperçus clair et sombre avec la palette de repli. */
@PreviewLightDark
@Composable
private fun PhotoSwipeAppFallbackPreview() {
    PhotoSwipeTheme(dynamicColor = false) {
        PhotoSwipeApp(clock = PREVIEW_CLOCK)
    }
}

/** Aperçu des couleurs dynamiques, tirées d'un fond d'écran d'exemple. */
@Preview(name = "Dynamic colors", wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE)
@Composable
private fun PhotoSwipeAppDynamicPreview() {
    PhotoSwipeTheme(dynamicColor = true) {
        PhotoSwipeApp(clock = PREVIEW_CLOCK)
    }
}
