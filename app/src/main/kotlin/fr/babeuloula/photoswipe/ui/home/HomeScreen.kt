package fr.babeuloula.photoswipe.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.ui.common.CenteredContent
import fr.babeuloula.photoswipe.ui.common.currentWindowLayout
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Accueil (SPEC §4.2) : carte « Aujourd'hui » et les 6 jours précédents, chacun avec son nombre de
 * médias à trier ; un tap ouvre la pile du jour. Une bannière signale les suppressions en attente
 * et ouvre le résumé. Gère le chargement et l'erreur.
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
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.home_open_settings))
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
                    HomeDays(
                        state = state,
                        onOpenDay = { onEvent(HomeEvent.OpenDay(it)) },
                        onOpenPendingDeletions = { onEvent(HomeEvent.OpenPendingDeletions) },
                    )
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
 * les 7 jours, un état vide dédié remplace la carte et la liste (PS-40). Sur grand écran ou en
 * paysage, la carte et la liste sont côte à côte ; en largeur moyenne, la liste est centrée.
 */
@Composable
private fun HomeDays(
    state: HomeUiState.Loaded,
    onOpenDay: (LocalDate) -> Unit,
    onOpenPendingDeletions: () -> Unit,
) {
    if (currentWindowLayout().isTwoPane && state.hasMemories) {
        // Grand écran ou paysage : « Aujourd'hui » à gauche, les jours à rattraper à droite.
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.pendingDeletionCount > 0) {
                    PendingDeletionsBanner(state.pendingDeletionCount, onClick = onOpenPendingDeletions)
                }
                TodayCard(state.today, onClick = { onOpenDay(state.today.date) })
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item { PreviousDaysTitle(Modifier.padding(start = 8.dp)) }
                items(state.previousDays, key = { it.date.toEpochDay() }) { day ->
                    PreviousDay(day, onClick = { onOpenDay(day.date) })
                }
            }
        }
        return
    }
    CenteredContent {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (state.pendingDeletionCount > 0) {
                item { PendingDeletionsBanner(state.pendingDeletionCount, onClick = onOpenPendingDeletions) }
            }
            if (!state.hasMemories) {
                item { NoMemories() }
            } else {
                item { TodayCard(state.today, onClick = { onOpenDay(state.today.date) }) }
                item { PreviousDaysTitle(Modifier.padding(top = 16.dp, start = 8.dp)) }
                items(state.previousDays, key = { it.date.toEpochDay() }) { day ->
                    PreviousDay(day, onClick = { onOpenDay(day.date) })
                }
            }
        }
    }
}

/** Titre « À rattraper », annoncé comme tel par TalkBack. */
@Composable
private fun PreviousDaysTitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.home_previous_days),
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.semantics { heading() },
    )
}

/** Un jour à rattraper : sa date et son nombre de médias à trier ; grisé et inactif s'il n'y en a aucun. */
@Composable
private fun PreviousDay(
    day: ReviewDay,
    onClick: () -> Unit,
) {
    val hasMedia = day.remainingCount > 0
    ListItem(
        headlineContent = { Text(day.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))) },
        supportingContent = {
            Text(
                pluralStringResource(R.plurals.home_remaining, day.remainingCount, day.remainingCount),
            )
        },
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable(enabled = hasMedia, onClick = onClick)
                .alpha(if (hasMedia) 1f else DISABLED_ALPHA),
    )
}

/**
 * Bannière des suppressions en attente (SPEC §3.4) : des médias sont marqués mais pas encore
 * envoyés à la corbeille. Un tap ouvre le résumé pour les confirmer.
 */
@Composable
private fun PendingDeletionsBanner(
    count: Int,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = pluralStringResource(R.plurals.home_pending_deletions, count, count),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClick) { Text(stringResource(R.string.home_pending_deletions_review)) }
        }
    }
}

/** Opacité d'un jour sans média, celle des contenus désactivés de Material 3. */
private const val DISABLED_ALPHA = 0.38f

/**
 * Carte « Aujourd'hui » : la date et le nombre de médias à trier ; elle ouvre la pile du jour.
 * Sans rien à trier, elle le dit clairement (tout est trié, ou aucun souvenir ce jour-là), invite à
 * rattraper les jours précédents, et devient inactive.
 */
@Composable
private fun TodayCard(
    today: ReviewDay,
    onClick: () -> Unit,
) {
    val hasMedia = today.remainingCount > 0
    ElevatedCard(onClick = onClick, enabled = hasMedia, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.home_today),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = today.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hasMedia) {
                Text(
                    text = pluralStringResource(R.plurals.home_remaining, today.remainingCount, today.remainingCount),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                NothingToSortToday(allSorted = today.memoryCount > 0)
            }
        }
    }
}

/** Rien à trier aujourd'hui : tout est trié ([allSorted]) ou aucun souvenir ce jour-là. */
@Composable
private fun NothingToSortToday(allSorted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            imageVector = if (allSorted) Icons.Filled.CheckCircle else Icons.Filled.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(if (allSorted) R.string.home_today_done else R.string.home_today_no_memories),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    Text(
        text = stringResource(R.string.home_today_come_back),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Aucun souvenir sur les 7 jours de la fenêtre : galerie récente, ou jours sans photo les années passées. */
@Composable
private fun NoMemories() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.home_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

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
        today = ReviewDay(LocalDate.parse("2026-07-15"), remainingCount = 1),
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
    PhotoSwipeTheme(dynamicColor = false) {
        HomeScreen(state = PREVIEW_DAYS, onEvent = {})
    }
}
