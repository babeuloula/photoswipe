package fr.babeuloula.photoswipe.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.usecase.GetReviewDays
import fr.babeuloula.photoswipe.domain.usecase.GetSessionSummary
import fr.babeuloula.photoswipe.permission.MediaManagementReader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
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
     * @property previousDays les 6 jours précédents, du plus récent au plus ancien, à rattraper.
     * @property pendingDeletionCount médias marqués pour suppression et pas encore confirmés, toutes
     * sessions confondues (SPEC §3.4) ; 0 masque la bannière.
     * @property showManageMediaTip vrai pour proposer l'accès « Gestion des médias » : un dialogue
     * d'Android a déjà été validé, le conseil n'a pas été fermé et l'accès n'est pas donné (ADR 0023).
     */
    data class Loaded(
        val today: ReviewDay,
        val previousDays: List<ReviewDay>,
        val pendingDeletionCount: Int = 0,
        val showManageMediaTip: Boolean = false,
    ) : HomeUiState {
        /** Vrai si au moins un souvenir existe sur la fenêtre, trié ou non (SPEC §4, états vides). */
        val hasMemories: Boolean get() = today.memoryCount > 0 || previousDays.any { it.memoryCount > 0 }
    }
}

/** Action de l'utilisateur ou du système sur l'accueil. */
sealed interface HomeEvent {
    /**
     * Tap sur la carte « Aujourd'hui » ou sur un jour à rattraper.
     *
     * @property date jour dont la pile s'ouvre.
     */
    data class OpenDay(
        val date: LocalDate,
    ) : HomeEvent

    /** « Réessayer » après une erreur. */
    data object Retry : HomeEvent

    /** Retour au premier plan : la galerie ou le jour ont pu changer (ADR 0008). */
    data object Resumed : HomeEvent

    /** Bouton des réglages. */
    data object OpenSettings : HomeEvent

    /** Tap sur la bannière des suppressions en attente. */
    data object OpenPendingDeletions : HomeEvent

    /** « paramètres » du conseil : ouvrir le réglage « Gestion des médias » d'Android. */
    data object OpenManageMediaSettings : HomeEvent

    /** Croix du conseil « Gestion des médias » : ne plus le proposer. */
    data object DismissManageMediaTip : HomeEvent
}

/** Navigation demandée par l'accueil. */
sealed interface HomeEffect {
    /** Ouvrir la pile du jour [date]. */
    data class OpenDeck(
        val date: LocalDate,
    ) : HomeEffect

    /** Ouvrir les réglages. */
    data object OpenSettings : HomeEffect

    /** Ouvrir le résumé, pour confirmer les suppressions en attente (SPEC §3.5). */
    data object OpenSummary : HomeEffect

    /** Ouvrir le réglage « Gestion des médias » d'Android pour PhotoSwipe. */
    data object OpenManageMediaSettings : HomeEffect
}

/**
 * ViewModel de l'accueil : la carte « Aujourd'hui » et les 6 jours précédents, chacun avec son
 * nombre de médias à trier (SPEC §4.2).
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
        private val getSessionSummary: GetSessionSummary,
        private val settingsRepository: SettingsRepository,
        private val mediaManagement: MediaManagementReader,
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
                is HomeEvent.OpenDay -> {
                    effects.trySend(HomeEffect.OpenDeck(event.date))
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

                HomeEvent.OpenPendingDeletions -> {
                    effects.trySend(HomeEffect.OpenSummary)
                }

                HomeEvent.OpenManageMediaSettings -> {
                    effects.trySend(HomeEffect.OpenManageMediaSettings)
                }

                HomeEvent.DismissManageMediaTip -> {
                    viewModelScope.launch {
                        runCatching { settingsRepository.saveManageMediaTip(ManageMediaTip.DISMISSED) }
                    }
                }
            }
        }

        /**
         * Calcule les jours. Après « Réessayer » ([showLoading]), l'écran repasse par le chargement ;
         * au retour au premier plan, il garde l'état affiché jusqu'au nouveau résultat, sans clignoter.
         * Le nombre de suppressions en attente est lu à chaque calcul : les marquages se font dans la
         * pile, et l'accueil se recalcule en y revenant.
         */
        private fun loadDays(showLoading: Boolean): Flow<HomeUiState> {
            val loaded: Flow<HomeUiState> =
                combine(
                    getReviewDays(),
                    flow { emit(pendingDeletionCount()) },
                    manageMediaTipShown(),
                ) { days, pending, tip ->
                    HomeUiState.Loaded(
                        today = days.first(),
                        previousDays = days.drop(1),
                        pendingDeletionCount = pending,
                        showManageMediaTip = tip,
                    )
                }
            return loaded
                .onStart { if (showLoading) emit(HomeUiState.Loading) }
                .catch { emit(HomeUiState.Error) }
        }

        /**
         * Conseil « Gestion des médias » à afficher : proposé, pas fermé, accès pas encore donné.
         * L'accès se relit à chaque calcul, donc au retour des réglages d'Android. Un réglage
         * illisible masque le conseil.
         */
        private fun manageMediaTipShown(): Flow<Boolean> =
            settingsRepository
                .observeManageMediaTip()
                .map { it == ManageMediaTip.OFFERED && !mediaManagement.isGranted() }
                .catch { emit(false) }

        /** Marquages à confirmer : le même nombre que le résumé annoncera (ADR 0011). */
        private suspend fun pendingDeletionCount(): Int = getSessionSummary(ReviewSession()).markedForDeletionCount

        private companion object {
            /** Garde le calcul actif pendant une rotation ou un court passage en arrière-plan. */
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
