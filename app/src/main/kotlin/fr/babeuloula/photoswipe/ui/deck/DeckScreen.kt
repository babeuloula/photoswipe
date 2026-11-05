package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import java.time.LocalDate

/**
 * Pile de swipe d'un jour (SPEC §4.3) : la carte plein écran du média du dessus, à glisser à droite
 * pour la garder ou à gauche pour la marquer à supprimer, ou à trier avec les boutons équivalents
 * placés dessous, avec retour et
 * « Terminer » dans la barre du haut. Gère le chargement, l'erreur et la pile vide.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckScreen(
    day: LocalDate,
    state: DeckUiState,
    onEvent: (DeckEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.deck_title, localizedDate(LocalContext.current, day))) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { onEvent(DeckEvent.Finish) }) { Text(stringResource(R.string.deck_finish)) }
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
            DeckContent(state = state, onEvent = onEvent)
        }
    }
}

/** Contenu de la pile selon son état : chargement, erreur, pile vide ou carte du dessus. */
@Composable
private fun BoxScope.DeckContent(
    state: DeckUiState,
    onEvent: (DeckEvent) -> Unit,
) {
    when (state) {
        DeckUiState.Loading -> {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }

        DeckUiState.Error -> {
            DeckMessage(
                text = stringResource(R.string.deck_error),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Button(onClick = { onEvent(DeckEvent.Retry) }) { Text(stringResource(R.string.deck_retry)) }
            }
        }

        is DeckUiState.Empty -> {
            DeckMessage(
                text = stringResource(R.string.deck_empty),
                modifier = Modifier.align(Alignment.Center),
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                )
            }
            if (state.canUndo) {
                UndoButton(
                    enabled = true,
                    onClick = { onEvent(DeckEvent.Undo) },
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                )
            }
        }

        is DeckUiState.Loaded -> {
            LoadedDeck(state = state, onEvent = onEvent)
        }
    }
}

/**
 * Carte du dessus et boutons de décision. Les boutons font sortir la carte comme un geste avant
 * d'envoyer la décision ; « Annuler » est actif dès qu'une décision peut être annulée.
 */
@Composable
private fun LoadedDeck(
    state: DeckUiState.Loaded,
    onEvent: (DeckEvent) -> Unit,
) {
    // Sens demandé par un bouton pour la carte du dessus ; 0 tant qu'aucun bouton n'est touché.
    var requestedDirection by remember(state.current.id) { mutableFloatStateOf(0f) }
    Column(modifier = Modifier.fillMaxSize()) {
        key(state.current.id) {
            SwipeableMediaCard(
                card = state.current,
                onDecision = onEvent,
                requestedDirection = requestedDirection,
                returningFrom = state.returningFrom,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
            )
        }
        // Un second tap pendant la sortie est ignoré : il ne trie pas la carte suivante.
        DecisionButtons(
            canUndo = state.canUndo,
            onMarkForDeletion = { if (requestedDirection == 0f) requestedDirection = -1f },
            onUndo = { if (requestedDirection == 0f) onEvent(DeckEvent.Undo) },
            onKeep = { if (requestedDirection == 0f) requestedDirection = 1f },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
        )
    }
}

/** Message centré de la pile (erreur, pile vide), avec un contenu d'accompagnement. */
@Composable
private fun DeckMessage(
    text: String,
    modifier: Modifier = Modifier,
    extra: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        extra()
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
    }
}

@PreviewLightDark
@Composable
private fun DeckScreenPreview() {
    CompositionLocalProvider(LocalImageLoader provides ImageLoader(LocalContext.current)) {
        PhotoSwipeTheme(dynamicColor = false) {
            DeckScreen(
                day = LocalDate.parse("2026-05-14"),
                state = DeckUiState.Loaded(listOf(PREVIEW_CARD)),
                onEvent = {},
                onBack = {},
            )
        }
    }
}
