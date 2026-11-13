package fr.babeuloula.photoswipe.ui.dayoverview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.DayOverview
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.usecase.GetDayOverview
import fr.babeuloula.photoswipe.domain.usecase.SortInBatch
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
     * @property selected souvenirs sélectionnés pour un tri par lot (PS-159) ; vide hors sélection.
     */
    data class Loaded(
        val overview: DayOverview,
        val openPhoto: MediaCardUiState? = null,
        val selected: Set<MediaId> = emptySet(),
    ) : DayOverviewUiState {
        /** Vrai pendant une sélection : un appui coche ou décoche au lieu d'ouvrir. */
        val selecting: Boolean get() = selected.isNotEmpty()

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

    /** Tri par lot (PS-159). */
    sealed interface SelectionEvent : DayOverviewEvent

    /**
     * Appui long sur un souvenir, ou action TalkBack « Sélectionner » : coche ou décoche [id], ce
     * qui ouvre la sélection.
     *
     * @property id souvenir concerné.
     */
    data class ToggleSelection(
        val id: MediaId,
    ) : SelectionEvent

    /**
     * En-tête d'une année pendant la sélection : coche toute l'année, ou la décoche si elle l'est déjà.
     *
     * @property year année concernée.
     */
    data class ToggleYear(
        val year: Int,
    ) : SelectionEvent

    /** Croix ou retour : sortie de la sélection, sans rien décider. */
    data object ClearSelection : SelectionEvent

    /** « Garder » : garde tous les souvenirs sélectionnés. */
    data object KeepSelected : SelectionEvent

    /** « Supprimer » : marque tous les souvenirs sélectionnés pour suppression. */
    data object MarkSelected : SelectionEvent

    /** « Annuler » du message qui suit un tri par lot : défait tout le lot. */
    data object UndoBatch : SelectionEvent
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

    /**
     * Un lot vient d'être trié (PS-159) : l'annoncer, avec « Annuler ».
     *
     * @property count souvenirs du lot.
     * @property kept vrai s'ils ont été gardés, faux s'ils ont été marqués pour suppression.
     */
    data class BatchSorted(
        val count: Int,
        val kept: Boolean,
    ) : DayOverviewEffect
}

/**
 * ViewModel de la vue d'ensemble d'un jour (PS-139) : tous ses souvenirs, triés ou non, rangés par
 * année (`GetDayOverview`), relus à chaque retour sur l'écran. Un appui long ouvre une sélection
 * pour garder ou marquer plusieurs souvenirs d'un coup, lot que « Annuler » défait (PS-159).
 */
@HiltViewModel
class DayOverviewViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getDayOverview: GetDayOverview,
        private val sortInBatch: SortInBatch,
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
                DayOverviewEvent.Retry -> {
                    load(showLoading = true)
                }

                DayOverviewEvent.Resumed -> {
                    load(showLoading = false)
                }

                // Pendant une sélection, retour la quitte ; sinon il ferme l'écran.
                DayOverviewEvent.Back -> {
                    if ((state.value as? DayOverviewUiState.Loaded)?.selecting == true) {
                        onSelectionEvent(DayOverviewEvent.ClearSelection)
                    } else {
                        effects.trySend(DayOverviewEffect.Close)
                    }
                }

                is DayOverviewEvent.Open -> {
                    val loaded = state.value as? DayOverviewUiState.Loaded
                    // Pendant une sélection, un appui coche ou décoche.
                    if (loaded?.selecting ==
                        true
                    ) {
                        onSelectionEvent(DayOverviewEvent.ToggleSelection(event.id))
                    } else {
                        open(event.id)
                    }
                }

                DayOverviewEvent.ClosePhoto -> {
                    setOpenPhoto(null)
                }

                DayOverviewEvent.StartSorting -> {
                    effects.trySend(DayOverviewEffect.OpenDeck(day))
                }

                is DayOverviewEvent.SelectionEvent -> {
                    onSelectionEvent(event)
                }
            }
        }

        /** Dernier lot trié, que « Annuler » défait. */
        private var lastBatch: ReviewSession? = null

        /** Sélection et tri par lot (PS-159). */
        private fun onSelectionEvent(event: DayOverviewEvent.SelectionEvent) {
            val loaded = state.value as? DayOverviewUiState.Loaded ?: return
            when (event) {
                is DayOverviewEvent.ToggleSelection -> {
                    val selected = event.id in loaded.selected
                    setSelected(if (selected) loaded.selected - event.id else loaded.selected + event.id)
                }

                is DayOverviewEvent.ToggleYear -> {
                    val year =
                        loaded.overview.years
                            .filter { it.year == event.year }
                            .flatMap { it.memories }
                            .mapTo(HashSet()) { it.media.id }
                    val allSelected = loaded.selected.containsAll(year)
                    setSelected(if (allSelected) loaded.selected - year else loaded.selected + year)
                }

                DayOverviewEvent.ClearSelection -> {
                    setSelected(emptySet())
                }

                DayOverviewEvent.KeepSelected -> {
                    sortSelected(loaded, kept = true)
                }

                DayOverviewEvent.MarkSelected -> {
                    sortSelected(loaded, kept = false)
                }

                DayOverviewEvent.UndoBatch -> {
                    undoBatch()
                }
            }
        }

        /** Défait le dernier lot trié, une seule fois. */
        private fun undoBatch() {
            val batch = lastBatch ?: return
            lastBatch = null
            viewModelScope.launch {
                runCatching { sortInBatch.undo(batch) }
                load(showLoading = false)
            }
        }

        /** Garde ou marque les souvenirs sélectionnés, quitte la sélection et annonce le lot. */
        private fun sortSelected(
            loaded: DayOverviewUiState.Loaded,
            kept: Boolean,
        ) {
            val media =
                loaded.overview.years
                    .flatMap { it.memories }
                    .map { it.media }
                    .filter { it.id in loaded.selected }
            if (media.isEmpty()) return
            setSelected(emptySet())
            viewModelScope.launch {
                runCatching { if (kept) sortInBatch.keep(media, day) else sortInBatch.mark(media, day) }
                    .onSuccess {
                        lastBatch = it
                        effects.trySend(DayOverviewEffect.BatchSorted(media.size, kept))
                    }
                load(showLoading = false)
            }
        }

        private fun setSelected(selected: Set<MediaId>) {
            state.update { if (it is DayOverviewUiState.Loaded) it.copy(selected = selected) else it }
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
                val selected = (state.value as? DayOverviewUiState.Loaded)?.selected.orEmpty()
                state.value =
                    runCatching { getDayOverview(day) }.fold(
                        // Une sélection en cours survit à la relecture, sans les souvenirs disparus.
                        onSuccess = { overview ->
                            val ids = overview.years.flatMap { it.memories }.mapTo(HashSet()) { it.media.id }
                            DayOverviewUiState.Loaded(overview, selected = selected intersect ids)
                        },
                        onFailure = { DayOverviewUiState.Error },
                    )
            }
        }
    }
