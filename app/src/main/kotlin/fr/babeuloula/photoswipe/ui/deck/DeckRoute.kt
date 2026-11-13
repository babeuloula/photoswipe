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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.share.ShareTargetReceiver
import fr.babeuloula.photoswipe.ui.common.LocalCelebration
import fr.babeuloula.photoswipe.ui.postcard.PostcardDialog
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
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onEvent(DeckEvent.Resumed) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val celebration = LocalCelebration.current
    ShakeToUndo(undoAvailable = state.undoAvailable, onUndo = { viewModel.onEvent(DeckEvent.Undo) })
    val favoriteRequest = rememberSystemRequest { viewModel.onEvent(DeckEvent.FavoriteRequestAnswered(it)) }
    val unfavoriteRequest = rememberSystemRequest { viewModel.onEvent(DeckEvent.UnfavoriteRequestAnswered(it)) }
    // Photo dont la carte postale est en aperçu (PS-177), `null` hors de l'aperçu.
    var postcard by remember { mutableStateOf<MediaCardUiState?>(null) }
    postcard?.let { card ->
        PostcardDialog(uri = card.uri, label = memoryLabel(context, card.yearsAgo, card.takenOn)) { failure ->
            postcard = null
            if (failure != null) scope.launch { snackbarHostState.showSnackbar(failure) }
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                DeckEffect.OpenSummary -> {
                    onOpenSummary()
                }

                DeckEffect.DayCompleted -> {
                    // Confettis par-dessus le résumé, ouvert sans attendre (PS-174).
                    celebration?.celebrate()
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

                is DeckEffect.SharePostcard -> {
                    postcard = effect.card
                }

                is DeckEffect.ElsewhereEffect -> {
                    val failure = launchElsewhere(context, effect)
                    if (failure != null) scope.launch { snackbarHostState.showSnackbar(failure) }
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

/** Lanceur d'une demande système (favori de la galerie) ; [onAnswer] reçoit vrai si elle est acceptée. */
@Composable
private fun rememberSystemRequest(onAnswer: (Boolean) -> Unit) =
    rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        onAnswer(result.resultCode == Activity.RESULT_OK)
    }

/** Secouer pour annuler (PS-175) : comme « Annuler », seulement s'il y a une décision à annuler. */
@Composable
private fun ShakeToUndo(
    undoAvailable: Boolean,
    onUndo: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    ShakeEffect(enabled = LocalShakeToUndo.current) {
        if (undoAvailable) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onUndo()
        }
    }
}
