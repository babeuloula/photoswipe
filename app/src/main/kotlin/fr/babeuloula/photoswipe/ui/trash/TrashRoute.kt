package fr.babeuloula.photoswipe.ui.trash

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
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
import kotlinx.coroutines.launch

/**
 * Page « Corbeille » reliée à son ViewModel (PS-168) : elle lance les dialogues système de
 * restauration et de suppression définitive, renvoie leur réponse, annonce le résultat et relit la
 * corbeille à chaque retour sur l'écran.
 */
@Composable
fun TrashRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TrashViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onEvent(TrashEvent.Resumed) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val systemDialog =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            viewModel.onEvent(TrashEvent.SystemRequestAnswered(confirmed = result.resultCode == Activity.RESULT_OK))
        }
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            val message =
                when (effect) {
                    TrashEffect.Close -> {
                        onBack()
                        null
                    }

                    is TrashEffect.LaunchSystemRequest -> {
                        systemDialog.launch(IntentSenderRequest.Builder(effect.intentSender).build())
                        null
                    }

                    TrashEffect.Restored -> {
                        R.string.trash_restored
                    }

                    TrashEffect.DeletedForever -> {
                        R.string.trash_deleted_forever
                    }

                    TrashEffect.NothingChanged -> {
                        R.string.trash_nothing_changed
                    }
                }
            // Lancée à part : l'annonce attend sa fin sans retenir les effets suivants.
            if (message != null) launch { snackbarHostState.showSnackbar(resources.getString(message)) }
        }
    }
    TrashScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}
