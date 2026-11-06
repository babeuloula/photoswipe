package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.port.ReminderScheduler
import fr.babeuloula.photoswipe.domain.port.SettingsRepository

/**
 * Enregistre les réglages du rappel quotidien et reprogramme sa tâche (SPEC §3.6) : activé, le
 * rappel est planifié à sa nouvelle heure (la planification précédente est remplacée) ;
 * désactivé, il est annulé.
 *
 * Les réglages sont écrits avant la planification : si elle échoue, le prochain enregistrement
 * repartira des bons réglages.
 */
class UpdateReminderSettings(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    /** Enregistre [settings] et planifie ou annule le rappel en conséquence. */
    suspend operator fun invoke(settings: ReminderSettings) {
        settingsRepository.saveReminderSettings(settings)
        if (settings.enabled) reminderScheduler.schedule(settings.time) else reminderScheduler.cancel()
    }
}
