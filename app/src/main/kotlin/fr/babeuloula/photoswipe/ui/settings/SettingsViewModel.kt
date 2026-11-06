package fr.babeuloula.photoswipe.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.usecase.UpdateReminderSettings
import fr.babeuloula.photoswipe.permission.NotificationPermissionReader
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

    /**
     * Réponse à la demande de permission de notification.
     *
     * @property granted vrai si l'utilisateur l'a accordée.
     * @property canAskAgain vrai si Android acceptera de la redemander ; sinon, seuls les réglages
     * système le peuvent.
     */
    data class NotificationPermissionAnswered(
        val granted: Boolean,
        val canAskAgain: Boolean,
    ) : SettingsEvent

    /** « Réglages » du message de refus : ouvrir les réglages de notification de l'application. */
    data object OpenNotificationSettings : SettingsEvent

    /** « Langue » : ouvrir le réglage de langue par application d'Android. */
    data object OpenLanguageSettings : SettingsEvent

    /** « Outils développeur », en debug seulement. */
    data object OpenDeveloperTools : SettingsEvent

    /** « Licences » de la section « À propos ». */
    data object OpenLicenses : SettingsEvent

    /** Flèche de retour. */
    data object Back : SettingsEvent
}

/** Navigation demandée par les réglages. */
sealed interface SettingsEffect {
    /** Revenir à l'écran précédent. */
    data object Close : SettingsEffect

    /** Ouvrir le réglage de langue par application d'Android 13+ (SPEC §9). */
    data object OpenLanguageSettings : SettingsEffect

    /** Ouvrir les outils développeur (variante debug). */
    data object OpenDeveloperTools : SettingsEffect

    /** Ouvrir les licences de l'application et de ses bibliothèques. */
    data object OpenLicenses : SettingsEffect

    /** Afficher la demande système `POST_NOTIFICATIONS` ; la réponse revient par un événement. */
    data object RequestNotificationPermission : SettingsEffect

    /**
     * Notifications refusées : le rappel reste désactivé, l'écran explique pourquoi.
     *
     * @property canAskAgain faux si Android ne redemandera plus : le message propose les réglages.
     */
    data class NotificationPermissionDenied(
        val canAskAgain: Boolean,
    ) : SettingsEffect

    /** Ouvrir les réglages de notification de l'application. */
    data object OpenNotificationSettings : SettingsEffect
}

/**
 * ViewModel des réglages : il observe les réglages du rappel enregistrés et enregistre chaque
 * changement par `UpdateReminderSettings`, qui reprogramme la tâche du rappel (SPEC §3.6).
 * L'heure se choisit aussi rappel désactivé : elle servira à l'activation.
 *
 * Activer le rappel demande d'abord la permission de notification, seulement à ce moment-là
 * (SPEC §3.6) : refusée, le rappel reste désactivé et l'écran l'explique.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        settingsRepository: SettingsRepository,
        private val updateReminderSettings: UpdateReminderSettings,
        private val notificationPermission: NotificationPermissionReader,
        /** Entrée des outils développeur : présente en debug seulement (SPEC §10). */
        val developerTools: DeveloperToolsEntry,
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
                    toggleReminder(event.enabled)
                }

                is SettingsEvent.NotificationPermissionAnswered -> {
                    if (event.granted) {
                        save { it.copy(enabled = true) }
                    } else {
                        effects.trySend(SettingsEffect.NotificationPermissionDenied(event.canAskAgain))
                    }
                }

                SettingsEvent.OpenNotificationSettings -> {
                    effects.trySend(SettingsEffect.OpenNotificationSettings)
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

                SettingsEvent.OpenLanguageSettings -> {
                    effects.trySend(SettingsEffect.OpenLanguageSettings)
                }

                SettingsEvent.OpenDeveloperTools -> {
                    effects.trySend(SettingsEffect.OpenDeveloperTools)
                }

                SettingsEvent.OpenLicenses -> {
                    effects.trySend(SettingsEffect.OpenLicenses)
                }

                SettingsEvent.Back -> {
                    effects.trySend(SettingsEffect.Close)
                }
            }
        }

        /** Active le rappel si les notifications sont permises, sinon les demande d'abord ; le désactive. */
        private fun toggleReminder(enabled: Boolean) {
            if (enabled && !notificationPermission.isGranted()) {
                effects.trySend(SettingsEffect.RequestNotificationPermission)
            } else {
                save { it.copy(enabled = enabled) }
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
