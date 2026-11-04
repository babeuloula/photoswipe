package fr.babeuloula.photoswipe.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

/** Accueil relié à son ViewModel : il exécute les navigations demandées. */
@Composable
fun HomeRoute(
    onOpenDeck: (LocalDate) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onEvent(HomeEvent.Resumed)
        onPauseOrDispose {}
    }
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                is HomeEffect.OpenDeck -> onOpenDeck(effect.day.date)
                HomeEffect.OpenSettings -> onOpenSettings()
            }
        }
    }
    HomeScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}
