package fr.babeuloula.photoswipe.ui.dayoverview

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaId
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Vue d'ensemble d'un jour reliée à son ViewModel (PS-139) : elle exécute les navigations, annonce
 * un tri par lot avec « Annuler » (PS-159) et relit les souvenirs à chaque retour sur l'écran.
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
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                DayOverviewEffect.Close -> {
                    onBack()
                }

                is DayOverviewEffect.OpenDeck -> {
                    onOpenDeck(effect.day, effect.first)
                }

                // Lancé à part : le message attend sa fin sans retenir les effets suivants.
                is DayOverviewEffect.BatchSorted -> {
                    launch {
                        val message =
                            resources.getQuantityString(
                                if (effect.kept) {
                                    R.plurals.day_overview_batch_kept
                                } else {
                                    R.plurals.day_overview_batch_marked
                                },
                                effect.count,
                                effect.count,
                            )
                        val result =
                            snackbarHostState.showSnackbar(
                                message,
                                actionLabel = resources.getString(R.string.deck_undo),
                                duration = SnackbarDuration.Long,
                            )
                        if (result == SnackbarResult.ActionPerformed) viewModel.onEvent(DayOverviewEvent.UndoBatch)
                    }
                }
            }
        }
    }
    DayOverviewScreen(
        day = viewModel.day,
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
