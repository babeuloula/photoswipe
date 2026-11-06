package fr.babeuloula.photoswipe.ui.summary

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.domain.usecase.GetMarkedPreviews
import fr.babeuloula.photoswipe.ui.common.CenteredContent
import fr.babeuloula.photoswipe.ui.common.Thumbnail
import fr.babeuloula.photoswipe.ui.common.segmentShape
import fr.babeuloula.photoswipe.ui.deck.fileSizeLabel
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import fr.babeuloula.photoswipe.ui.theme.monoLabel
import fr.babeuloula.photoswipe.ui.theme.swipeColors

/**
 * Résumé avant confirmation (SPEC §3.5) : l'espace libéré estimé en tête, puis ce qui a été trié
 * aujourd'hui (gardés, favoris, ADR 0024) et ce qui attend la confirmation (marqués pour suppression,
 * toutes sessions confondues). « Confirmer la suppression » présente les dialogues
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
                            painterResource(R.drawable.ic_arrow_back),
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
                    CenteredContent { SummaryContent(state = state, onEvent = onEvent) }
                    if (state.askingTrashConfirmation) {
                        TrashConfirmationDialog(state.summary.markedForDeletionCount, onEvent)
                    }
                }
            }
        }
    }
}

/** Espace libéré, détail des décisions et boutons de confirmation en bas. */
@Composable
private fun SummaryContent(
    state: SummaryUiState.Loaded,
    onEvent: (SummaryEvent) -> Unit,
) {
    val summary = state.summary
    val colors = MaterialTheme.swipeColors
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            FreedSpace(summary.estimatedFreedBytes, summary.markedForDeletionCount)
            Column {
                SummaryHeading(stringResource(R.string.summary_sorted_today))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SummaryLine(
                        icon = R.drawable.ic_check,
                        tint = colors.keep,
                        text = pluralStringResource(R.plurals.summary_kept, summary.keptCount, summary.keptCount),
                        shape = segmentShape(0, 2),
                    )
                    SummaryLine(
                        icon = R.drawable.ic_favorite_filled,
                        tint = colors.favorite,
                        text =
                            pluralStringResource(
                                R.plurals.summary_favorites,
                                summary.favoriteCount,
                                summary.favoriteCount,
                            ),
                        shape = segmentShape(1, 2),
                    )
                }
            }
            Column {
                SummaryHeading(stringResource(R.string.summary_to_confirm))
                MarkedGroup(state)
            }
            NothingDeletedYet()
        }
        SummaryActions(canConfirm = state.canConfirm, onEvent = onEvent)
    }
}

/** « Rien n'est encore supprimé… », avec une icône d'information. */
@Composable
private fun NothingDeletedYet() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
        Icon(
            painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.summary_nothing_deleted_yet),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** « Confirmer la suppression », inactif sans marquage ou pendant les dialogues, et « Plus tard ». */
@Composable
private fun SummaryActions(
    canConfirm: Boolean,
    onEvent: (SummaryEvent) -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = { onEvent(SummaryEvent.Confirm) },
            enabled = canConfirm,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text(stringResource(R.string.summary_confirm))
        }
        TextButton(
            onClick = { onEvent(SummaryEvent.Later) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            Text(stringResource(R.string.summary_later))
        }
    }
}

/** Titre d'un groupe de lignes, annoncé comme tel par TalkBack. */
@Composable
private fun SummaryHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp).semantics { heading() },
    )
}

/**
 * Espace libéré estimé, mis en avant sans carte (ADR 0025) : le nombre en grand, en chasse fixe, et
 * ce dont il dépend (« Si vous confirmez les 12 médias marqués »).
 */
@Composable
private fun FreedSpace(
    bytes: Long,
    markedCount: Int,
) {
    Column(
        modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.summary_freed_space),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = fileSizeLabel(LocalContext.current, bytes), style = MaterialTheme.typography.displayLarge)
        if (markedCount > 0) {
            Text(
                text = pluralStringResource(R.plurals.summary_if_confirmed, markedCount, markedCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Médias marqués pour suppression : leur nombre, puis les vignettes des derniers marqués et « +7 ». */
@Composable
private fun MarkedGroup(state: SummaryUiState.Loaded) {
    val count = state.summary.markedForDeletionCount
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(bottom = if (state.markedPreviews.isEmpty()) 0.dp else 12.dp),
    ) {
        SummaryLine(
            icon = R.drawable.ic_delete,
            tint = MaterialTheme.swipeColors.delete,
            text = pluralStringResource(R.plurals.summary_marked, count, count),
            shape = MaterialTheme.shapes.large,
        )
        if (state.markedPreviews.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.markedPreviews.forEach {
                    Thumbnail(
                        it,
                        Modifier.weight(1f).height(48.dp).clip(MaterialTheme.shapes.small),
                    )
                }
                repeat(GetMarkedPreviews.DEFAULT_LIMIT - state.markedPreviews.size) { Spacer(Modifier.weight(1f)) }
                MoreMarked(state.hiddenMarkedCount, Modifier.weight(1f))
            }
        }
    }
}

/** Case « +7 » des médias marqués sans vignette ; vide s'il n'y en a pas. */
@Composable
private fun MoreMarked(
    count: Int,
    modifier: Modifier = Modifier,
) {
    if (count == 0) {
        Spacer(modifier)
        return
    }
    val description = stringResource(R.string.summary_more_marked_description, count)
    Box(
        modifier =
            modifier
                .height(48.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.summary_more_marked, count),
            style = MaterialTheme.typography.monoLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Une ligne du détail : icône de la décision et nombre de médias. */
@Composable
private fun SummaryLine(
    @DrawableRes icon: Int,
    tint: Color,
    text: String,
    shape: Shape,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = tint)
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
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
