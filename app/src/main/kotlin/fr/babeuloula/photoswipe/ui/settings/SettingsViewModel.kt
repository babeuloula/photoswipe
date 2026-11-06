package fr.babeuloula.photoswipe.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.usecase.UpdateReminderSettings
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

/**
 * État des réglages (SPEC §3.6).
 *
 * @property reminder réglages du rappel quotidien, `null` tant qu'ils ne sont pas lus.
 * @property timePickerOpen vrai pendant le choix de l'heure du rappel.
 */
data class SettingsUiState(
    val reminder: ReminderSettings? = null,
    val timePickerOpen: Boolean = false,
)

/** Action de l'utilisateur sur les réglages. */
sealed interface SettingsEvent {
    /** Interrupteur du rappel quotidien. */
    data class ReminderToggled(
        val enabled: Boolean,
    ) : SettingsEvent

    /** Appui sur l'heure du rappel : ouvrir le sélecteur. */
    data object PickReminderTime : SettingsEvent

    /** Heure choisie dans le sélecteur. */
    data class ReminderTimePicked(
        val time: LocalTime,
    ) : SettingsEvent

    /** Sélecteur fermé sans choisir. */
    data object TimePickerDismissed : SettingsEvent

    /** Flèche de retour. */
    data object Back : SettingsEvent
}

/** Navigation demandée par les réglages. */
sealed interface SettingsEffect {
    /** Revenir à l'écran précédent. */
    data object Close : SettingsEffect
}

/**
 * ViewModel des réglages : il observe les réglages du rappel enregistrés et enregistre chaque
 * changement par `UpdateReminderSettings`, qui reprogramme la tâche du rappel (SPEC §3.6).
 * L'heure se choisit aussi rappel désactivé : elle servira à l'activation.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        settingsRepository: SettingsRepository,
        private val updateReminderSettings: UpdateReminderSettings,
    ) : ViewModel() {
        private val state = MutableStateFlow(SettingsUiState())
        private val effects = Channel<SettingsEffect>(Channel.BUFFERED)

        /** État des réglages. */
        val uiState: StateFlow<SettingsUiState> = state.asStateFlow()

        /** Navigations à exécuter, chacune livrée une seule fois. */
        val uiEffects: Flow<SettingsEffect> = effects.receiveAsFlow()

        init {
            viewModelScope.launch {
                settingsRepository
                    .observeReminderSettings()
                    .catch { emit(ReminderSettings.DEFAULT) }
                    .collect { reminder -> state.update { it.copy(reminder = reminder) } }
            }
        }

        /** Traite une action de l'utilisateur. */
        fun onEvent(event: SettingsEvent) {
            when (event) {
                is SettingsEvent.ReminderToggled -> {
                    save { it.copy(enabled = event.enabled) }
                }

                SettingsEvent.PickReminderTime -> {
                    state.update { it.copy(timePickerOpen = it.reminder != null) }
                }

                is SettingsEvent.ReminderTimePicked -> {
                    state.update { it.copy(timePickerOpen = false) }
                    save { it.copy(time = event.time) }
                }

                SettingsEvent.TimePickerDismissed -> {
                    state.update { it.copy(timePickerOpen = false) }
                }

                SettingsEvent.Back -> {
                    effects.trySend(SettingsEffect.Close)
                }
            }
        }

        /**
         * Enregistre les réglages du rappel modifiés par [change]. L'écran suit la valeur observée :
         * si l'écriture échoue, il garde l'ancienne.
         */
        private fun save(change: (ReminderSettings) -> ReminderSettings) {
            val current = state.value.reminder ?: return
            viewModelScope.launch { runCatching { updateReminderSettings(change(current)) } }
        }
    }
