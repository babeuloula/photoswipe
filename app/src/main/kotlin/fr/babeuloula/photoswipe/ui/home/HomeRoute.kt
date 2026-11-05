package fr.babeuloula.photoswipe.ui.home

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.babeuloula.photoswipe.R
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Accueil relié à son ViewModel : il exécute les navigations demandées, et annonce les médias
 * envoyés à la corbeille depuis le résumé ([trashed]).
 */
@Composable
fun HomeRoute(
    navigation: HomeNavigation,
    modifier: Modifier = Modifier,
    trashed: TrashedAnnouncement? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val trashedMessage = trashed?.let { pluralStringResource(R.plurals.summary_trashed, it.count, it.count) }
    // Résultat de la confirmation du résumé : annoncé une fois, puis oublié. Le message part dans sa
    // propre coroutine : l'oubli fait disparaître [trashed], ce qui relance cet effet sans l'annuler.
    LaunchedEffect(trashedMessage) {
        if (trashed != null && trashedMessage != null) {
            scope.launch { snackbarHostState.showSnackbar(trashedMessage, duration = SnackbarDuration.Long) }
            trashed.markAnnounced()
        }
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.onEvent(HomeEvent.Resumed)
        onPauseOrDispose {}
    }
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                is HomeEffect.OpenDeck -> navigation.openDeck(effect.date)
                HomeEffect.OpenSettings -> navigation.openSettings()
                HomeEffect.OpenSummary -> navigation.openSummary()
            }
        }
    }
    HomeScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier, snackbarHostState = snackbarHostState)
}

/**
 * Médias envoyés à la corbeille depuis le résumé, que l'accueil annonce une fois.
 *
 * @property count nombre de médias, toujours positif.
 * @property markAnnounced appelé quand l'annonce part, pour ne pas la répéter.
 */
class TrashedAnnouncement(
    val count: Int,
    val markAnnounced: () -> Unit,
)

/** Navigations demandées par l'accueil, exécutées par le graphe de navigation. */
class HomeNavigation(
    val openDeck: (LocalDate) -> Unit,
    val openSettings: () -> Unit,
    val openSummary: () -> Unit,
)
