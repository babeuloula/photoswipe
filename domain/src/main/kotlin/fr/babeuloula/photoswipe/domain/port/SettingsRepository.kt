package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.ReminderSettings

/**
 * Préférences de l'utilisateur (SPEC §5.2). Implémenté dans `:data` sur DataStore.
 *
 * La langue n'y figure pas : elle passe par le réglage de langue par application d'Android
 * (SPEC §4). Les réglages du tutoriel arriveront avec l'epic 11.
 */
interface SettingsRepository {
    /** Réglages du rappel quotidien, [ReminderSettings.DEFAULT] tant que rien n'a été enregistré. */
    suspend fun reminderSettings(): ReminderSettings

    /** Enregistre les réglages du rappel quotidien. */
    suspend fun saveReminderSettings(settings: ReminderSettings)
}
