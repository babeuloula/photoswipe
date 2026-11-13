package fr.babeuloula.photoswipe.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.ActivityReport
import fr.babeuloula.photoswipe.domain.model.Streak
import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import fr.babeuloula.photoswipe.domain.model.YearMemories
import fr.babeuloula.photoswipe.domain.usecase.GetActivityReport
import fr.babeuloula.photoswipe.domain.usecase.GetStreak
import fr.babeuloula.photoswipe.domain.usecase.GetUsageStatistics
import fr.babeuloula.photoswipe.domain.usecase.GetYearMemories
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
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
     * @property calendar souvenirs de chaque jour de l'année (PS-157), `null` tant qu'ils sont lus
     * ou s'ils n'ont pas pu l'être : la galerie entière est lue, plus lentement que le reste.
     * @property today jour courant, repéré dans le calendrier.
     */
    data class Loaded(
        val statistics: UsageStatistics,
        val activity: ActivityReport,
        val streak: Streak = Streak(),
        val calendar: YearMemories? = null,
        val today: LocalDate? = null,
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

    /**
     * Appui sur un jour du calendrier qui a des souvenirs (PS-157).
     *
     * @property day jour touché.
     */
    data class OpenDay(
        val day: LocalDate,
    ) : ProgressEvent
}

/** Navigation demandée par la page « Progression ». */
sealed interface ProgressEffect {
    /** Revenir à l'écran précédent. */
    data object Close : ProgressEffect

    /**
     * Ouvrir la vue d'ensemble du jour [day], d'où l'on lance sa pile (PS-157).
     *
     * @property day jour à ouvrir.
     */
    data class OpenOverview(
        val day: LocalDate,
    ) : ProgressEffect
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
        private val getYearMemories: GetYearMemories,
        private val clock: Clock,
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

                is ProgressEvent.OpenDay -> {
                    effects.trySend(ProgressEffect.OpenOverview(event.day))
                }
            }
        }

        /** Calendrier lu en dernier, gardé d'une lecture à l'autre de la page. */
        private var calendar: YearMemories? = null

        /**
         * Lit les statistiques, l'activité et la série, puis le calendrier, plus lent, qui complète
         * la page une fois prêt. Une erreur ne remplace pas des chiffres déjà affichés ; un
         * calendrier illisible reste absent.
         */
        private fun load() {
            viewModelScope.launch {
                runCatching {
                    ProgressUiState.Loaded(
                        getUsageStatistics(),
                        getActivityReport(),
                        getStreak(),
                        calendar,
                        LocalDate.now(clock),
                    )
                }.onSuccess { state.value = it }
                    .onFailure { if (state.value !is ProgressUiState.Loaded) state.value = ProgressUiState.Error }
                runCatching { getYearMemories().first() }.onSuccess { year ->
                    calendar = year
                    state.update { if (it is ProgressUiState.Loaded) it.copy(calendar = year) else it }
                }
            }
        }
    }
