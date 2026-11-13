package fr.babeuloula.photoswipe.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import fr.babeuloula.photoswipe.domain.usecase.UpdateReminderSettings
import fr.babeuloula.photoswipe.permission.ExactAlarmPermissionReader
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
 * @property themeMode thème de l'application, `null` tant qu'il n'est pas lu.
 * @property animationsEnabled vrai si les animations de l'application sont actives (PS-146).
 */
data class SettingsUiState(
    val reminder: ReminderSettings? = null,
    val timePickerOpen: Boolean = false,
    val themeMode: ThemeMode? = null,
    val animationsEnabled: Boolean = true,
)

/** Action de l'utilisateur sur les réglages. */
sealed interface SettingsEvent {
    /** Action qui ne fait qu'ouvrir un autre écran : elle se traduit telle quelle en [effect]. */
    sealed interface OpensScreen : SettingsEvent {
        /** Écran à ouvrir. */
        val effect: SettingsEffect
    }

    /**
     * Interrupteur du rappel quotidien.
     *
     * @property enabled nouvelle position de l'interrupteur.
     */
    data class ReminderToggled(
        val enabled: Boolean,
    ) : SettingsEvent

    /** Appui sur l'heure du rappel : ouvrir le sélecteur. */
    data object PickReminderTime : SettingsEvent

    /**
     * Heure choisie dans le sélecteur.
     *
     * @property time heure du rappel.
     */
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
    data object OpenNotificationSettings : OpensScreen {
        override val effect: SettingsEffect get() = SettingsEffect.OpenNotificationSettings
    }

    /** Action sur le thème de l'application. */
    sealed interface ThemeEvent : SettingsEvent

    /**
     * Thème choisi.
     *
     * @property mode thème de l'application.
     */
    data class ThemePicked(
        val mode: ThemeMode,
    ) : ThemeEvent

    /**
     * Interrupteur « Animations » (PS-146), pour alléger l'application sur un téléphone ancien.
     *
     * @property enabled vrai pour garder les animations.
     */
    data class AnimationsToggled(
        val enabled: Boolean,
    ) : ThemeEvent

    /** « Langue » : ouvrir le réglage de langue par application d'Android. */
    data object OpenLanguageSettings : OpensScreen {
        override val effect: SettingsEffect get() = SettingsEffect.OpenLanguageSettings
    }

    /** « Outils développeur », en debug seulement. */
    data object OpenDeveloperTools : OpensScreen {
        override val effect: SettingsEffect get() = SettingsEffect.OpenDeveloperTools
    }

    /** « Progression » : les statistiques d'utilisation, sur leur propre page (PS-153). */
    data object OpenProgress : OpensScreen {
        override val effect: SettingsEffect get() = SettingsEffect.OpenProgress
    }

    /** « Revoir le tutoriel » de la section « À propos ». */
    data object OpenTutorial : OpensScreen {
        override val effect: SettingsEffect get() = SettingsEffect.OpenTutorial
    }

    /** « Licences » de la section « À propos ». */
    data object OpenLicenses : OpensScreen {
        override val effect: SettingsEffect get() = SettingsEffect.OpenLicenses
    }

    /** « Code source » de la section « À propos » : le dépôt GitHub (PS-127). */
    data object OpenSourceCode : OpensScreen {
        override val effect: SettingsEffect get() = SettingsEffect.OpenSourceCode
    }

    /** « Rappel à l'heure pile » : ouvrir « Alarmes et rappels » d'Android (PS-125). */
    data object OpenExactAlarmSettings : OpensScreen {
        override val effect: SettingsEffect get() = SettingsEffect.OpenExactAlarmSettings
    }

    /** « Noter PhotoSwipe » : la fiche de l'application sur le Play Store (PS-120). */
    data object RateApp : OpensScreen {
        override val effect: SettingsEffect get() = SettingsEffect.OpenRating
    }

    /** « Soutenir le développeur » : la page de don PayPal (PS-120). */
    data object Donate : OpensScreen {
        override val effect: SettingsEffect get() = SettingsEffect.OpenDonation
    }

    /** Flèche de retour. */
    data object Back : OpensScreen {
        override val effect: SettingsEffect get() = SettingsEffect.Close
    }
}

/** Navigation demandée par les réglages. */
sealed interface SettingsEffect {
    /** Revenir à l'écran précédent. */
    data object Close : SettingsEffect

    /** Ouvrir « Alarmes et rappels » d'Android pour PhotoSwipe (PS-125). */
    data object OpenExactAlarmSettings : SettingsEffect

    /** Ouvrir le réglage de langue par application d'Android 13+ (SPEC §9). */
    data object OpenLanguageSettings : SettingsEffect

    /** Ouvrir les outils développeur (variante debug). */
    data object OpenDeveloperTools : SettingsEffect

    /** Revoir le tutoriel, sans changer l'état « tutoriel vu ». */
    data object OpenTutorial : SettingsEffect

    /** Ouvrir la page « Progression » (PS-153). */
    data object OpenProgress : SettingsEffect

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

    /** Ouvrir la fiche de l'application sur le Play Store, pour la noter. */
    data object OpenRating : SettingsEffect

    /** Ouvrir la page de don PayPal. */
    data object OpenDonation : SettingsEffect

    /** Ouvrir le dépôt du code source dans le navigateur. */
    data object OpenSourceCode : SettingsEffect
}

/**
 * ViewModel des réglages : il observe les réglages du rappel enregistrés et enregistre chaque
 * changement par `UpdateReminderSettings`, qui reprogramme la tâche du rappel (SPEC §3.6). Il
 * observe aussi le thème de l'application et enregistre celui choisi ; `MainActivity`, qui
 * l'observe de son côté, l'applique aussitôt.
 * L'heure se choisit aussi rappel désactivé : elle servira à l'activation.
 *
 * Activer le rappel demande d'abord la permission de notification, seulement à ce moment-là
 * (SPEC §3.6) : refusée, le rappel reste désactivé et l'écran l'explique.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val updateReminderSettings: UpdateReminderSettings,
        private val notificationPermission: NotificationPermissionReader,
        /** Entrée des outils développeur : présente en debug seulement (SPEC §10). */
        val developerTools: DeveloperToolsEntry,
        /** Autorisation des alarmes exactes, relue par l'écran à chaque retour au premier plan (PS-125). */
        val exactAlarms: ExactAlarmPermissionReader,
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
            viewModelScope.launch {
                settingsRepository
                    .observeThemeMode()
                    .catch { emit(ThemeMode.DEFAULT) }
                    .collect { mode -> state.update { it.copy(themeMode = mode) } }
            }
            viewModelScope.launch {
                settingsRepository
                    .observeAnimationsEnabled()
                    .catch { emit(true) }
                    .collect { enabled -> state.update { it.copy(animationsEnabled = enabled) } }
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

                is SettingsEvent.ThemeEvent -> {
                    onThemeEvent(event)
                }

                is SettingsEvent.OpensScreen -> {
                    effects.trySend(event.effect)
                }
            }
        }

        /** Enregistre le thème choisi ou le choix des animations, dans la section « Apparence ». */
        private fun onThemeEvent(event: SettingsEvent.ThemeEvent) {
            viewModelScope.launch {
                runCatching {
                    when (event) {
                        is SettingsEvent.ThemePicked -> settingsRepository.saveThemeMode(event.mode)
                        is SettingsEvent.AnimationsToggled -> settingsRepository.saveAnimationsEnabled(event.enabled)
                    }
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
