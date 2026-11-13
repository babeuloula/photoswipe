package fr.babeuloula.photoswipe.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import fr.babeuloula.photoswipe.ui.common.CenteredContent
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import java.time.LocalDate

/**
 * Page « Progression » (PS-153), ouverte par le bouton de la barre du haut de l'accueil ou depuis
 * les réglages : les statistiques d'utilisation, sorties des réglages. Le contenu est centré sur
 * grand écran. Gère le chargement et l'erreur.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onEvent: (ProgressEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.progress_title)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ProgressEvent.Back) }) {
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
                ProgressUiState.Loading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

                ProgressUiState.Error -> {
                    ProgressError(
                        onRetry = { onEvent(ProgressEvent.Retry) },
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                is ProgressUiState.Loaded -> {
                    CenteredContent {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            StatisticsSection(state.statistics)
                        }
                    }
                }
            }
        }
    }
}

/** Erreur de lecture, avec un bouton pour réessayer. */
@Composable
private fun ProgressError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.progress_error),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onRetry) { Text(stringResource(R.string.home_retry)) }
    }
}

@PreviewLightDark
@Composable
private fun ProgressScreenPreview() {
    PhotoSwipeTheme {
        ProgressScreen(
            state =
                ProgressUiState.Loaded(
                    UsageStatistics(LocalDate.parse("2026-03-02"), deletedCount = 128, freedBytes = 1_200_000_000),
                ),
            onEvent = {},
        )
    }
}
