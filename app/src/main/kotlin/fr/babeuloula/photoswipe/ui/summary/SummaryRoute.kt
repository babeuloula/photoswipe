package fr.babeuloula.photoswipe.ui.summary

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.babeuloula.photoswipe.R
import kotlinx.coroutines.launch

/**
 * Résumé relié à son ViewModel : il exécute les navigations, lance les dialogues système de
 * corbeille et de favoris et renvoie leur réponse au ViewModel. Quand la galerie a changé,
 * l'accueil annonce le résultat ; ici ne s'affichent que les messages des médias introuvables et
 * de la confirmation refusée ou annulée, avec « Réessayer » (SPEC §3.5).
 */
@Composable
fun SummaryRoute(
    onGoHome: (trashedCount: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val systemDialog =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            viewModel.onEvent(SummaryEvent.SystemRequestAnswered(confirmed = result.resultCode == Activity.RESULT_OK))
        }
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                is SummaryEffect.GoHome -> {
                    onGoHome(effect.trashedCount)
                }

                SummaryEffect.Close -> {
                    onBack()
                }

                is SummaryEffect.LaunchSystemRequest -> {
                    systemDialog.launch(IntentSenderRequest.Builder(effect.intentSender).build())
                }

                SummaryEffect.MarkedMediaMissing -> {
                    val message = resources.getString(R.string.summary_marked_media_missing)
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }

                is SummaryEffect.NothingApplied -> {
                    val message =
                        listOfNotNull(
                            resources.getString(R.string.summary_nothing_applied),
                            effect.stillMarkedCount.takeIf { it > 0 }?.let {
                                resources.getQuantityString(R.plurals.summary_still_marked, it, it)
                            },
                        ).joinToString(" ")
                    val retry = resources.getString(R.string.summary_retry)
                    scope.launch {
                        val result =
                            snackbarHostState.showSnackbar(
                                message,
                                actionLabel = retry,
                                duration = SnackbarDuration.Long,
                            )
                        if (result == SnackbarResult.ActionPerformed) viewModel.onEvent(SummaryEvent.Confirm)
                    }
                }
            }
        }
    }
    SummaryScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
