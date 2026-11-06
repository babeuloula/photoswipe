package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [SettingsRepository] en mémoire : [ReminderSettings.DEFAULT] tant que rien n'est enregistré,
 * comme DataStore ; [observeReminderSettings] émet la valeur actuelle puis chaque nouvelle valeur.
 *
 * @param reminder réglages présents au départ.
 */
class FakeSettingsRepository(
    reminder: ReminderSettings = ReminderSettings.DEFAULT,
) : SettingsRepository {
    private val stored = MutableStateFlow(reminder)

    /** Réglages du rappel enregistrés, pour les vérifications du test. */
    val reminder: ReminderSettings get() = stored.value

    override suspend fun reminderSettings(): ReminderSettings = stored.value

    override fun observeReminderSettings(): Flow<ReminderSettings> = stored.asStateFlow()

    override suspend fun saveReminderSettings(settings: ReminderSettings) {
        stored.value = settings
    }
}
