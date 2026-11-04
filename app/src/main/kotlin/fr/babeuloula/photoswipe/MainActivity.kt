package fr.babeuloula.photoswipe

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Point d'entrée de l'application, branché sur Hilt.
 *
 * Hilt exige une `ComponentActivity`. Compose et le thème Material 3 (PS-4) viendront s'y greffer.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity()
