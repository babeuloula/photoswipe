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
import fr.babeuloula.photoswipe.permission.MediaManagementReader
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
     * @property confirming vrai pendant les dialogues système (corbeille puis favoris) : le bouton
     * de confirmation est désactivé, pour ne pas les lancer deux fois.
     * @property askingTrashConfirmation vrai pendant la confirmation de PhotoSwipe, qui remplace le
     * dialogue de corbeille d'Android quand l'accès « Gestion des médias » est donné.
     */
    data class Loaded(
        val summary: SessionSummary,
        val confirming: Boolean = false,
        val askingTrashConfirmation: Boolean = false,
    ) : SummaryUiState {
        /** Vrai s'il y a des médias à envoyer à la corbeille ; les favoris sont appliqués dans la pile. */
        val canConfirm: Boolean
            get() = summary.markedForDeletionCount > 0 && !confirming
    }
}

/** Action de l'utilisateur sur le résumé. */
sealed interface SummaryEvent {
    /** « Réessayer » après une erreur. */
    data object Retry : SummaryEvent

    /**
     * Bouton de confirmation : présenter les dialogues système, un par lot, ceux de la corbeille
     * puis ceux des favoris (SPEC §3.5).
     */
    data object Confirm : SummaryEvent

    /** « Envoyer à la corbeille » de la confirmation de PhotoSwipe, avec l'accès « Gestion des médias ». */
    data object TrashConfirmationAccepted : SummaryEvent

    /** Confirmation de PhotoSwipe fermée sans rien envoyer à la corbeille. */
    data object TrashConfirmationDismissed : SummaryEvent

    /**
     * Réponse au dialogue système en cours.
     *
     * @property confirmed vrai si l'utilisateur l'a accepté.
     */
    data class SystemRequestAnswered(
        val confirmed: Boolean,
    ) : SummaryEvent

    /** « Plus tard » : revenir à l'accueil, les marquages et favoris restent en attente. */
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

    /**
     * Lancer un dialogue système ; la réponse revient par [SummaryEvent.SystemRequestAnswered].
     *
     * @property intentSender dialogue de corbeille ou de favoris à lancer.
     */
    data class LaunchSystemRequest(
        val intentSender: IntentSender,
    ) : SummaryEffect

    /**
     * Les médias en attente n'existent plus dans la galerie (supprimés ailleurs, données de démo) :
     * aucun dialogue ne peut les envoyer à la corbeille ni les mettre en favori.
     */
    data object MarkedMediaMissing : SummaryEffect

    /**
     * Rien n'a changé dans la galerie : dialogues refusés ou annulés, ou demandes impossibles à
     * préparer. Les marquages restent en base ; l'écran le dit et propose de réessayer.
     *
     * @property stillMarkedCount médias toujours marqués pour suppression.
     */
    data class NothingApplied(
        val stillMarkedCount: Int,
    ) : SummaryEffect
}

/**
 * ViewModel du résumé : les gardés viennent de la route (la session de la pile, ADR 0017), le
 * reste des demandes en attente en base, par `GetSessionSummary` (ADR 0011).
 *
 * La confirmation présente les dialogues de corbeille (`PrepareTrash`), un par lot ; les favoris
 * sont appliqués dès la pile (SPEC §2). Chaque réponse est enregistrée lot par lot
 * (`RecordTrashResult`) : ce qui est refusé reste marqué (SPEC §3.5). Si des médias sont partis à
 * la corbeille, retour à l'accueil, qui l'annonce ; sinon le résumé reste affiché, dit que rien
 * n'est perdu et propose de réessayer (`NothingApplied`). Avec l'accès « Gestion des médias »,
 * Android ne demande plus rien : PhotoSwipe demande alors sa propre confirmation (ADR 0023).
 */
@HiltViewModel
class SummaryViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getSessionSummary: GetSessionSummary,
        private val confirmation: ConfirmationActions,
        private val mediaActionIntents: MediaActionIntents,
        private val mediaManagement: MediaManagementReader,
    ) : ViewModel() {
        /** Médias gardés pendant la session qui mène au résumé. */
        private val keptCount: Int = savedStateHandle.toRoute<SummaryRoute>().keptCount

        private val state = MutableStateFlow<SummaryUiState>(SummaryUiState.Loading)
        private val effects = Channel<SummaryEffect>(Channel.BUFFERED)

        /** Dialogues de corbeille encore à présenter, un par lot. */
        private val pendingRequests = ArrayDeque<MediaActionRequest>()

        /** Dialogue affiché, en attente de réponse. */
        private var currentRequest: MediaActionRequest? = null

        /** Dialogues présentés pendant cette confirmation. */
        private var presentedCount = 0

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
                SummaryEvent.Confirm -> askThenConfirm()
                SummaryEvent.TrashConfirmationAccepted -> confirm()
                SummaryEvent.TrashConfirmationDismissed -> closeTrashConfirmation()
                is SummaryEvent.SystemRequestAnswered -> recordAnswer(event.confirmed)
                SummaryEvent.Later -> effects.trySend(SummaryEffect.GoHome())
                SummaryEvent.Back -> effects.trySend(SummaryEffect.Close)
            }
        }

        /**
         * Avec l'accès « Gestion des médias », Android n'affiche plus son dialogue de corbeille :
         * PhotoSwipe demande alors sa propre confirmation avant d'envoyer quoi que ce soit (ADR 0023).
         * Sans lui, la confirmation commence tout de suite.
         */
        private fun askThenConfirm() {
            val loaded = state.value as? SummaryUiState.Loaded ?: return
            if (!loaded.canConfirm) return
            if (mediaManagement.isGranted()) {
                state.value = loaded.copy(askingTrashConfirmation = true)
            } else {
                confirm()
            }
        }

        private fun closeTrashConfirmation() {
            val loaded = state.value as? SummaryUiState.Loaded ?: return
            state.value = loaded.copy(askingTrashConfirmation = false)
        }

        /** Prépare les dialogues de corbeille, puis lance le premier. */
        private fun confirm() {
            val loaded = state.value as? SummaryUiState.Loaded ?: return
            if (!loaded.canConfirm) return
            state.value = loaded.copy(confirming = true, askingTrashConfirmation = false)
            presentedCount = 0
            trashedCount = 0
            pendingRequests.clear()
            viewModelScope.launch {
                runCatching { confirmation.prepareTrash() }.fold(
                    onSuccess = { requests ->
                        pendingRequests.addAll(requests)
                        launchNext()
                    },
                    onFailure = { finish(failed = true) },
                )
            }
        }

        /** Enregistre la réponse au dialogue en cours, puis passe au lot suivant. */
        private fun recordAnswer(confirmed: Boolean) {
            val request = currentRequest ?: return
            currentRequest = null
            viewModelScope.launch {
                val recorded = runCatching { confirmation.recordTrashResult(request, confirmed) }.isSuccess
                if (confirmed && recorded) trashedCount += request.mediaIds.size
                launchNext()
            }
        }

        /** Lance le dialogue du lot suivant, ou termine. */
        private suspend fun launchNext() {
            val request = pendingRequests.removeFirstOrNull()
            if (request != null) {
                currentRequest = request
                presentedCount++
                effects.trySend(SummaryEffect.LaunchSystemRequest(mediaActionIntents.intentSender(request)))
            } else {
                finish()
            }
        }

        /**
         * Fin de la confirmation : si des médias sont partis à la corbeille, retour à l'accueil, qui
         * l'annonce ; sinon le résumé se met à jour et dit pourquoi rien n'a changé : aucun dialogue
         * possible (médias introuvables), ou dialogues refusés, ou demandes impossibles à préparer
         * ([failed]).
         */
        private suspend fun finish(failed: Boolean = false) {
            if (trashedCount > 0) {
                // Les médias partis à la corbeille ne sont plus dans la galerie : leurs lignes sont purgées.
                confirmation.orphanCleanup.run()
                // Un dialogue d'Android vient d'être validé : l'accueil proposera de s'en passer.
                if (!mediaManagement.isGranted()) runCatching { confirmation.offerManageMediaTip() }
                effects.trySend(SummaryEffect.GoHome(trashedCount))
                return
            }
            val reloaded = refresh() as? SummaryUiState.Loaded ?: return
            val effect =
                if (!failed && presentedCount == 0) {
                    SummaryEffect.MarkedMediaMissing
                } else {
                    SummaryEffect.NothingApplied(stillMarkedCount = reloaded.summary.markedForDeletionCount)
                }
            effects.trySend(effect)
        }

        /** Calcule le résumé, en affichant le chargement. */
        private fun load() {
            state.value = SummaryUiState.Loading
            viewModelScope.launch { refresh() }
        }

        /** Relit le résumé en base et le publie ; pendant une confirmation, l'ancien reste affiché. */
        private suspend fun refresh(): SummaryUiState {
            val loaded =
                runCatching { getSessionSummary(ReviewSession()) }.fold(
                    onSuccess = { SummaryUiState.Loaded(it.copy(keptCount = keptCount)) },
                    onFailure = { SummaryUiState.Error },
                )
            state.value = loaded
            return loaded
        }
    }
