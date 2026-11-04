package fr.babeuloula.photoswipe.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.Wallpapers
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme

/**
 * Racine de l'interface Compose.
 *
 * L'application est dessinée bord à bord (edge-to-edge) : le `Scaffold` transmet les marges des
 * barres système (`innerPadding`), que le contenu applique pour ne jamais passer dessous.
 * L'écran d'accueil est provisoire, en attendant la pile de swipe (epic 5).
 */
@Composable
fun PhotoSwipeApp(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.home_placeholder),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Aperçus clair et sombre avec la palette de repli. */
@PreviewLightDark
@Composable
private fun PhotoSwipeAppFallbackPreview() {
    PhotoSwipeTheme(dynamicColor = false) {
        PhotoSwipeApp()
    }
}

/** Aperçu des couleurs dynamiques, tirées d'un fond d'écran d'exemple. */
@Preview(name = "Dynamic colors", wallpaper = Wallpapers.GREEN_DOMINATED_EXAMPLE)
@Composable
private fun PhotoSwipeAppDynamicPreview() {
    PhotoSwipeTheme(dynamicColor = true) {
        PhotoSwipeApp()
    }
}
