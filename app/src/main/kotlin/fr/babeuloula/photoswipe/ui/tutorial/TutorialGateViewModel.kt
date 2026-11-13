package fr.babeuloula.photoswipe.ui.tutorial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.usecase.MarkTutorialSeen
import fr.babeuloula.photoswipe.domain.usecase.RecordFirstInstall
import fr.babeuloula.photoswipe.domain.usecase.ShouldShowTutorial
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Étape du lancement, avant l'accès aux médias (SPEC §4). */
enum class TutorialGateState {
    /** Les réglages ne sont pas encore lus : rien ne s'affiche, pour ne pas montrer un écran de trop. */
    CHECKING,

    /** Le tutoriel s'affiche : jamais vu, ou vu dans une version plus ancienne. */
    SHOWING,

    /** Le tutoriel est vu : la suite de l'application s'affiche. */
    DONE,
}

/**
 * Tutoriel du premier lancement (SPEC §4, ADR 0027) : il s'affiche avant l'écran d'accès aux médias
 * si [ShouldShowTutorial] le demande. Passé ou terminé, il est retenu comme vu ([MarkTutorialSeen])
 * et ne revient plus tant que sa version ne change pas.
 *
 * Le ViewModel vit avec l'activité : une recréation (changement de langue) garde l'étape. Une
 * lecture des réglages qui échoue ne bloque pas l'application : le tutoriel n'est pas montré.
 *
 * Pendant que le tutoriel s'affiche, il retient une première installation ([RecordFirstInstall]),
 * avant de le marquer comme vu : le tutoriel jamais vu la distingue d'une mise à jour, qui annoncera
 * ses nouveautés sur l'accueil (PS-173).
 */
@HiltViewModel
class TutorialGateViewModel
    @Inject
    constructor(
        private val shouldShowTutorial: ShouldShowTutorial,
        private val markTutorialSeen: MarkTutorialSeen,
        private val recordFirstInstall: RecordFirstInstall,
    ) : ViewModel() {
        private val state = MutableStateFlow(TutorialGateState.CHECKING)

        /** Enregistrement d'une première installation, à finir avant de marquer le tutoriel vu. */
        private var firstInstall: Job? = null

        /** Étape actuelle. */
        val uiState: StateFlow<TutorialGateState> = state.asStateFlow()

        init {
            viewModelScope.launch {
                val show = runCatching { shouldShowTutorial() }.getOrDefault(false)
                if (show) firstInstall = launch { runCatching { recordFirstInstall() } }
                state.value = if (show) TutorialGateState.SHOWING else TutorialGateState.DONE
            }
        }

        /** « Passer » ou « C'est parti » : le tutoriel est vu, l'application continue. */
        fun onTutorialDone() {
            if (state.value != TutorialGateState.SHOWING) return
            state.value = TutorialGateState.DONE
            viewModelScope.launch {
                firstInstall?.join()
                runCatching { markTutorialSeen() }
            }
        }
    }
