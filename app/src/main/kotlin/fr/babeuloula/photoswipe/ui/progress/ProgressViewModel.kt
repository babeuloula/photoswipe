package fr.babeuloula.photoswipe.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.ActivityReport
import fr.babeuloula.photoswipe.domain.model.Streak
import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import fr.babeuloula.photoswipe.domain.usecase.GetActivityReport
import fr.babeuloula.photoswipe.domain.usecase.GetStreak
import fr.babeuloula.photoswipe.domain.usecase.GetUsageStatistics
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** État de la page « Progression » (PS-153). */
sealed interface ProgressUiState {
    /** Lecture en cours. */
    data object Loading : ProgressUiState

    /** Les statistiques n'ont pas pu être lues. */
    data object Error : ProgressUiState

    /**
     * Progression lue.
     *
     * @property statistics statistiques d'utilisation depuis l'installation (PS-122).
     * @property activity activité de tri des derniers mois, pour les graphiques (PS-154).
     * @property streak série de jours en cours et meilleure série (PS-155).
     */
    data class Loaded(
        val statistics: UsageStatistics,
        val activity: ActivityReport,
        val streak: Streak = Streak(),
    ) : ProgressUiState
}

/** Action de l'utilisateur ou du système sur la page « Progression ». */
sealed interface ProgressEvent {
    /** Flèche de retour. */
    data object Back : ProgressEvent

    /** « Réessayer » après une erreur. */
    data object Retry : ProgressEvent

    /** Retour sur l'écran : un tri a pu changer les chiffres. */
    data object Resumed : ProgressEvent
}

/** Navigation demandée par la page « Progression ». */
sealed interface ProgressEffect {
    /** Revenir à l'écran précédent. */
    data object Close : ProgressEffect
}

/**
 * ViewModel de la page « Progression » (PS-153) : il lit les statistiques d'utilisation et
 * l'activité des derniers mois (PS-154) et la série de jours (PS-155), et les relit à chaque retour sur l'écran. Les prochains blocs de la page (série, calendrier, badges)
 * s'y ajouteront.
 */
@HiltViewModel
class ProgressViewModel
    @Inject
    constructor(
        private val getUsageStatistics: GetUsageStatistics,
        private val getActivityReport: GetActivityReport,
        private val getStreak: GetStreak,
    ) : ViewModel() {
        private val state = MutableStateFlow<ProgressUiState>(ProgressUiState.Loading)
        private val effects = Channel<ProgressEffect>(Channel.BUFFERED)

        /** État de la page. */
        val uiState: StateFlow<ProgressUiState> = state.asStateFlow()

        /** Navigations à exécuter, chacune livrée une seule fois. */
        val uiEffects: Flow<ProgressEffect> = effects.receiveAsFlow()

        init {
            load()
        }

        /** Traite une action de l'utilisateur. */
        fun onEvent(event: ProgressEvent) {
            when (event) {
                ProgressEvent.Back -> {
                    effects.trySend(ProgressEffect.Close)
                }

                ProgressEvent.Retry -> {
                    state.value = ProgressUiState.Loading
                    load()
                }

                ProgressEvent.Resumed -> {
                    load()
                }
            }
        }

        /** Lit les statistiques et l'activité ; une erreur ne remplace pas des chiffres déjà affichés. */
        private fun load() {
            viewModelScope.launch {
                runCatching { ProgressUiState.Loaded(getUsageStatistics(), getActivityReport(), getStreak()) }
                    .onSuccess { state.value = it }
                    .onFailure { if (state.value !is ProgressUiState.Loaded) state.value = ProgressUiState.Error }
            }
        }
    }
