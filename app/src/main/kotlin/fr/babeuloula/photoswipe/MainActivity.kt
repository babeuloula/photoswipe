package fr.babeuloula.photoswipe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import fr.babeuloula.photoswipe.ui.PhotoSwipeApp
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme

/**
 * Point d'entrée de l'application, branché sur Hilt, qui affiche l'interface Compose.
 *
 * `enableEdgeToEdge()` dessine l'application sous les barres système (transparentes) et adapte
 * la couleur de leurs icônes au thème clair ou sombre ; les insets sont gérés côté Compose.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PhotoSwipeTheme {
                PhotoSwipeApp()
            }
        }
    }
}
