package fr.babeuloula.photoswipe.ui.common

import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf
import fr.babeuloula.photoswipe.platform.PlatformFeatures

/**
 * Fonctions d'Android du téléphone, lues par les écrans qui cachent ou adaptent ce qui n'existe pas
 * sur toutes les versions (favoris et corbeille de la galerie avant Android 11, epic 14). Fournies
 * par `MainActivity` depuis Hilt ; par défaut, toutes présentes, pour les aperçus et les tests qui
 * ne la fournissent pas.
 */
val LocalPlatformFeatures =
    staticCompositionLocalOf { PlatformFeatures.forSdk(Build.VERSION_CODES.UPSIDE_DOWN_CAKE) }
