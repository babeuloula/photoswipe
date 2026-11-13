package fr.babeuloula.photoswipe.ui.trash

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.Trash
import fr.babeuloula.photoswipe.domain.model.TrashItem
import fr.babeuloula.photoswipe.domain.port.MediaActionRequest
import fr.babeuloula.photoswipe.domain.usecase.GetTrash
import fr.babeuloula.photoswipe.domain.usecase.RecordRestoreResult
import fr.babeuloula.photoswipe.domain.usecase.RequestPermanentDeletion
import fr.babeuloula.photoswipe.domain.usecase.RequestRestore
import fr.babeuloula.photoswipe.permission.MediaManagementReader
import fr.babeuloula.photoswipe.ui.common.MediaActionIntents
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Use cases de la page « Corbeille » (PS-168), regroupés pour son ViewModel.
 *
 * @property getTrash médias supprimés par PhotoSwipe encore dans la corbeille.
 * @property requestRestore prépare la sortie de la corbeille.
 * @property recordRestoreResult enregistre une restauration acceptée.
 * @property requestPermanentDeletion prépare la suppression définitive.
 */
class TrashActions
    @Inject
    constructor(
        val getTrash: GetTrash,
        val requestRestore: RequestRestore,
        val recordRestoreResult: RecordRestoreResult,
        val requestPermanentDeletion: RequestPermanentDeletion,
    )

/** État de la page « Corbeille ». */
sealed interface TrashUiState {
    /** Corbeille en cours de lecture. */
    data object Loading : TrashUiState

    /** La corbeille n'a pas pu être lue. */
    data object Error : TrashUiState

    /**
     * Corbeille lue.
     *
     * @property trash médias supprimés par PhotoSwipe encore restaurables.
     * @property busy vrai pendant une demande système : les boutons sont désactivés.
     * @property confirmingDeletion média dont la suppression définitive attend la confirmation de
     * PhotoSwipe, quand l'accès « Gestion des médias » fait disparaître celle d'Android (ADR 0023).
     */
    data class Loaded(
        val trash: Trash,
        val busy: Boolean = false,
        val confirmingDeletion: TrashItem? = null,
    ) : TrashUiState
}

/** Action de l'utilisateur sur la page « Corbeille ». */
sealed interface TrashEvent {
    /** « Réessayer » après une erreur. */
    data object Retry : TrashEvent

    /** Flèche de retour. */
    data object Back : TrashEvent

    /** Retour sur l'écran : la corbeille a pu changer ailleurs. */
    data object Resumed : TrashEvent

    /**
     * « Restaurer » un média.
     *
     * @property mediaId média à sortir de la corbeille.
     */
    data class Restore(
        val mediaId: MediaId,
    ) : TrashEvent

    /**
     * « Supprimer définitivement » un média.
     *
     * @property mediaId média à supprimer pour de bon.
     */
    data class DeleteForever(
        val mediaId: MediaId,
    ) : TrashEvent

    /** Suppression définitive acceptée dans la confirmation de PhotoSwipe. */
    data object DeletionConfirmed : TrashEvent

    /** Confirmation de PhotoSwipe fermée sans rien supprimer. */
    data object DeletionDismissed : TrashEvent

    /**
     * Réponse au dialogue système en cours.
     *
     * @property confirmed vrai si l'utilisateur l'a accepté.
     */
    data class SystemRequestAnswered(
        val confirmed: Boolean,
    ) : TrashEvent
}

/** Effet ponctuel de la page « Corbeille ». */
sealed interface TrashEffect {
    /** Revenir à l'écran précédent. */
    data object Close : TrashEffect

    /**
     * Lancer un dialogue système ; la réponse revient par [TrashEvent.SystemRequestAnswered].
     *
     * @property intentSender dialogue de restauration ou de suppression définitive.
     */
    data class LaunchSystemRequest(
        val intentSender: IntentSender,
    ) : TrashEffect

    /** Le média est revenu dans la galerie, de nouveau à trier. */
    data object Restored : TrashEffect

    /** Le média est supprimé pour de bon : sa place est rendue. */
    data object DeletedForever : TrashEffect

    /** Rien n'a changé : dialogue refusé, ou demande impossible à préparer. */
    data object NothingChanged : TrashEffect
}

/**
 * ViewModel de la page « Corbeille » (PS-168) : lit les médias supprimés par PhotoSwipe encore
 * dans la corbeille d'Android (`GetTrash`), et les restaure ou les supprime définitivement, un à la
 * fois, par un dialogue d'Android (silencieux avec l'accès « Gestion des médias »). Une restauration
 * acceptée est enregistrée par `RecordRestoreResult` ; une suppression définitive avec l'accès est
 * d'abord confirmée par PhotoSwipe (ADR 0023). Après chaque réponse, la corbeille est relue : un
 * média déjà vidé ou restauré ailleurs disparaît simplement de la liste.
 */
@HiltViewModel
class TrashViewModel
    @Inject
    constructor(
        private val actions: TrashActions,
        private val mediaActionIntents: MediaActionIntents,
        private val mediaManagement: MediaManagementReader,
    ) : ViewModel() {
        private val state = MutableStateFlow<TrashUiState>(TrashUiState.Loading)
        private val effects = Channel<TrashEffect>(Channel.BUFFERED)

        /** Demande affichée, en attente de réponse, et sa nature. */
        private var current: Pending? = null

        /** État de la page. */
        val uiState: StateFlow<TrashUiState> = state.asStateFlow()

        /** Effets à exécuter, chacun livré une seule fois. */
        val uiEffects: Flow<TrashEffect> = effects.receiveAsFlow()

        init {
            load()
        }

        /** Traite une action de l'utilisateur. */
        fun onEvent(event: TrashEvent) {
            when (event) {
                TrashEvent.Retry -> load()
                TrashEvent.Back -> effects.trySend(TrashEffect.Close)
                TrashEvent.Resumed -> refreshWhenIdle()
                is TrashEvent.Restore -> itemOf(event.mediaId)?.let { start(it, restore = true) }
                is TrashEvent.DeleteForever -> askThenDelete(event.mediaId)
                TrashEvent.DeletionConfirmed -> confirmDeletion()
                TrashEvent.DeletionDismissed -> loaded()?.let { state.value = it.copy(confirmingDeletion = null) }
                is TrashEvent.SystemRequestAnswered -> recordAnswer(event.confirmed)
            }
        }

        private fun loaded(): TrashUiState.Loaded? = state.value as? TrashUiState.Loaded

        private fun itemOf(mediaId: MediaId): TrashItem? =
            loaded()
                ?.takeUnless { it.busy }
                ?.trash
                ?.items
                ?.firstOrNull { it.media.id == mediaId }

        /** Avec l'accès « Gestion des médias », Android ne demande rien : PhotoSwipe confirme lui-même. */
        private fun askThenDelete(mediaId: MediaId) {
            val item = itemOf(mediaId) ?: return
            if (mediaManagement.isGranted()) {
                state.value = loaded()?.copy(confirmingDeletion = item) ?: return
            } else {
                start(item, restore = false)
            }
        }

        private fun confirmDeletion() {
            val item = loaded()?.confirmingDeletion ?: return
            state.value = loaded()?.copy(confirmingDeletion = null) ?: return
            start(item, restore = false)
        }

        /** Prépare la demande de restauration ([restore]) ou de suppression définitive, puis la lance. */
        private fun start(
            item: TrashItem,
            restore: Boolean,
        ) {
            val loaded = loaded() ?: return
            state.value = loaded.copy(busy = true)
            val media = listOf(item.media)
            val request =
                runCatching {
                    if (restore) actions.requestRestore(media) else actions.requestPermanentDeletion(media)
                }.getOrNull()?.firstOrNull()
            if (request == null) {
                state.value = loaded.copy(busy = false)
                effects.trySend(TrashEffect.NothingChanged)
                return
            }
            current = Pending(request, restore)
            effects.trySend(TrashEffect.LaunchSystemRequest(mediaActionIntents.intentSender(request)))
        }

        /** Enregistre la réponse au dialogue en cours, puis relit la corbeille. */
        private fun recordAnswer(confirmed: Boolean) {
            val pending = current ?: return
            current = null
            viewModelScope.launch {
                val recorded =
                    !pending.restore ||
                        runCatching { actions.recordRestoreResult(pending.request, confirmed) }.isSuccess
                refresh()
                val effect =
                    when {
                        !confirmed || !recorded -> TrashEffect.NothingChanged
                        pending.restore -> TrashEffect.Restored
                        else -> TrashEffect.DeletedForever
                    }
                effects.trySend(effect)
            }
        }

        /** Relit la corbeille au retour sur l'écran, sauf pendant une demande, dont le dialogue fait aussi revenir. */
        private fun refreshWhenIdle() {
            val loaded = loaded() ?: return
            if (loaded.busy || loaded.confirmingDeletion != null) return
            viewModelScope.launch { refresh() }
        }

        private fun load() {
            state.value = TrashUiState.Loading
            viewModelScope.launch { refresh() }
        }

        private suspend fun refresh() {
            state.value =
                runCatching { actions.getTrash() }.fold(
                    onSuccess = { TrashUiState.Loaded(it) },
                    onFailure = { TrashUiState.Error },
                )
        }

        /** Demande système en attente de réponse : [restore] vrai pour une restauration. */
        private class Pending(
            val request: MediaActionRequest,
            val restore: Boolean,
        )
    }
