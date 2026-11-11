package fr.babeuloula.photoswipe.ui.deck

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.ui.common.FilmEdgeLabel
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.common.currentWindowLayout
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import fr.babeuloula.photoswipe.ui.theme.monoLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Pile de swipe d'un jour (SPEC §4.3) : la carte plein écran du média du dessus, à glisser à droite
 * pour la garder ou à gauche pour la marquer à supprimer, ou à trier avec les boutons équivalents
 * placés dessous, avec retour et
 * « Terminer » dans la barre du haut. Sans [day], c'est la pile des médias marqués (PS-112).
 * Gère le chargement, l'erreur et la pile vide.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckScreen(
    day: LocalDate?,
    state: DeckUiState,
    onEvent: (DeckEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onOpenOverview: (() -> Unit)? = null,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // Sans jour : la pile des médias marqués, ouverte depuis le résumé (PS-112).
                        day?.let { stringResource(R.string.deck_title, dayAndMonth(LocalContext.current, it)) }
                            ?: stringResource(R.string.deck_marked_title),
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(DeckEvent.Back) }) {
                        Icon(
                            painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.navigate_back),
                        )
                    }
                },
                actions = {
                    // Tous les souvenirs du jour d'un coup d'œil (PS-139) ; pas pour la pile des marqués.
                    onOpenOverview?.let {
                        IconButton(onClick = it) {
                            Icon(
                                painterResource(R.drawable.ic_grid_view),
                                contentDescription = stringResource(R.string.day_overview_open),
                            )
                        }
                    }
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
            Column(Modifier.fillMaxSize()) {
                if (state is DeckUiState.Loaded) {
                    DeckProgress(
                        position = state.position,
                        total = state.total,
                        yearProgress = YearProgress(state.year, state.yearPosition, state.yearTotal),
                    )
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    DeckContent(state = state, onEvent = onEvent)
                }
            }
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
            EmptyDeck(canUndo = state.canUndo, onEvent = onEvent)
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
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
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
internal fun LoadedDeck(
    state: DeckUiState.Loaded,
    onEvent: (DeckEvent) -> Unit,
) {
    // Sortie demandée par un bouton ou le menu pour la carte du dessus ; `null` tant qu'il n'y en a pas.
    var requestedExit by remember(state.current.id) { mutableStateOf<CardSide?>(null) }
    // Geste en cours sur la carte du dessus : bouton mis en avant, remontée de la carte suivante.
    var indication by remember(state.current.id) { mutableStateOf<SwipeIndication?>(null) }
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
    // Vidéo en plein écran, enregistrée ici, hors de la carte : elle reste ouverte quand une
    // rotation recrée l'activité (PS-126).
    val fullscreenVideo = rememberSaveable { mutableStateOf<String?>(null) }
    // Photo ouverte en grand (identifiant du média), enregistrée ici pour la même raison : en
    // paysage, la pile passe en deux volets et la carte change de place (PS-123).
    val openPhoto = rememberSaveable { mutableStateOf<Long?>(null) }
    CompositionLocalProvider(LocalFullscreenVideo provides fullscreenVideo, LocalOpenPhoto provides openPhoto) {
        DeckLayout(
            twoPane = currentWindowLayout().isTwoPane,
            cards = { cardModifier ->
                CardStack(
                    state = state,
                    requestedExit = requestedExit,
                    onEvent = onEvent,
                    onIndicationChange = { indication = it },
                    nextCardProgress = { indication?.progress ?: 0f },
                    modifier = cardModifier,
                )
            },
            buttons = { vertical, buttonsModifier ->
                DecisionButtons(
                    canUndo = state.canUndo,
                    onClick = onButton,
                    vertical = vertical,
                    highlighted = indication?.side?.takeIf { (indication?.progress ?: 0f) > 0f },
                    modifier = buttonsModifier,
                )
            },
        )
    }
    if (state.menuOpen) {
        CardMenu(
            card = state.current,
            onFavorite = {
                onEvent(DeckEvent.CloseMenu)
                requestedExit = CardSide.TOP
            },
            onUnfavorite = { onEvent(DeckEvent.Unfavorite) },
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
    onIndicationChange: (SwipeIndication?) -> Unit,
    nextCardProgress: () -> Float,
    modifier: Modifier = Modifier,
) {
    // Photo du dessus ouverte en grand (PS-117), tenue par la pile : gardée quand une rotation
    // recrée l'activité (PS-123) ; à défaut, par la pile de cartes elle-même.
    val ownOpenPhoto = rememberSaveable { mutableStateOf<Long?>(null) }
    val openPhoto = LocalOpenPhoto.current ?: ownOpenPhoto
    if (openPhoto.value == state.current.id.value) {
        PhotoViewer(card = state.current, onClose = { openPhoto.value = null })
    }
    Box(modifier = modifier) {
        state.next?.let { next ->
            key(next.id) {
                MediaCard(
                    card = next,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                // Posée un peu plus bas et plus petite, elle remonte à mesure que la carte du dessus part.
                                val progress = nextCardProgress()
                                val scale = lerp(NEXT_CARD_SCALE, 1f, progress)
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin(NEXT_CARD_PIVOT_X, 1f)
                                translationY = lerp(NEXT_CARD_OFFSET.toPx(), 0f, progress)
                            }.clearAndSetSemantics { testTag = NEXT_CARD_TAG },
                )
            }
        }
        key(state.current.id) {
            SwipeableMediaCard(
                card = state.current,
                onEvent = onEvent,
                requestedExit = requestedExit,
                returningFrom = state.returningFrom,
                onIndicationChange = onIndicationChange,
                onOpen = { openPhoto.value = state.current.id.value },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * Menu de la carte du dessus (SPEC §2), en feuille du bas Material 3 : « Ajouter aux favoris », ou
 * « Retirer des favoris » pour une carte déjà en favori (PS-124), et « Partager » ; le fermer sans
 * choisir laisse la carte en place.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardMenu(
    card: MediaCardUiState,
    onFavorite: () -> Unit,
    onUnfavorite: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        MenuPreview(card)
        // Carte déjà en favori (cœur) : le menu propose de l'en retirer, sans trier la carte (PS-124).
        if (card.favorite) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.deck_unfavorite)) },
                leadingContent = { Icon(painterResource(R.drawable.ic_favorite_filled), contentDescription = null) },
                modifier = Modifier.clickable(onClick = onUnfavorite),
            )
        } else {
            ListItem(
                headlineContent = { Text(stringResource(R.string.deck_favorite)) },
                leadingContent = { Icon(painterResource(R.drawable.ic_favorite), contentDescription = null) },
                modifier = Modifier.clickable(onClick = onFavorite),
            )
        }
        ListItem(
            headlineContent = { Text(stringResource(R.string.deck_share)) },
            leadingContent = { Icon(painterResource(R.drawable.ic_share), contentDescription = null) },
            modifier =
                Modifier
                    .clickable(onClick = onShare)
                    .padding(bottom = 24.dp),
        )
    }
}

/**
 * En-tête du menu : la vignette du média, son repère « Il y a 7 ans · 6 nov. 2019 », son type et son
 * poids, pour savoir à quelle carte s'applique le menu.
 */
@Composable
private fun MenuPreview(card: MediaCardUiState) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = card.uri,
            contentDescription = null,
            imageLoader = LocalImageLoader.current,
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
            error = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(width = 56.dp, height = 72.dp).clip(MaterialTheme.shapes.medium),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FilmEdgeLabel(memoryLabel(context, card.yearsAgo, card.takenOn))
            Text(
                text =
                    stringResource(
                        if (card.type == MediaType.VIDEO) R.string.deck_sheet_video else R.string.deck_sheet_photo,
                        fileSizeLabel(context, card.sizeBytes),
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        PhotoSwipeTheme {
            DeckScreen(
                day = LocalDate.parse("2026-05-14"),
                state = DeckUiState.Loaded(listOf(PREVIEW_CARD)),
                onEvent = {},
            )
        }
    }
}

/** Pivot horizontal de la carte suivante : son milieu, pour qu'elle grandisse vers le haut depuis son bas. */
private const val NEXT_CARD_PIVOT_X = 0.5f

/** Taille de la carte suivante au repos, avant de remonter sous la carte du dessus. */
private const val NEXT_CARD_SCALE = 0.92f

/** Décalage vers le bas de la carte suivante au repos. */
private val NEXT_CARD_OFFSET = 14.dp

/** Étiquette de test de la carte suivante, préparée sous celle du dessus. */
internal const val NEXT_CARD_TAG = "next_card"
