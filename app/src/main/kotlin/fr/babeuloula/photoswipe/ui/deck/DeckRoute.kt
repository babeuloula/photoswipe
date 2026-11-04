package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Pile de swipe reliée à son ViewModel : elle exécute les navigations demandées. */
@Composable
fun DeckRoute(
    onBack: () -> Unit,
    onOpenSummary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeckViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                DeckEffect.OpenSummary -> onOpenSummary()
            }
        }
    }
    DeckScreen(day = viewModel.day, state = state, onEvent = viewModel::onEvent, onBack = onBack, modifier = modifier)
}
