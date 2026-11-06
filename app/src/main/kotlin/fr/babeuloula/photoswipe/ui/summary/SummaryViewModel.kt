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
     */
    data class Loaded(
        val summary: SessionSummary,
        val confirming: Boolean = false,
    ) : SummaryUiState {
        /** Vrai s'il y a des médias à envoyer à la corbeille ou à mettre en favori dans la galerie. */
        val canConfirm: Boolean
            get() = (summary.markedForDeletionCount > 0 || summary.favoriteCount > 0) && !confirming

        /** Vrai si la confirmation commence par la corbeille ; sinon, elle n'applique que des favoris. */
        val confirmsDeletion: Boolean get() = summary.markedForDeletionCount > 0
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
     * @property favoritedCount médias ajoutés aux favoris de la galerie juste avant, annoncés aussi.
     */
    data class GoHome(
        val trashedCount: Int = 0,
        val favoritedCount: Int = 0,
    ) : SummaryEffect

    /** Revenir à l'écran précédent. */
    data object Close : SummaryEffect

    /** Lancer un dialogue système ; la réponse revient par [SummaryEvent.SystemRequestAnswered]. */
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
     * préparer. Les marquages et favoris restent en base ; l'écran le dit et propose de réessayer.
     *
     * @property stillMarkedCount médias toujours marqués pour suppression.
     * @property pendingFavoriteCount favoris toujours en attente.
     */
    data class NothingApplied(
        val stillMarkedCount: Int,
        val pendingFavoriteCount: Int,
    ) : SummaryEffect
}

/**
 * ViewModel du résumé : les gardés viennent de la route (la session de la pile, ADR 0017), le
 * reste des demandes en attente en base, par `GetSessionSummary` (ADR 0011).
 *
 * La confirmation présente d'abord les dialogues de corbeille (`PrepareTrash`), puis ceux des
 * favoris (`PrepareFavorites`), préparés seulement après la corbeille pour ne pas demander en
 * favori un média qui vient d'y partir. Chaque réponse est enregistrée lot par lot
 * (`RecordTrashResult`, `RecordFavoriteResult`) : ce qui est refusé reste en attente (SPEC §3.5).
 * Si quelque chose a changé dans la galerie, retour à l'accueil, qui l'annonce ; sinon le résumé
 * reste affiché, dit que rien n'est perdu et propose de réessayer (`NothingApplied`).
 */
@HiltViewModel
class SummaryViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val getSessionSummary: GetSessionSummary,
        private val confirmation: ConfirmationActions,
        private val mediaActionIntents: MediaActionIntents,
    ) : ViewModel() {
        /** Médias gardés pendant la session qui mène au résumé. */
        private val keptCount: Int = savedStateHandle.toRoute<SummaryRoute>().keptCount

        private val state = MutableStateFlow<SummaryUiState>(SummaryUiState.Loading)
        private val effects = Channel<SummaryEffect>(Channel.BUFFERED)

        /** Dialogues de l'étape en cours encore à présenter, un par lot. */
        private val pendingRequests = ArrayDeque<MediaActionRequest>()

        /** Étape de la confirmation : corbeille, puis favoris. */
        private var step = ConfirmationStep.TRASH

        /** Dialogue affiché, en attente de réponse. */
        private var currentRequest: MediaActionRequest? = null

        /** Dialogues présentés pendant cette confirmation. */
        private var presentedCount = 0

        /** Médias envoyés à la corbeille pendant cette confirmation. */
        private var trashedCount = 0

        /** Médias ajoutés aux favoris de la galerie pendant cette confirmation. */
        private var favoritedCount = 0

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

        /** Commence la confirmation par les dialogues de corbeille. */
        private fun confirm() {
            val loaded = state.value as? SummaryUiState.Loaded ?: return
            if (!loaded.canConfirm) return
            state.value = loaded.copy(confirming = true)
            step = ConfirmationStep.TRASH
            presentedCount = 0
            trashedCount = 0
            favoritedCount = 0
            pendingRequests.clear()
            viewModelScope.launch { prepareStep() }
        }

        /** Prépare les dialogues de l'étape en cours, puis lance le premier. */
        private suspend fun prepareStep() {
            runCatching {
                when (step) {
                    ConfirmationStep.TRASH -> confirmation.prepareTrash()
                    ConfirmationStep.FAVORITES -> confirmation.prepareFavorites()
                }
            }.fold(
                onSuccess = { requests ->
                    pendingRequests.addAll(requests)
                    launchNext()
                },
                onFailure = { finish(failed = true) },
            )
        }

        /** Enregistre la réponse au dialogue en cours, puis passe au lot suivant. */
        private fun recordAnswer(confirmed: Boolean) {
            val request = currentRequest ?: return
            currentRequest = null
            viewModelScope.launch {
                val recorded =
                    runCatching {
                        when (step) {
                            ConfirmationStep.TRASH -> confirmation.recordTrashResult(request, confirmed)
                            ConfirmationStep.FAVORITES -> confirmation.recordFavoriteResult(request, confirmed)
                        }
                    }.isSuccess
                if (confirmed && recorded) {
                    when (step) {
                        ConfirmationStep.TRASH -> trashedCount += request.mediaIds.size
                        ConfirmationStep.FAVORITES -> favoritedCount += request.mediaIds.size
                    }
                }
                launchNext()
            }
        }

        /** Lance le dialogue du lot suivant ; l'étape finie, passe aux favoris, puis termine. */
        private suspend fun launchNext() {
            val request = pendingRequests.removeFirstOrNull()
            if (request != null) {
                currentRequest = request
                presentedCount++
                effects.trySend(SummaryEffect.LaunchSystemRequest(mediaActionIntents.intentSender(request)))
            } else if (step == ConfirmationStep.TRASH) {
                step = ConfirmationStep.FAVORITES
                prepareStep()
            } else {
                finish()
            }
        }

        /**
         * Fin de la confirmation : si la galerie a changé, retour à l'accueil, qui l'annonce ;
         * sinon le résumé se met à jour et dit pourquoi rien n'a changé : aucun dialogue possible
         * (médias introuvables), ou dialogues refusés, ou demandes impossibles à préparer ([failed]).
         */
        private suspend fun finish(failed: Boolean = false) {
            if (trashedCount > 0 || favoritedCount > 0) {
                // Les médias partis à la corbeille ne sont plus dans la galerie : leurs lignes sont purgées.
                if (trashedCount > 0) confirmation.orphanCleanup.run()
                effects.trySend(SummaryEffect.GoHome(trashedCount, favoritedCount))
                return
            }
            val reloaded = refresh() as? SummaryUiState.Loaded ?: return
            val effect =
                if (!failed && presentedCount == 0) {
                    SummaryEffect.MarkedMediaMissing
                } else {
                    SummaryEffect.NothingApplied(
                        stillMarkedCount = reloaded.summary.markedForDeletionCount,
                        pendingFavoriteCount = reloaded.summary.favoriteCount,
                    )
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

        /** Étapes de la confirmation, dans l'ordre. */
        private enum class ConfirmationStep { TRASH, FAVORITES }
    }
