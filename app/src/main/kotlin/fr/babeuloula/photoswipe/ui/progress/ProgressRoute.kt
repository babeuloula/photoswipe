package fr.babeuloula.photoswipe.ui.progress

import android.content.res.Resources
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
import fr.babeuloula.photoswipe.domain.model.Badge
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Page « Progression » reliée à son ViewModel (PS-153) : elle exécute le retour et l'ouverture d'un
 * jour du calendrier (PS-157), annonce les badges obtenus (PS-158), et relit les chiffres à chaque
 * retour sur l'écran.
 */
@Composable
fun ProgressRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenOverview: (LocalDate) -> Unit = {},
    viewModel: ProgressViewModel = hiltViewModel(),
) {
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onEvent(ProgressEvent.Resumed) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                ProgressEffect.Close -> {
                    onBack()
                }

                is ProgressEffect.OpenOverview -> {
                    onOpenOverview(effect.day)
                }

                // Lancée à part : l'annonce attend sa fin sans retenir les effets suivants.
                is ProgressEffect.AnnounceBadges -> {
                    launch { snackbarHostState.showSnackbar(badgesAnnouncement(resources, effect.badges)) }
                }
            }
        }
    }
    ProgressScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/** « Nouveau badge : 7 jours de suite », ou « Nouveaux badges : … » pour plusieurs. */
private fun badgesAnnouncement(
    resources: Resources,
    badges: List<Badge>,
): String =
    resources.getQuantityString(
        R.plurals.progress_badge_new,
        badges.size,
        badges.joinToString(", ") { resources.getString(badgeName(it)) },
    )
