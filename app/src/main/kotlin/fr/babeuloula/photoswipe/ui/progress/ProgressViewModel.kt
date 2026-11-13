package fr.babeuloula.photoswipe.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.Achievements
import fr.babeuloula.photoswipe.domain.model.ActivityReport
import fr.babeuloula.photoswipe.domain.model.Badge
import fr.babeuloula.photoswipe.domain.model.Streak
import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import fr.babeuloula.photoswipe.domain.model.YearMemories
import fr.babeuloula.photoswipe.domain.usecase.SetFreeSpaceGoal
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
     * @property achievements badges et objectif (PS-158), `null` s'ils n'ont pas pu être lus.
     * @property goalDialogOpen vrai pendant le choix de l'objectif.
     */
    data class Loaded(
        val statistics: UsageStatistics,
        val activity: ActivityReport,
        val streak: Streak = Streak(),
        val calendar: YearMemories? = null,
        val today: LocalDate? = null,
        val achievements: Achievements? = null,
        val goalDialogOpen: Boolean = false,
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

    /** Objectif de place à libérer (PS-158). */
    sealed interface GoalEvent : ProgressEvent

    /** « Se fixer un objectif » ou « Changer » : ouvrir le choix. */
    data object PickGoal : GoalEvent

    /**
     * Objectif choisi.
     *
     * @property targetBytes octets à libérer à partir de maintenant.
     */
    data class GoalPicked(
        val targetBytes: Long,
    ) : GoalEvent

    /** Choix fermé sans rien changer. */
    data object GoalDialogDismissed : GoalEvent

    /** « Retirer » l'objectif. */
    data object RemoveGoal : GoalEvent
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

    /**
     * Annoncer les badges qui viennent d'être obtenus (PS-158), une seule fois.
     *
     * @property badges badges obtenus.
     */
    data class AnnounceBadges(
        val badges: List<Badge>,
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
        private val reads: ProgressReads,
        private val setFreeSpaceGoal: SetFreeSpaceGoal,
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

                is ProgressEvent.GoalEvent -> {
                    onGoalEvent(event)
                }
            }
        }

        /** Ouvre ou ferme le choix de l'objectif, puis fixe ou retire l'objectif et relit les badges. */
        private fun onGoalEvent(event: ProgressEvent.GoalEvent) {
            state.update {
                if (it is ProgressUiState.Loaded) {
                    it.copy(
                        goalDialogOpen = event == ProgressEvent.PickGoal,
                    )
                } else {
                    it
                }
            }
            val target =
                when (event) {
                    is ProgressEvent.GoalPicked -> event.targetBytes
                    ProgressEvent.RemoveGoal -> null
                    else -> return
                }
            viewModelScope.launch {
                runCatching { setFreeSpaceGoal(target) }
                readAchievements()
            }
        }

        /** Lit les badges et l'objectif, et annonce les badges qui viennent d'être obtenus. */
        private suspend fun readAchievements() {
            runCatching { reads.getAchievements() }.onSuccess { achievements ->
                state.update { if (it is ProgressUiState.Loaded) it.copy(achievements = achievements) else it }
                if (achievements.newlyEarned.isNotEmpty()) {
                    effects.trySend(
                        ProgressEffect.AnnounceBadges(achievements.newlyEarned),
                    )
                }
            }
        }

        /** Calendrier lu en dernier, gardé d'une lecture à l'autre de la page. */
        private var calendar: YearMemories? = null

        /**
         * Lit les statistiques, l'activité et la série, puis les badges, puis le calendrier, plus lent, qui complète
         * la page une fois prêt. Une erreur ne remplace pas des chiffres déjà affichés ; un
         * calendrier illisible reste absent.
         */
        private fun load() {
            viewModelScope.launch {
                runCatching {
                    ProgressUiState.Loaded(
                        reads.getUsageStatistics(),
                        reads.getActivityReport(),
                        reads.getStreak(),
                        calendar,
                        LocalDate.now(clock),
                        achievements = (state.value as? ProgressUiState.Loaded)?.achievements,
                    )
                }.onSuccess { state.value = it }
                    .onFailure { if (state.value !is ProgressUiState.Loaded) state.value = ProgressUiState.Error }
                readAchievements()
                runCatching { reads.getYearMemories().first() }.onSuccess { year ->
                    calendar = year
                    state.update { if (it is ProgressUiState.Loaded) it.copy(calendar = year) else it }
                }
            }
        }
    }
