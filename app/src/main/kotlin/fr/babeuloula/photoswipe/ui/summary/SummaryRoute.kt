package fr.babeuloula.photoswipe.ui.summary

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
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
 * corbeille et renvoie leur réponse au ViewModel, puis annonce le résultat.
 */
@Composable
fun SummaryRoute(
    onGoHome: () -> Unit,
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
                SummaryEffect.GoHome -> {
                    onGoHome()
                }

                SummaryEffect.Close -> {
                    onBack()
                }

                is SummaryEffect.LaunchSystemRequest -> {
                    systemDialog.launch(IntentSenderRequest.Builder(effect.intentSender).build())
                }

                is SummaryEffect.Trashed -> {
                    val message =
                        resources.getQuantityString(
                            R.plurals.summary_trashed,
                            effect.trashedCount,
                            effect.trashedCount,
                        )
                    scope.launch { snackbarHostState.showSnackbar(message) }
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
