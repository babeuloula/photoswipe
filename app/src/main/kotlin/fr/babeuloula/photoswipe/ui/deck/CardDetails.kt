package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import kotlinx.coroutines.delay

/**
 * Repère et boutons ([top]), barre de progression et pastilles ([bottom]) d'une carte, qui
 * apparaissent et s'effacent en fondu selon [shown].
 */
@Composable
internal fun BoxScope.CardDetails(
    shown: Boolean,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
    ) { top() }
    AnimatedVisibility(
        visible = shown,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
    ) { bottom() }
}

/**
 * Détails de la carte affichés : toujours, sauf pendant la lecture de la vidéo [video], où ils
 * s'effacent [CONTROLS_VISIBLE_MILLIS] après le départ pour ne pas la cacher (une vidéo en paysage
 * surtout, sur une carte basse). Un appui la met en pause et les fait revenir ; avec TalkBack, ils
 * restent affichés.
 */
@Composable
internal fun rememberCardDetailsShown(video: ActiveVideoState?): Boolean {
    val hideAfter =
        LocalAccessibilityManager.current?.calculateRecommendedTimeoutMillis(
            CONTROLS_VISIBLE_MILLIS,
            containsIcons = true,
            containsControls = true,
        ) ?: CONTROLS_VISIBLE_MILLIS
    val playing = video?.playing == true
    var shown by remember(video?.uri) { mutableStateOf(true) }
    LaunchedEffect(video?.uri, playing, hideAfter) {
        shown = true
        if (playing && hideAfter != Long.MAX_VALUE) {
            delay(hideAfter)
            shown = false
        }
    }
    return shown
}
