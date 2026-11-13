package fr.babeuloula.photoswipe.ui.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Page « Progression » reliée à son ViewModel (PS-153) : elle exécute le retour et relit les
 * chiffres à chaque retour sur l'écran.
 */
@Composable
fun ProgressRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onEvent(ProgressEvent.Resumed) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                ProgressEffect.Close -> onBack()
            }
        }
    }
    ProgressScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}
