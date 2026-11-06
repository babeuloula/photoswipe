package fr.babeuloula.photoswipe.ui.tutorial

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Affiche le tutoriel au premier lancement, puis [content] (SPEC §4, ADR 0027). Il entoure l'accès
 * aux médias : le tutoriel passe avant la demande d'accès. [modifier] s'applique au tutoriel ;
 * [content] gère lui-même les marges des barres système.
 */
@Composable
fun TutorialGate(
    modifier: Modifier = Modifier,
    viewModel: TutorialGateViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (state) {
        TutorialGateState.CHECKING -> Box(Modifier.fillMaxSize())
        TutorialGateState.SHOWING -> TutorialScreen(onDone = viewModel::onTutorialDone, modifier = modifier)
        TutorialGateState.DONE -> content()
    }
}
