package fr.babeuloula.photoswipe.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Mouvement de l'application (SPEC §8) : les ressorts du système de mouvement *Expressive* de
 * Material 3. `MaterialTheme.motionScheme` est encore interne dans Material 3 stable : les valeurs
 * de `MotionScheme.expressive()` sont reprises ici, et seront remplacées par lui quand il sera
 * public. Expressive plutôt que standard : il garde le léger rebond de la carte qui revient en place.
 *
 * Ressorts « spatiaux » (position, taille) : avec un léger rebond. Ressorts d'« effets » (opacité,
 * couleur) : sans rebond. Chacun en version rapide, normale et lente.
 */
@Immutable
class PhotoSwipeMotion internal constructor() {
    /** Déplacement court, par exemple une carte qui sort de l'écran. */
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(FAST_SPATIAL_DAMPING, FAST_SPATIAL_STIFFNESS)

    /** Déplacement courant, par exemple une carte qui revient en place ou un écran qui entre. */
    fun <T> defaultSpatial(): FiniteAnimationSpec<T> = spring(SPATIAL_DAMPING, DEFAULT_SPATIAL_STIFFNESS)

    /** Déplacement ample et posé. */
    fun <T> slowSpatial(): FiniteAnimationSpec<T> = spring(SPATIAL_DAMPING, SLOW_SPATIAL_STIFFNESS)

    /**
     * Sortie rapide, sans rebond : la carte qui quitte l'écran, après un geste validé ou un bouton.
     * Le ressort reprend la vitesse du doigt (ADR 0025).
     */
    fun <T> fastExit(): FiniteAnimationSpec<T> = spring(EFFECTS_DAMPING, FAST_EXIT_STIFFNESS)

    /**
     * Déplacement court à peine amorti, celui du schéma *standard* de Material 3 : la carte
     * suivante qui remonte sous la carte du dessus (ADR 0025).
     */
    fun <T> fastSpatialStandard(): FiniteAnimationSpec<T> = spring(STANDARD_FAST_SPATIAL_DAMPING, FAST_EXIT_STIFFNESS)

    /** Effet bref, par exemple un bouton qui s'efface. */
    fun <T> fastEffects(): FiniteAnimationSpec<T> = spring(EFFECTS_DAMPING, FAST_EFFECTS_STIFFNESS)

    /** Effet courant, par exemple un fondu entre deux écrans. */
    fun <T> defaultEffects(): FiniteAnimationSpec<T> = spring(EFFECTS_DAMPING, DEFAULT_EFFECTS_STIFFNESS)

    /** Effet lent. */
    fun <T> slowEffects(): FiniteAnimationSpec<T> = spring(EFFECTS_DAMPING, SLOW_EFFECTS_STIFFNESS)

    private companion object {
        // Valeurs du schéma de mouvement Expressive de Material 3 (MotionScheme.expressive()).
        const val FAST_SPATIAL_DAMPING = 0.6f
        const val SPATIAL_DAMPING = 0.8f
        const val FAST_SPATIAL_STIFFNESS = 800f
        const val DEFAULT_SPATIAL_STIFFNESS = 380f
        const val SLOW_SPATIAL_STIFFNESS = 200f
        const val EFFECTS_DAMPING = 1f
        const val FAST_EFFECTS_STIFFNESS = 3800f
        const val DEFAULT_EFFECTS_STIFFNESS = 1600f
        const val SLOW_EFFECTS_STIFFNESS = 800f

        // Schéma standard de Material 3 (MotionScheme.standard()), déplacement rapide.
        const val STANDARD_FAST_SPATIAL_DAMPING = 0.9f
        const val FAST_EXIT_STIFFNESS = 1400f
    }
}

/** Fourni par [PhotoSwipeTheme]. */
internal val LocalPhotoSwipeMotion = staticCompositionLocalOf { PhotoSwipeMotion() }

/** Mouvement du thème courant. */
val MaterialTheme.motion: PhotoSwipeMotion
    @Composable
    @ReadOnlyComposable
    get() = LocalPhotoSwipeMotion.current
