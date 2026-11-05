package fr.babeuloula.photoswipe.ui.summary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.ui.deck.fileSizeLabel
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import fr.babeuloula.photoswipe.ui.theme.swipeColors

/**
 * Résumé avant confirmation (SPEC §3.5) : l'espace libéré estimé en tête, puis les gardés, les
 * marqués pour suppression et les favoris. « Confirmer la suppression » présente les dialogues
 * système de corbeille ; « Plus tard » ramène à l'accueil sans rien changer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    state: SummaryUiState,
    onEvent: (SummaryEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.summary_title)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(SummaryEvent.Back) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            when (state) {
                SummaryUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                SummaryUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(stringResource(R.string.summary_error), style = MaterialTheme.typography.bodyLarge)
                        Button(
                            onClick = { onEvent(SummaryEvent.Retry) },
                        ) { Text(stringResource(R.string.summary_retry)) }
                    }
                }

                is SummaryUiState.Loaded -> {
                    SummaryContent(state = state, onEvent = onEvent)
                }
            }
        }
    }
}

/** Espace libéré, détail des décisions et boutons. */
@Composable
private fun SummaryContent(
    state: SummaryUiState.Loaded,
    onEvent: (SummaryEvent) -> Unit,
) {
    val summary = state.summary
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FreedSpaceCard(summary.estimatedFreedBytes)
        val colors = MaterialTheme.swipeColors
        Column {
            SummaryLine(
                Icons.Filled.Check,
                colors.keep,
                pluralStringResource(R.plurals.summary_kept, summary.keptCount, summary.keptCount),
            )
            SummaryLine(
                Icons.Filled.Delete,
                colors.delete,
                pluralStringResource(
                    R.plurals.summary_marked,
                    summary.markedForDeletionCount,
                    summary.markedForDeletionCount,
                ),
            )
            SummaryLine(
                Icons.Filled.Favorite,
                colors.favorite,
                pluralStringResource(R.plurals.summary_favorites, summary.favoriteCount, summary.favoriteCount),
            )
        }
        Text(
            text = stringResource(R.string.summary_nothing_deleted_yet),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ConfirmButton(state, onEvent)
        OutlinedButton(onClick = { onEvent(SummaryEvent.Later) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.summary_later))
        }
    }
}

/** Bouton de confirmation : rouge pour une suppression ; sans marquage, il n'applique que les favoris. */
@Composable
private fun ConfirmButton(
    state: SummaryUiState.Loaded,
    onEvent: (SummaryEvent) -> Unit,
) {
    Button(
        onClick = { onEvent(SummaryEvent.Confirm) },
        enabled = state.canConfirm,
        colors =
            if (state.confirmsDeletion) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )
            } else {
                ButtonDefaults.buttonColors()
            },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(
                if (state.confirmsDeletion) R.string.summary_confirm else R.string.summary_apply_favorites,
            ),
        )
    }
}

/** Espace libéré estimé, mis en avant : c'est ce que la confirmation rendra au téléphone. */
@Composable
private fun FreedSpaceCard(bytes: Long) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.summary_freed_space),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = fileSizeLabel(LocalContext.current, bytes),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** Une ligne du détail : icône de la décision et nombre de médias. */
@Composable
private fun SummaryLine(
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    text: String,
) {
    ListItem(
        headlineContent = { Text(text) },
        leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
    )
}

@PreviewLightDark
@Composable
private fun SummaryScreenPreview() {
    PhotoSwipeTheme(dynamicColor = false) {
        SummaryScreen(
            state =
                SummaryUiState.Loaded(
                    SessionSummary(
                        keptCount = 12,
                        markedForDeletionCount = 4,
                        favoriteCount = 1,
                        estimatedFreedBytes = 48_000_000,
                    ),
                ),
            onEvent = {},
        )
    }
}
