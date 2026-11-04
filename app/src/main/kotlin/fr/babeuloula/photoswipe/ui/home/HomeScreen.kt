package fr.babeuloula.photoswipe.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Accueil (SPEC §4.2) : carte « Aujourd'hui » et les 6 jours précédents, chacun avec son nombre de
 * médias à trier ; un tap ouvre la pile du jour. Gère le chargement et l'erreur. La bannière des
 * suppressions en attente arrive avec PS-39.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
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
                    HomeDays(state, onOpenDay = { onEvent(HomeEvent.OpenDay(it)) })
                }
            }
        }
    }
}

/**
 * Carte « Aujourd'hui », puis les 6 jours précédents à rattraper.
 *
 * Un jour sans média à trier reste dans la liste, grisé et sans action : la semaine garde ses
 * 7 jours, et l'utilisateur voit qu'il n'a rien manqué (choix de PS-38).
 */
@Composable
private fun HomeDays(
    state: HomeUiState.Loaded,
    onOpenDay: (LocalDate) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { TodayCard(state.today, onClick = { onOpenDay(state.today.date) }) }
        item {
            Text(
                text = stringResource(R.string.home_previous_days),
                style = MaterialTheme.typography.titleMedium,
                modifier =
                    Modifier
                        .padding(top = 16.dp, start = 8.dp)
                        .semantics { heading() },
            )
        }
        items(state.previousDays, key = { it.date.toEpochDay() }) { day ->
            PreviousDay(day, onClick = { onOpenDay(day.date) })
        }
    }
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

/** Opacité d'un jour sans média, celle des contenus désactivés de Material 3. */
private const val DISABLED_ALPHA = 0.38f

/** Carte « Aujourd'hui » : la date et le nombre de médias à trier ; elle ouvre la pile du jour. */
@Composable
private fun TodayCard(
    today: ReviewDay,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
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
            Text(
                text = pluralStringResource(R.plurals.home_remaining, today.remainingCount, today.remainingCount),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
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
    )

@PreviewLightDark
@Composable
private fun HomeScreenPreview() {
    PhotoSwipeTheme(dynamicColor = false) {
        HomeScreen(state = PREVIEW_DAYS, onEvent = {})
    }
}
