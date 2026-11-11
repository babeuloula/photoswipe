package fr.babeuloula.photoswipe.ui.deck

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.babeuloula.photoswipe.R
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Pile de swipe reliée à son ViewModel : elle exécute les navigations, lance la demande de favori
 * de la galerie et ouvre la feuille de partage, ou signale par un message qu'un média ne peut pas
 * être partagé.
 */
@Composable
fun DeckRoute(
    onBack: () -> Unit,
    onOpenSummary: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenOverview: (LocalDate) -> Unit = {},
    viewModel: DeckViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.deck_share)
    val shareFailed = stringResource(R.string.deck_share_failed)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val favoriteRequest =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            viewModel.onEvent(DeckEvent.FavoriteRequestAnswered(confirmed = result.resultCode == Activity.RESULT_OK))
        }
    val unfavoriteRequest =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            viewModel.onEvent(DeckEvent.UnfavoriteRequestAnswered(confirmed = result.resultCode == Activity.RESULT_OK))
        }
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                DeckEffect.OpenSummary -> {
                    onOpenSummary()
                }

                DeckEffect.Close -> {
                    onBack()
                }

                is DeckEffect.LaunchFavoriteRequest -> {
                    favoriteRequest.launch(IntentSenderRequest.Builder(effect.intentSender).build())
                }

                is DeckEffect.LaunchUnfavoriteRequest -> {
                    unfavoriteRequest.launch(IntentSenderRequest.Builder(effect.intentSender).build())
                }

                is DeckEffect.Share -> {
                    val shared = launchShare(shareIntent(effect.uri, effect.type, shareTitle), context::startActivity)
                    if (!shared) scope.launch { snackbarHostState.showSnackbar(shareFailed) }
                }
            }
        }
    }
    // Un seul lecteur vidéo pour toute la pile, libéré en quittant l'écran (SPEC §7).
    CompositionLocalProvider(LocalCardVideoPlayer provides rememberCardVideoPlayer()) {
        DeckScreen(
            day = viewModel.day,
            state = state,
            onEvent = viewModel::onEvent,
            modifier = modifier,
            snackbarHostState = snackbarHostState,
            onOpenOverview = viewModel.day?.let { day -> { onOpenOverview(day) } },
        )
    }
}
