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
import androidx.compose.runtime.key
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

        DeckUiState.Empty -> {
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
        }

        is DeckUiState.Loaded -> {
            Column(modifier = Modifier.fillMaxSize()) {
                key(state.current.id) {
                    SwipeableMediaCard(
                        card = state.current,
                        onKeep = { onEvent(DeckEvent.Keep) },
                        onMarkForDeletion = { onEvent(DeckEvent.MarkForDeletion) },
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp),
                    )
                }
                DecisionButtons(
                    onMarkForDeletion = { onEvent(DeckEvent.MarkForDeletion) },
                    onKeep = { onEvent(DeckEvent.Keep) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                )
            }
        }
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
