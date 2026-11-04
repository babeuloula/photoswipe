package fr.babeuloula.photoswipe.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.domain.usecase.GetReviewDays
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** État de l'accueil (SPEC §4.2). */
sealed interface HomeUiState {
    /** Premier calcul en cours. */
    data object Loading : HomeUiState

    /** La galerie ou la base n'a pas pu être lue. */
    data object Error : HomeUiState

    /**
     * Jours de la fenêtre calculés.
     *
     * @property today aujourd'hui et son nombre de médias à trier.
     */
    data class Loaded(
        val today: ReviewDay,
    ) : HomeUiState
}

/** Action de l'utilisateur ou du système sur l'accueil. */
sealed interface HomeEvent {
    /** Tap sur la carte « Aujourd'hui ». */
    data object OpenToday : HomeEvent

    /** « Réessayer » après une erreur. */
    data object Retry : HomeEvent

    /** Retour au premier plan : la galerie ou le jour ont pu changer (ADR 0008). */
    data object Resumed : HomeEvent

    /** Bouton des réglages. */
    data object OpenSettings : HomeEvent
}

/** Navigation demandée par l'accueil. */
sealed interface HomeEffect {
    /** Ouvrir la pile de [day]. */
    data class OpenDeck(
        val day: ReviewDay,
    ) : HomeEffect

    /** Ouvrir les réglages. */
    data object OpenSettings : HomeEffect
}

/**
 * ViewModel de l'accueil : la carte « Aujourd'hui » et son nombre de médias à trier (SPEC §4.2).
 *
 * Les nombres suivent les décisions en direct (`GetReviewDays` est un flux). Ils sont recalculés
 * depuis le début à chaque retour au premier plan et après « Réessayer » : la galerie, le jour ou
 * les permissions ont pu changer entre-temps.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val getReviewDays: GetReviewDays,
    ) : ViewModel() {
        /** Demandes de calcul ; la valeur dit s'il faut repasser par l'état de chargement. */
        private val reloads = MutableSharedFlow<Boolean>(replay = 1).apply { tryEmit(false) }
        private val effects = Channel<HomeEffect>(Channel.BUFFERED)

        /** État de l'accueil. */
        val uiState: StateFlow<HomeUiState> =
            reloads
                .flatMapLatest { showLoading -> loadDays(showLoading) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState.Loading)

        /** Navigations à exécuter, chacune livrée une seule fois. */
        val uiEffects: Flow<HomeEffect> = effects.receiveAsFlow()

        /** Traite une action de l'utilisateur ou du système. */
        fun onEvent(event: HomeEvent) {
            when (event) {
                HomeEvent.OpenToday -> {
                    (uiState.value as? HomeUiState.Loaded)?.let {
                        effects.trySend(
                            HomeEffect.OpenDeck(it.today),
                        )
                    }
                }

                HomeEvent.Retry -> {
                    reloads.tryEmit(true)
                }

                HomeEvent.Resumed -> {
                    reloads.tryEmit(false)
                }

                HomeEvent.OpenSettings -> {
                    effects.trySend(HomeEffect.OpenSettings)
                }
            }
        }

        /**
         * Calcule les jours. Après « Réessayer » ([showLoading]), l'écran repasse par le chargement ;
         * au retour au premier plan, il garde l'état affiché jusqu'au nouveau résultat, sans clignoter.
         */
        private fun loadDays(showLoading: Boolean): Flow<HomeUiState> =
            getReviewDays()
                .map<List<ReviewDay>, HomeUiState> { days -> HomeUiState.Loaded(today = days.first()) }
                .onStart { if (showLoading) emit(HomeUiState.Loading) }
                .catch { emit(HomeUiState.Error) }

        private companion object {
            /** Garde le calcul actif pendant une rotation ou un court passage en arrière-plan. */
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
