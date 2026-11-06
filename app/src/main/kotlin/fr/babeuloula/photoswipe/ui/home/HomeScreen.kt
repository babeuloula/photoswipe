package fr.babeuloula.photoswipe.ui.home

import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.ui.common.CenteredContent
import fr.babeuloula.photoswipe.ui.common.FilmEdgeLabel
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.common.currentWindowLayout
import fr.babeuloula.photoswipe.ui.theme.IbmPlexMono
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import fr.babeuloula.photoswipe.ui.theme.monoLabel
import fr.babeuloula.photoswipe.ui.theme.swipeColors
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Accueil (SPEC §4.2) : carte « Aujourd'hui » et les 6 jours précédents, chacun avec son nombre de
 * médias à trier ; un tap ouvre la pile du jour. Une bannière signale les suppressions en attente
 * et ouvre le résumé ; un conseil propose l'accès « Gestion des médias » après un premier dialogue
 * d'Android validé. Gère le chargement et l'erreur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { onEvent(HomeEvent.OpenSettings) }) {
                        Icon(
                            painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.home_open_settings),
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
                HomeUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                HomeUiState.Error -> {
                    HomeError(
                        onRetry = { onEvent(HomeEvent.Retry) },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is HomeUiState.Loaded -> {
                    HomeDays(state = state, onEvent = onEvent)
                }
            }
        }
    }
}

/**
 * Carte « Aujourd'hui », puis les 6 jours précédents à rattraper.
 *
 * Un jour sans média à trier reste dans la liste, grisé et sans action : la semaine garde ses
 * 7 jours, et l'utilisateur voit qu'il n'a rien manqué (choix de PS-38). Sans aucun souvenir sur
 * les 7 jours, la carte du jour le dit pour toute la semaine (PS-40). Sur grand écran ou en
 * paysage, la carte et la liste sont côte à côte ; en largeur moyenne, la liste est centrée.
 */
@Composable
private fun HomeDays(
    state: HomeUiState.Loaded,
    onEvent: (HomeEvent) -> Unit,
) {
    val onOpenDay = { day: LocalDate -> onEvent(HomeEvent.OpenDay(day)) }
    val today = @Composable { TodayCard(state, onOpenDay = { onOpenDay(state.today.date) }, onEvent = onEvent) }
    if (currentWindowLayout().isTwoPane) {
        // Grand écran ou paysage : la liste à gauche (jour, bannières, jours à rattraper), le détail du jour à droite.
        val withDetail = state.today.memoryCount > 0
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            LazyColumn(
                modifier = Modifier.weight(LIST_PANE_WEIGHT),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                item {
                    Box(Modifier.padding(bottom = 10.dp)) {
                        TodayCard(
                            state,
                            onOpenDay = { onOpenDay(state.today.date) },
                            onEvent = onEvent,
                            compact = withDetail,
                        )
                    }
                }
                if (state.showManageMediaTip || state.pendingDeletionCount > 0) {
                    item { Box(Modifier.padding(bottom = 10.dp)) { HomeBanners(state, onEvent) } }
                }
                item { PreviousDaysTitle() }
                previousDays(state.previousDays, onOpenDay)
            }
            if (withDetail) {
                TodayDetailPane(
                    today = state.today,
                    onOpenDay = { onOpenDay(state.today.date) },
                    onSummary = { onEvent(HomeEvent.OpenPendingDeletions) },
                    modifier = Modifier.weight(DETAIL_PANE_WEIGHT).fillMaxHeight(),
                )
            }
        }
        return
    }
    CenteredContent {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item { Box(Modifier.padding(bottom = 10.dp)) { today() } }
            if (state.showManageMediaTip || state.pendingDeletionCount > 0) {
                item { Box(Modifier.padding(bottom = 10.dp)) { HomeBanners(state, onEvent) } }
            }
            item { PreviousDaysTitle() }
            previousDays(state.previousDays, onOpenDay)
        }
    }
}

/** Part de la largeur du volet de liste, en deux volets. */
private const val LIST_PANE_WEIGHT = 1f

/** Part de la largeur du volet de détail, en deux volets : la planche-contact a besoin de place. */
private const val DETAIL_PANE_WEIGHT = 2f

/** Erreur de chargement, avec un bouton pour réessayer. */
@Composable
private fun HomeError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.home_error), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry) { Text(stringResource(R.string.home_retry)) }
    }
}

/** Jour fixe des aperçus. */
private val PREVIEW_DAYS =
    HomeUiState.Loaded(
        today = ReviewDay(LocalDate.parse("2026-07-15"), remainingCount = 1, years = 2019..2025),
        previousDays =
            listOf(
                ReviewDay(LocalDate.parse("2026-07-14"), remainingCount = 1),
                ReviewDay(LocalDate.parse("2026-07-13"), remainingCount = 0),
            ),
        pendingDeletionCount = 1,
    )

@PreviewLightDark
@Composable
private fun HomeScreenPreview() {
    PhotoSwipeTheme {
        HomeScreen(state = PREVIEW_DAYS, onEvent = {})
    }
}
