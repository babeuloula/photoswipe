package fr.babeuloula.photoswipe.ui.common

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Vrai quand le réglage d'accessibilité « Supprimer les animations » d'Android est actif (échelle
 * de durée des animations à 0) : les animations décoratives laissent place à une version statique
 * (SPEC §8). Relu à chaque retour au premier plan, le réglage pouvant changer entre-temps.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    var reduced by remember { mutableStateOf(animationsRemoved(context)) }
    LifecycleResumeEffect(context) {
        reduced = animationsRemoved(context)
        onPauseOrDispose {}
    }
    return reduced
}

/** Vrai si l'échelle de durée des animations du système vaut 0. */
private fun animationsRemoved(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
