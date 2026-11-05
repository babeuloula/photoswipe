package fr.babeuloula.photoswipe.ui.deck

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.babeuloula.photoswipe.R

/** Pile de swipe reliée à son ViewModel : elle exécute les navigations et ouvre la feuille de partage. */
@Composable
fun DeckRoute(
    onBack: () -> Unit,
    onOpenSummary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeckViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.deck_share)
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                DeckEffect.OpenSummary -> onOpenSummary()
                is DeckEffect.Share -> context.startActivity(shareIntent(effect.uri, effect.type, shareTitle))
            }
        }
    }
    DeckScreen(day = viewModel.day, state = state, onEvent = viewModel::onEvent, onBack = onBack, modifier = modifier)
}
