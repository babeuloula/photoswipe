package fr.babeuloula.photoswipe.ui.summary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Résumé relié à son ViewModel : il exécute les navigations demandées. */
@Composable
fun SummaryRoute(
    onGoHome: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                SummaryEffect.GoHome -> onGoHome()
                SummaryEffect.Close -> onBack()
            }
        }
    }
    SummaryScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}
