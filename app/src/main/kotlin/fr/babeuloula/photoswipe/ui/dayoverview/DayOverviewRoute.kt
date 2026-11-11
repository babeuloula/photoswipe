package fr.babeuloula.photoswipe.ui.dayoverview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.babeuloula.photoswipe.domain.model.MediaId
import java.time.LocalDate

/**
 * Vue d'ensemble d'un jour reliée à son ViewModel (PS-139) : elle exécute les navigations et relit
 * les souvenirs à chaque retour sur l'écran.
 */
@Composable
fun DayOverviewRoute(
    onBack: () -> Unit,
    onOpenDeck: (LocalDate, MediaId?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DayOverviewViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onEvent(DayOverviewEvent.Resumed) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                DayOverviewEffect.Close -> onBack()
                is DayOverviewEffect.OpenDeck -> onOpenDeck(effect.day, effect.first)
            }
        }
    }
    DayOverviewScreen(day = viewModel.day, state = state, onEvent = viewModel::onEvent, modifier = modifier)
}
