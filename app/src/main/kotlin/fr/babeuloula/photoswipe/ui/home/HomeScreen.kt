package fr.babeuloula.photoswipe.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * Accueil (SPEC §4.2) : carte « Aujourd'hui » avec le nombre de médias à trier, qui ouvre la pile
 * du jour. Gère le chargement et l'erreur. Les jours à rattraper et la bannière des suppressions en
 * attente arrivent avec PS-38 et PS-39.
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
                    .padding(innerPadding)
                    .padding(16.dp),
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
                    TodayCard(state.today, onClick = { onEvent(HomeEvent.OpenToday) })
                }
            }
        }
    }
}

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
private val PREVIEW_TODAY = ReviewDay(LocalDate.parse("2026-07-15"), remainingCount = 1)

@PreviewLightDark
@Composable
private fun HomeScreenPreview() {
    PhotoSwipeTheme(dynamicColor = false) {
        HomeScreen(state = HomeUiState.Loaded(PREVIEW_TODAY), onEvent = {})
    }
}
