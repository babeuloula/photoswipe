package fr.babeuloula.photoswipe.ui.crash

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.crash.CrashReportLink
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme

/**
 * Propose d'envoyer le rapport du plantage précédent (ADR 0033), par-dessus [content], et ouvre la
 * page d'envoi dans le navigateur si la personne le veut.
 */
@Composable
fun CrashReportPrompt(
    viewModel: CrashReportViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val report by viewModel.pendingReport.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val linkUnavailable = stringResource(R.string.link_unavailable)
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                is CrashReportEffect.OpenReport -> {
                    if (!CrashReportLink.open(context, effect.report)) {
                        Toast.makeText(context, linkUnavailable, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    content()
    if (report != null) CrashReportDialog(onEvent = viewModel::onEvent)
}

/**
 * Dialogue du rapport de plantage : le voir et l'envoyer depuis le navigateur, ou l'ignorer.
 * Un retour ou un appui à côté l'ignore aussi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrashReportDialog(onEvent: (CrashReportEvent) -> Unit) {
    val title = stringResource(R.string.crash_report_title)
    BasicAlertDialog(
        onDismissRequest = { onEvent(CrashReportEvent.Dismiss) },
        modifier = Modifier.semantics { paneTitle = title },
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bug_report),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = stringResource(R.string.crash_report_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onEvent(CrashReportEvent.Send) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.crash_report_send))
                    }
                    TextButton(onClick = { onEvent(CrashReportEvent.Dismiss) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.crash_report_ignore))
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun CrashReportDialogPreview() {
    PhotoSwipeTheme {
        CrashReportDialog(onEvent = {})
    }
}
