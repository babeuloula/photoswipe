package fr.babeuloula.photoswipe.ui.crash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.crash.CrashReportStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Réponse de la personne au rapport de plantage. */
enum class CrashReportEvent {
    /** « Voir et envoyer » : la page d'envoi s'ouvre dans le navigateur. */
    Send,

    /** « Ignorer », retour ou appui à côté : le rapport est oublié. */
    Dismiss,
}

/** Effet ponctuel du rapport de plantage. */
sealed interface CrashReportEffect {
    /** Ouvrir la page d'envoi de [report] dans le navigateur. */
    data class OpenReport(
        val report: String,
    ) : CrashReportEffect
}

/**
 * Rapport du plantage précédent (ADR 0033) : au lancement, s'il y en a un, la personne choisit de
 * le voir et l'envoyer, ou de l'ignorer. Dans les deux cas il est effacé et n'est plus proposé.
 *
 * Le ViewModel vit avec l'activité : une recréation ne repose pas la question.
 */
@HiltViewModel
class CrashReportViewModel
    @Inject
    constructor(
        private val store: CrashReportStore,
    ) : ViewModel() {
        private val report = MutableStateFlow<String?>(null)
        private val effects = Channel<CrashReportEffect>(Channel.BUFFERED)

        /** Rapport à proposer, `null` s'il n'y en a pas ou si la personne a répondu. */
        val pendingReport: StateFlow<String?> = report.asStateFlow()

        /** Effets à exécuter une fois. */
        val uiEffects: Flow<CrashReportEffect> = effects.receiveAsFlow()

        init {
            viewModelScope.launch { report.value = store.pending() }
        }

        /** Applique la réponse de la personne. */
        fun onEvent(event: CrashReportEvent) {
            val answered = report.value ?: return
            report.value = null
            viewModelScope.launch {
                if (event == CrashReportEvent.Send) effects.send(CrashReportEffect.OpenReport(answered))
                store.clear()
            }
        }
    }
