package fr.babeuloula.photoswipe.ui.tutorial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.LocalPlatformFeatures
import fr.babeuloula.photoswipe.ui.deck.fileSizeLabel
import fr.babeuloula.photoswipe.ui.theme.motion
import fr.babeuloula.photoswipe.ui.theme.swipeColors
import kotlinx.coroutines.delay

/**
 * Page « rien n'est définitif » : le résumé de fin de tri (espace libéré estimé, gardés, marqués),
 * puis le dialogue qui confirme l'envoi des marqués à la corbeille. Statique ([animate] faux) : le
 * dialogue ouvert devant le résumé.
 */
@Composable
internal fun ConfirmationIllustration(
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    var dialogOpen by remember { mutableStateOf(!animate) }
    if (animate) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(SUMMARY_MILLIS)
                dialogOpen = true
                delay(DIALOG_MILLIS)
                dialogOpen = false
            }
        }
    }
    val motion = MaterialTheme.motion
    Box(modifier, contentAlignment = Alignment.Center) {
        SampleSummary(Modifier.fillMaxSize())
        AnimatedVisibility(
            visible = dialogOpen,
            enter = fadeIn(motion.defaultEffects()),
            exit = fadeOut(motion.fastExit()),
        ) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA)))
        }
        AnimatedVisibility(
            visible = dialogOpen,
            enter =
                scaleIn(motion.defaultSpatial(), initialScale = DIALOG_START_SCALE) + fadeIn(motion.defaultEffects()),
            exit = scaleOut(motion.fastExit(), targetScale = DIALOG_START_SCALE) + fadeOut(motion.fastExit()),
        ) {
            SampleTrashDialog()
        }
    }
}

/** Résumé d'exemple : espace libéré en grand, puis les gardés et les marqués. */
@Composable
private fun SampleSummary(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)) {
        Text(
            stringResource(R.string.summary_freed_space),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(fileSizeLabel(context, SAMPLE_FREED_BYTES), style = MaterialTheme.typography.displaySmall, maxLines = 1)
        SummaryLine(
            R.drawable.ic_check,
            MaterialTheme.swipeColors.keep,
            pluralStringResource(R.plurals.summary_kept, KEPT, KEPT),
        )
        SummaryLine(
            R.drawable.ic_delete,
            MaterialTheme.swipeColors.delete,
            pluralStringResource(R.plurals.summary_marked, MARKED, MARKED),
        )
    }
}

/** Ligne du résumé d'exemple : une icône colorée et son nombre. */
@Composable
private fun SummaryLine(
    icon: Int,
    tint: androidx.compose.ui.graphics.Color,
    text: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Dialogue de corbeille d'exemple, comme celui du résumé : icône, question, délai de restauration,
 * action ; sans corbeille (Android 10), celui de la suppression définitive (PS-185).
 */
@Composable
private fun SampleTrashDialog() {
    val trash = LocalPlatformFeatures.current.systemTrash
    Column(
        modifier =
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_delete),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
        )
        Text(
            pluralStringResource(
                if (trash) R.plurals.summary_trash_confirm_title else R.plurals.summary_delete_confirm_title,
                MARKED,
                MARKED,
            ),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(if (trash) R.string.summary_trash_confirm_text else R.string.summary_delete_confirm_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(if (trash) R.string.summary_trash_confirm_action else R.string.trash_delete_forever),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.align(Alignment.End),
        )
    }
}

/** Espace libéré de l'exemple : 1,2 Go. */
private const val SAMPLE_FREED_BYTES = 1_200_000_000L

/** Médias gardés de l'exemple. */
private const val KEPT = 18

/** Médias marqués de l'exemple. */
internal const val MARKED = 12

/** Opacité du voile derrière le dialogue, celle des dialogues Material 3. */
private const val SCRIM_ALPHA = 0.32f

/** Échelle de départ du dialogue qui s'ouvre. */
private const val DIALOG_START_SCALE = 0.85f

/** Durée du résumé seul, avant le dialogue. */
internal const val SUMMARY_MILLIS = 1_600L

/** Durée du dialogue ouvert. */
private const val DIALOG_MILLIS = 2_200L
