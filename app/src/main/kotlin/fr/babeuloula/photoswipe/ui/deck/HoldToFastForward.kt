package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R

/**
 * Appui et maintien sur une vidéo (PS-161) : un appui garde son rôle ([onTap] : pause et lecture
 * sur la carte, commandes en plein écran) ; maintenir le doigt lit la vidéo en ×2 ([onFast] vrai)
 * jusqu'au relâchement, sans déclencher l'appui. TalkBack propose la même chose par une action qui
 * bascule la vitesse.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Modifier.holdToFastForward(
    fast: Boolean,
    onFast: (Boolean) -> Unit,
    onTap: () -> Unit,
    onTapLabel: String? = null,
): Modifier {
    val currentFast by rememberUpdatedState(fast)
    val currentOnFast by rememberUpdatedState(onFast)
    val label = stringResource(if (fast) R.string.deck_video_normal_speed else R.string.deck_video_fast_forward)
    return this
        // Relâcher le doigt, ou le glisser assez pour lancer un geste, revient à la vitesse normale.
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                waitForUpOrCancellation()
                if (currentFast) currentOnFast(false)
            }
        }.combinedClickable(
            interactionSource = null,
            indication = null,
            onClickLabel = onTapLabel,
            onLongClick = { onFast(true) },
            onClick = onTap,
        ).semantics {
            customActions =
                listOf(
                    CustomAccessibilityAction(label) {
                        onFast(!fast)
                        true
                    },
                )
        }
}

/** Puce « ×2 » de la lecture accélérée, annoncée par TalkBack. */
@Composable
internal fun FastForwardChip(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.deck_video_fast_forward_on)
    Text(
        text = stringResource(R.string.deck_video_speed_chip),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primaryFixed,
        modifier =
            modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onPrimaryFixed)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag(FAST_FORWARD_CHIP_TAG)
                .clearAndSetSemantics {
                    contentDescription = label
                    liveRegion = LiveRegionMode.Polite
                },
    )
}

/** Étiquette de test de la puce « ×2 ». */
internal const val FAST_FORWARD_CHIP_TAG = "fast_forward_chip"
