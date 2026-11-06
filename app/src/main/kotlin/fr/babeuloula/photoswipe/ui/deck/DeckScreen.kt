package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.common.currentWindowLayout
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
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.deck_title, localizedDate(LocalContext.current, day))) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(DeckEvent.Back) }) {
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
 * Place la pile et ses boutons : en dessous sur téléphone ; à côté, en colonne, sur grand écran ou
 * en paysage ([twoPane]), pour que la carte garde toute la hauteur (SPEC §8).
 */
@Composable
private fun DeckLayout(
    twoPane: Boolean,
    cards: @Composable (Modifier) -> Unit,
    buttons: @Composable (vertical: Boolean, Modifier) -> Unit,
) {
    if (twoPane) {
        Row(modifier = Modifier.fillMaxSize()) {
            cards(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp),
            )
            buttons(
                true,
                Modifier
                    .fillMaxHeight()
                    .padding(end = 24.dp),
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            cards(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            buttons(
                false,
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            )
        }
    }
}

/**
 * Carte du dessus, boutons de décision et menu favori / partage. Les boutons et le menu font sortir
 * la carte comme un geste avant d'envoyer la décision ; « Annuler » est actif dès qu'une décision
 * peut être annulée.
 */
@Composable
private fun LoadedDeck(
    state: DeckUiState.Loaded,
    onEvent: (DeckEvent) -> Unit,
) {
    // Sortie demandée par un bouton ou le menu pour la carte du dessus ; `null` tant qu'il n'y en a pas.
    var requestedExit by remember(state.current.id) { mutableStateOf<CardSide?>(null) }
    // Pendant la sortie de la carte, les boutons sont ignorés : un second tap ne trie pas la suivante.
    val onButton: (DeckButton) -> Unit = { button ->
        if (requestedExit == null) {
            when (button) {
                DeckButton.MARK_FOR_DELETION -> requestedExit = CardSide.LEFT
                DeckButton.KEEP -> requestedExit = CardSide.RIGHT
                DeckButton.UNDO -> onEvent(DeckEvent.Undo)
                DeckButton.MENU -> onEvent(DeckEvent.OpenMenu)
            }
        }
    }
    DeckLayout(
        twoPane = currentWindowLayout().isTwoPane,
        cards = { cardModifier ->
            CardStack(state = state, requestedExit = requestedExit, onEvent = onEvent, modifier = cardModifier)
        },
        buttons = { vertical, buttonsModifier ->
            DecisionButtons(
                canUndo = state.canUndo,
                onClick = onButton,
                vertical = vertical,
                modifier = buttonsModifier,
            )
        },
    )
    if (state.menuOpen) {
        CardMenu(
            onFavorite = {
                onEvent(DeckEvent.CloseMenu)
                requestedExit = CardSide.TOP
            },
            onShare = { onEvent(DeckEvent.Share) },
            onDismiss = { onEvent(DeckEvent.CloseMenu) },
        )
    }
}

/**
 * Carte du dessus, à trier, posée sur la suivante. La suivante est composée à la même taille pour
 * que son image soit chargée pendant le tri de la carte du dessus et serve telle quelle à son
 * arrivée ; elle est cachée à TalkBack, qui ne lit que la carte à trier.
 */
@Composable
private fun CardStack(
    state: DeckUiState.Loaded,
    requestedExit: CardSide?,
    onEvent: (DeckEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        state.next?.let { next ->
            key(next.id) {
                MediaCard(
                    card = next,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .clearAndSetSemantics { testTag = NEXT_CARD_TAG },
                )
            }
        }
        key(state.current.id) {
            SwipeableMediaCard(
                card = state.current,
                onEvent = onEvent,
                requestedExit = requestedExit,
                returningFrom = state.returningFrom,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Menu de la carte du dessus (SPEC §2), en feuille du bas Material 3 : « Ajouter aux favoris » et
 * « Partager » ; le fermer sans choisir laisse la carte en place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardMenu(
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.deck_favorite)) },
            leadingContent = { Icon(Icons.Filled.Favorite, contentDescription = null) },
            modifier = Modifier.clickable(onClick = onFavorite),
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.deck_share)) },
            leadingContent = { Icon(Icons.Filled.Share, contentDescription = null) },
            modifier =
                Modifier
                    .clickable(onClick = onShare)
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
            )
        }
    }
}

/** Étiquette de test de la carte suivante, préparée sous celle du dessus. */
internal const val NEXT_CARD_TAG = "next_card"
