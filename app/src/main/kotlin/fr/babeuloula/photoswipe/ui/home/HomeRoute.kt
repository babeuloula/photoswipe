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
 * Accueil relié à son ViewModel : il exécute les navigations demandées, et annonce ce que la
 * confirmation du résumé vient de changer dans la galerie ([confirmed]).
 */
@Composable
fun HomeRoute(
    navigation: HomeNavigation,
    modifier: Modifier = Modifier,
    confirmed: ConfirmationAnnouncement? = null,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val confirmedMessage = confirmed?.let { confirmationMessage(it) }
    // Résultat de la confirmation du résumé : annoncé une fois, puis oublié. Le message part dans sa
    // propre coroutine : l'oubli fait disparaître [confirmed], ce qui relance cet effet sans l'annuler.
    LaunchedEffect(confirmedMessage) {
        if (confirmed != null && confirmedMessage != null) {
            scope.launch { snackbarHostState.showSnackbar(confirmedMessage, duration = SnackbarDuration.Long) }
            confirmed.markAnnounced()
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

/** Message de l'annonce : la corbeille, puis les favoris, chacun s'il a eu lieu. */
@Composable
private fun confirmationMessage(announcement: ConfirmationAnnouncement): String =
    listOfNotNull(
        announcement.trashedCount.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.summary_trashed, it, it) },
        announcement.favoritedCount.takeIf { it > 0 }?.let {
            pluralStringResource(
                R.plurals.summary_favorited,
                it,
                it,
            )
        },
    ).joinToString(" ")

/**
 * Ce que la confirmation du résumé vient de changer dans la galerie, que l'accueil annonce une fois.
 *
 * @property trashedCount médias envoyés à la corbeille.
 * @property favoritedCount médias ajoutés aux favoris de la galerie ; l'un des deux au moins est positif.
 * @property markAnnounced appelé quand l'annonce part, pour ne pas la répéter.
 */
class ConfirmationAnnouncement(
    val trashedCount: Int,
    val favoritedCount: Int,
    val markAnnounced: () -> Unit,
)

/**
 * Navigations demandées par l'accueil, exécutées par le graphe de navigation.
 *
 * @property openDeck ouvre la pile d'un jour.
 * @property openSettings ouvre les réglages.
 * @property openSummary ouvre le résumé des décisions en attente.
 */
class HomeNavigation(
    val openDeck: (LocalDate) -> Unit,
    val openSettings: () -> Unit,
    val openSummary: () -> Unit,
)
