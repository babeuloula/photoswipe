package fr.babeuloula.photoswipe.ui.dayoverview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.DayOverview
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.usecase.GetDayOverview
import fr.babeuloula.photoswipe.ui.deck.MediaCardUiState
import fr.babeuloula.photoswipe.ui.navigation.DayOverviewRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** État de la vue d'ensemble d'un jour (PS-139). */
sealed interface DayOverviewUiState {
    /** Souvenirs en cours de lecture. */
    data object Loading : DayOverviewUiState

    /** La galerie ou la base n'a pas pu être lue. */
    data object Error : DayOverviewUiState

    /**
     * Souvenirs du jour lus.
     *
     * @property overview souvenirs rangés par année, avec leur décision.
     * @property openPhoto photo déjà triée ouverte en grand, `null` sinon.
     */
    data class Loaded(
        val overview: DayOverview,
        val openPhoto: MediaCardUiState? = null,
    ) : DayOverviewUiState {
        /** Souvenirs qui restent à trier ce jour-là. */
        val remainingCount: Int
            get() = overview.years.sumOf { year -> year.memories.count { it.decision == null } }
    }
}

/** Action de l'utilisateur sur la vue d'ensemble. */
sealed interface DayOverviewEvent {
    /** « Réessayer » après une erreur. */
    data object Retry : DayOverviewEvent

    /** Retour sur l'écran : des décisions ont pu être prises dans la pile. */
    data object Resumed : DayOverviewEvent

    /** Flèche de retour. */
    data object Back : DayOverviewEvent

    /**
     * Souvenir touché : à trier, la pile s'ouvre sur lui ; déjà trié, une photo s'ouvre en grand.
     *
     * @property id média touché.
     */
    data class Open(
        val id: MediaId,
    ) : DayOverviewEvent

    /** Photo ouverte en grand refermée. */
    data object ClosePhoto : DayOverviewEvent

    /** « Trier les 5 restants » : la pile du jour, dans son ordre. */
    data object StartSorting : DayOverviewEvent
}

/** Navigation demandée par la vue d'ensemble. */
sealed interface DayOverviewEffect {
    /** Revenir à l'écran précédent. */
    data object Close : DayOverviewEffect

    /**
     * Ouvrir la pile du jour [day].
     *
     * @property first média à montrer en premier, `null` pour l'ordre de la pile.
     */
    data class OpenDeck(
        val day: LocalDate,
        val first: MediaId? = null,
    ) : DayOverviewEffect
}

/**
 * ViewModel de la vue d'ensemble d'un jour (PS-139) : tous ses souvenirs, triés ou non, rangés par
 * année (`GetDayOverview`), relus à chaque retour sur l'écran.
 */
@HiltViewModel
class DayOverviewViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getDayOverview: GetDayOverview,
        private val clock: Clock,
    ) : ViewModel() {
        /** Jour affiché. */
        val day: LocalDate = savedStateHandle.toRoute<DayOverviewRoute>().day

        private val state = MutableStateFlow<DayOverviewUiState>(DayOverviewUiState.Loading)
        private val effects = Channel<DayOverviewEffect>(Channel.BUFFERED)

        /** État de l'écran. */
        val uiState: StateFlow<DayOverviewUiState> = state.asStateFlow()

        /** Navigations à exécuter, chacune livrée une seule fois. */
        val uiEffects: Flow<DayOverviewEffect> = effects.receiveAsFlow()

        init {
            load(showLoading = true)
        }

        /** Traite une action de l'utilisateur. */
        fun onEvent(event: DayOverviewEvent) {
            when (event) {
                DayOverviewEvent.Retry -> load(showLoading = true)
                DayOverviewEvent.Resumed -> load(showLoading = false)
                DayOverviewEvent.Back -> effects.trySend(DayOverviewEffect.Close)
                is DayOverviewEvent.Open -> open(event.id)
                DayOverviewEvent.ClosePhoto -> setOpenPhoto(null)
                DayOverviewEvent.StartSorting -> effects.trySend(DayOverviewEffect.OpenDeck(day))
            }
        }

        /** À trier : la pile s'ouvre sur ce média ; photo déjà triée : elle s'ouvre en grand. */
        private fun open(id: MediaId) {
            val loaded = state.value as? DayOverviewUiState.Loaded ?: return
            val memory =
                loaded.overview.years
                    .flatMap { it.memories }
                    .firstOrNull { it.media.id == id } ?: return
            when {
                memory.decision == null -> {
                    effects.trySend(DayOverviewEffect.OpenDeck(day, id))
                }

                memory.media.type == MediaType.IMAGE -> {
                    setOpenPhoto(MediaCardUiState.of(memory.media, day, clock.zone, favorite = memory.favorite))
                }
            }
        }

        private fun setOpenPhoto(photo: MediaCardUiState?) {
            state.update { if (it is DayOverviewUiState.Loaded) it.copy(openPhoto = photo) else it }
        }

        /** Lit les souvenirs ; au retour sur l'écran ([showLoading] faux), l'ancien état reste affiché. */
        private fun load(showLoading: Boolean) {
            if (showLoading) state.value = DayOverviewUiState.Loading
            viewModelScope.launch {
                state.value =
                    runCatching { getDayOverview(day) }.fold(
                        onSuccess = { DayOverviewUiState.Loaded(it) },
                        onFailure = { DayOverviewUiState.Error },
                    )
            }
        }
    }
