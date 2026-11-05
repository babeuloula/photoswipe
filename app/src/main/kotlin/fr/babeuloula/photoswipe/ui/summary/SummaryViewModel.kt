package fr.babeuloula.photoswipe.ui.summary

import android.content.IntentSender
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.usecase.GetSessionSummary
import fr.babeuloula.photoswipe.domain.usecase.PrepareTrash
import fr.babeuloula.photoswipe.domain.usecase.RecordTrashResult
import fr.babeuloula.photoswipe.ui.common.MediaActionIntents
import fr.babeuloula.photoswipe.ui.navigation.SummaryRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** État du résumé (SPEC §3.5). */
sealed interface SummaryUiState {
    /** Résumé en cours de calcul. */
    data object Loading : SummaryUiState

    /** La base n'a pas pu être lue. */
    data object Error : SummaryUiState

    /**
     * Résumé calculé.
     *
     * @property summary gardés de la session ; marqués, favoris et espace libéré de toutes les
     * demandes en attente (ADR 0011).
     * @property confirming vrai pendant les dialogues système de corbeille : « Confirmer » est
     * désactivé, pour ne pas les lancer deux fois.
     */
    data class Loaded(
        val summary: SessionSummary,
        val confirming: Boolean = false,
    ) : SummaryUiState {
        /** Vrai s'il y a des médias à envoyer à la corbeille. */
        val canConfirm: Boolean get() = summary.markedForDeletionCount > 0 && !confirming
    }
}

/** Action de l'utilisateur sur le résumé. */
sealed interface SummaryEvent {
    /** « Réessayer » après une erreur. */
    data object Retry : SummaryEvent

    /** « Confirmer la suppression » : présenter les dialogues système de corbeille, un par lot. */
    data object Confirm : SummaryEvent

    /**
     * Réponse au dialogue système en cours.
     *
     * @property confirmed vrai si l'utilisateur l'a accepté.
     */
    data class SystemRequestAnswered(
        val confirmed: Boolean,
    ) : SummaryEvent

    /** « Plus tard » : revenir à l'accueil, les marquages restent en attente. */
    data object Later : SummaryEvent

    /** Flèche de retour. */
    data object Back : SummaryEvent
}

/** Navigation demandée par le résumé. */
sealed interface SummaryEffect {
    /**
     * Revenir à l'accueil.
     *
     * @property trashedCount médias envoyés à la corbeille juste avant, annoncés par l'accueil ;
     * 0 pour « Plus tard ».
     */
    data class GoHome(
        val trashedCount: Int = 0,
    ) : SummaryEffect

    /** Revenir à l'écran précédent. */
    data object Close : SummaryEffect

    /** Lancer un dialogue système (corbeille) ; la réponse revient par [SummaryEvent.SystemRequestAnswered]. */
    data class LaunchSystemRequest(
        val intentSender: IntentSender,
    ) : SummaryEffect

    /**
     * Les médias marqués n'existent plus dans la galerie (supprimés ailleurs, données de démo) :
     * aucun dialogue ne peut les envoyer à la corbeille.
     */
    data object MarkedMediaMissing : SummaryEffect
}

/**
 * ViewModel du résumé : les gardés viennent de la route (la session de la pile, ADR 0017), le
 * reste des demandes en attente en base, par `GetSessionSummary` (ADR 0011).
 *
 * « Confirmer » prépare un dialogue système de corbeille par lot (`PrepareTrash`) et les présente
 * l'un après l'autre ; chaque réponse est enregistrée par `RecordTrashResult` : les médias
 * acceptés passent à `TRASHED`, les autres restent marqués (SPEC §3.5).
 */
@HiltViewModel
class SummaryViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getSessionSummary: GetSessionSummary,
        private val prepareTrash: PrepareTrash,
        private val recordTrashResult: RecordTrashResult,
        private val mediaActionIntents: MediaActionIntents,
    ) : ViewModel() {
        /** Médias gardés pendant la session qui mène au résumé. */
        private val keptCount: Int = savedStateHandle.toRoute<SummaryRoute>().keptCount

        private val state = MutableStateFlow<SummaryUiState>(SummaryUiState.Loading)
        private val effects = Channel<SummaryEffect>(Channel.BUFFERED)

        /** Dialogues de corbeille encore à présenter, un par lot. */
        private val pendingRequests = ArrayDeque<MediaActionRequest>()

        /** Dialogue affiché, en attente de réponse. */
        private var currentRequest: MediaActionRequest? = null

        /** Médias envoyés à la corbeille pendant cette confirmation. */
        private var trashedCount = 0

        /** État du résumé. */
        val uiState: StateFlow<SummaryUiState> = state.asStateFlow()

        /** Navigations à exécuter, chacune livrée une seule fois. */
        val uiEffects: Flow<SummaryEffect> = effects.receiveAsFlow()

        init {
            load()
        }

        /** Traite une action de l'utilisateur. */
        fun onEvent(event: SummaryEvent) {
            when (event) {
                SummaryEvent.Retry -> load()
                SummaryEvent.Confirm -> confirm()
                is SummaryEvent.SystemRequestAnswered -> recordAnswer(event.confirmed)
                SummaryEvent.Later -> effects.trySend(SummaryEffect.GoHome())
                SummaryEvent.Back -> effects.trySend(SummaryEffect.Close)
            }
        }

        /** Prépare les dialogues de corbeille de tous les marquages en attente, puis lance le premier. */
        private fun confirm() {
            val loaded = state.value as? SummaryUiState.Loaded ?: return
            if (!loaded.canConfirm) return
            state.value = loaded.copy(confirming = true)
            trashedCount = 0
            viewModelScope.launch {
                runCatching { prepareTrash() }.fold(
                    onSuccess = { requests ->
                        if (requests.isEmpty()) {
                            effects.trySend(SummaryEffect.MarkedMediaMissing)
                            load(showLoading = false)
                        } else {
                            pendingRequests.clear()
                            pendingRequests.addAll(requests)
                            launchNext()
                        }
                    },
                    onFailure = { load(showLoading = false) },
                )
            }
        }

        /** Enregistre la réponse au dialogue en cours (`RecordTrashResult`), puis passe au lot suivant. */
        private fun recordAnswer(confirmed: Boolean) {
            val request = currentRequest ?: return
            currentRequest = null
            viewModelScope.launch {
                val recorded = runCatching { recordTrashResult(request, confirmed) }.isSuccess
                if (confirmed && recorded) trashedCount += request.mediaIds.size
                launchNext()
            }
        }

        /**
         * Lance le dialogue du lot suivant. Sans lot restant : si des médias sont partis à la
         * corbeille, retour à l'accueil, qui l'annonce ; sinon (tout refusé), le résumé se met à jour.
         */
        private fun launchNext() {
            val request = pendingRequests.removeFirstOrNull()
            if (request == null) {
                if (trashedCount > 0) {
                    effects.trySend(SummaryEffect.GoHome(trashedCount))
                } else {
                    load(showLoading = false)
                }
                return
            }
            currentRequest = request
            effects.trySend(SummaryEffect.LaunchSystemRequest(mediaActionIntents.intentSender(request)))
        }

        /** Calcule le résumé ; après une confirmation, l'écran garde l'ancien jusqu'au nouveau. */
        private fun load(showLoading: Boolean = true) {
            if (showLoading) state.value = SummaryUiState.Loading
            viewModelScope.launch {
                state.value =
                    runCatching { getSessionSummary(ReviewSession()) }.fold(
                        onSuccess = { SummaryUiState.Loaded(it.copy(keptCount = keptCount)) },
                        onFailure = { SummaryUiState.Error },
                    )
            }
        }
    }
