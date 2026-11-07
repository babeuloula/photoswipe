package fr.babeuloula.photoswipe.ui.common

import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Écrans qui autorisent la rotation du téléphone, comptés pour l'activité (ADR 0028) : le plein
 * écran vidéo et la photo en grand. Partout ailleurs, l'application reste en portrait.
 */
class RotationRequests {
    /** Nombre d'écrans affichés qui autorisent la rotation. */
    var count: Int by mutableIntStateOf(0)
        private set

    /** Un écran qui autorise la rotation s'affiche. */
    fun acquire() {
        count++
    }

    /** Il disparaît. */
    fun release() {
        count = (count - 1).coerceAtLeast(0)
    }
}

/** Écrans qui autorisent la rotation, fournis par [RotationPolicy] ; `null` hors de l'activité. */
val LocalRotationRequests = staticCompositionLocalOf<RotationRequests?> { null }

/**
 * Règle d'orientation de l'activité (ADR 0028), posée par `MainActivity` autour de toute
 * l'interface : l'orientation suit le téléphone (`FULL_USER`, rotation automatique respectée)
 * tant qu'un écran l'autorise ([AllowRotation]), sinon le portrait.
 *
 * L'orientation est relue après chaque composition : quand une rotation recrée l'activité, la
 * nouvelle composition rouvre d'abord le plein écran restauré, et le portrait n'est jamais
 * demandé entre-temps. Si cet écran n'est pas restauré, ou se ferme de quelque façon que ce soit,
 * le compteur retombe à zéro et le portrait revient.
 */
@Composable
fun RotationPolicy(content: @Composable () -> Unit) {
    val requests = remember { RotationRequests() }
    val activity = LocalActivity.current
    LaunchedEffect(requests, activity) {
        snapshotFlow { requests.count > 0 }.collect { allowed ->
            activity?.requestedOrientation =
                if (allowed) ActivityInfo.SCREEN_ORIENTATION_FULL_USER else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    CompositionLocalProvider(LocalRotationRequests provides requests, content = content)
}

/**
 * Autorise la rotation du téléphone tant que l'écran qui l'appelle est affiché (plein écran
 * vidéo, photo en grand). Sans [RotationPolicy] autour, sans effet.
 */
@Composable
fun AllowRotation() {
    val requests = LocalRotationRequests.current ?: return
    DisposableEffect(requests) {
        requests.acquire()
        onDispose { requests.release() }
    }
}
