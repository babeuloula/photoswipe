package fr.babeuloula.photoswipe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import fr.babeuloula.photoswipe.ui.PhotoSwipeApp
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import javax.inject.Inject

/**
 * Point d'entrée de l'application, branché sur Hilt, qui affiche l'interface Compose.
 *
 * `enableEdgeToEdge()` dessine l'application sous les barres système (transparentes) et adapte
 * la couleur de leurs icônes au thème clair ou sombre ; les insets sont gérés côté Compose.
 * Le chargeur d'images injecté est transmis aux écrans par [LocalImageLoader] (ADR 0016).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalImageLoader provides imageLoader) {
                PhotoSwipeTheme {
                    PhotoSwipeApp()
                }
            }
        }
    }
}
