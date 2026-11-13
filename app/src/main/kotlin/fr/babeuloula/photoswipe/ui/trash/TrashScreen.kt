package fr.babeuloula.photoswipe.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.Trash
import fr.babeuloula.photoswipe.domain.model.TrashItem
import fr.babeuloula.photoswipe.ui.common.CenteredContent
import fr.babeuloula.photoswipe.ui.common.Thumbnail
import fr.babeuloula.photoswipe.ui.common.segmentShape
import fr.babeuloula.photoswipe.ui.deck.fileSizeLabel
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Page « Corbeille » (PS-168) : la place encore occupée, ce que la page montre (seulement les
 * suppressions de PhotoSwipe, gardées 30 jours par Android), puis chaque média encore restaurable,
 * avec sa vignette quand elle se lit, sa date de suppression définitive et ses boutons « Restaurer »
 * et « Supprimer définitivement ».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    state: TrashUiState,
    onEvent: (TrashEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trash_title)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(TrashEvent.Back) }) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (state) {
                TrashUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                TrashUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(stringResource(R.string.trash_error), style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = { onEvent(TrashEvent.Retry) }) { Text(stringResource(R.string.summary_retry)) }
                    }
                }

                is TrashUiState.Loaded -> {
                    CenteredContent { TrashContent(state, onEvent) }
                    state.confirmingDeletion?.let { DeleteForeverDialog(onEvent) }
                }
            }
        }
    }
}

/** Place occupée, explication, puis la liste des médias, ou le message de la corbeille vide. */
@Composable
private fun TrashContent(
    state: TrashUiState.Loaded,
    onEvent: (TrashEvent) -> Unit,
) {
    val items = state.trash.items
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            Column(Modifier.padding(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!state.trash.isEmpty) OccupiedSpace(state.trash)
                TrashNotice()
                if (state.trash.isEmpty) {
                    Text(
                        text = stringResource(R.string.trash_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp).testTag(TRASH_EMPTY_TAG),
                    )
                }
            }
        }
        itemsIndexed(items, key = { _, item -> item.media.id.value }) { index, item ->
            TrashRow(item, enabled = !state.busy, shape = segmentShape(index, items.size), onEvent = onEvent)
        }
    }
}

/** Place que les médias occupent encore dans la corbeille, et leur nombre. */
@Composable
private fun OccupiedSpace(trash: Trash) {
    Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.trash_occupied),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = fileSizeLabel(LocalContext.current, trash.occupiedBytes),
            style = MaterialTheme.typography.displaySmall,
        )
        Text(
            text = pluralStringResource(R.plurals.trash_count, trash.items.size, trash.items.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Ce que montre la page : seulement les suppressions de PhotoSwipe, gardées 30 jours par Android. */
@Composable
private fun TrashNotice() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(horizontal = 8.dp)) {
        Icon(
            painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.trash_notice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Un média de la corbeille : vignette, date de suppression définitive, poids et ses deux actions. */
@Composable
private fun TrashRow(
    item: TrashItem,
    enabled: Boolean,
    shape: Shape,
    onEvent: (TrashEvent) -> Unit,
) {
    val date = purgeDateLabel(item.purgeAt)
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 4.dp)
                .testTag("$TRASH_ROW_TAG${item.media.id.value}"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Thumbnail(item.media, Modifier.size(64.dp).clip(MaterialTheme.shapes.small))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(
                    text =
                        stringResource(
                            if (item.media.type == MediaType.VIDEO) R.string.trash_video else R.string.trash_photo,
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.trash_purge_on, date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = fileSizeLabel(LocalContext.current, item.media.sizeBytes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = { onEvent(TrashEvent.DeleteForever(item.media.id)) },
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.trash_delete_forever))
            }
            TextButton(
                onClick = { onEvent(TrashEvent.Restore(item.media.id)) },
                enabled = enabled,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.trash_restore))
            }
        }
    }
}

/** Date de suppression définitive, au format moyen de la langue de l'application. */
@Composable
private fun purgeDateLabel(purgeAt: Instant): String {
    val locale = LocalConfiguration.current.locales[0]
    return purgeAt
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
}

/** Confirmation de PhotoSwipe avant une suppression définitive, avec l'accès « Gestion des médias ». */
@Composable
private fun DeleteForeverDialog(onEvent: (TrashEvent) -> Unit) {
    AlertDialog(
        onDismissRequest = { onEvent(TrashEvent.DeletionDismissed) },
        icon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
        title = { Text(stringResource(R.string.trash_delete_confirm_title), textAlign = TextAlign.Center) },
        text = { Text(stringResource(R.string.trash_delete_confirm_text)) },
        confirmButton = {
            TextButton(onClick = { onEvent(TrashEvent.DeletionConfirmed) }) {
                Text(stringResource(R.string.trash_delete_forever))
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(TrashEvent.DeletionDismissed) }) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

/** Préfixe du repère de test d'une ligne, suivi de l'identifiant du média. */
internal const val TRASH_ROW_TAG = "trash_row_"

/** Repère de test du message de la corbeille vide. */
internal const val TRASH_EMPTY_TAG = "trash_empty"

@PreviewLightDark
@Composable
private fun TrashScreenPreview() {
    val now = Instant.parse("2026-11-13T10:00:00Z")
    PhotoSwipeTheme {
        TrashScreen(
            state =
                TrashUiState.Loaded(
                    Trash(
                        listOf(
                            TrashItem(
                                MediaItem(MediaId(1), "content://media/1", MediaType.IMAGE, now, PREVIEW_SIZE_BYTES),
                                now,
                                now.plusSeconds(PREVIEW_PURGE_SECONDS),
                            ),
                        ),
                    ),
                ),
            onEvent = {},
        )
    }
}

/** Poids du média de l'aperçu. */
private const val PREVIEW_SIZE_BYTES = 2_400_000L

/** Délai avant la suppression définitive dans l'aperçu : 30 jours. */
private const val PREVIEW_PURGE_SECONDS = 30L * 24 * 60 * 60
