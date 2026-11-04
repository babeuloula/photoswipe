package fr.babeuloula.photoswipe.ui.common

import androidx.compose.runtime.staticCompositionLocalOf
import coil3.ImageLoader

/**
 * Chargeur d'images Coil fourni par Hilt, transmis aux composables par `MainActivity` (ADR 0016).
 * Un aperçu ou un test qui compose une image hors de l'application en fournit un lui-même.
 */
val LocalImageLoader =
    staticCompositionLocalOf<ImageLoader> {
        error("No ImageLoader provided: wrap the content in CompositionLocalProvider(LocalImageLoader provides …)")
    }
