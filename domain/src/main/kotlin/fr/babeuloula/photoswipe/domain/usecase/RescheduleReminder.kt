package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.port.ReminderScheduler
import fr.babeuloula.photoswipe.domain.port.SettingsRepository

/**
 * Replanifie le rappel quotidien à partir des réglages enregistrés (SPEC §3.6), après un
 * changement d'heure ou de fuseau : le délai jusqu'au prochain rappel est recalculé. Un rappel
 * désactivé est annulé, au cas où une planification aurait survécu.
 */
class RescheduleReminder(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    /** Planifie ou annule le rappel selon les réglages enregistrés. */
    suspend operator fun invoke() {
        val settings = settingsRepository.reminderSettings()
        if (settings.enabled) reminderScheduler.schedule(settings.time) else reminderScheduler.cancel()
    }
}
