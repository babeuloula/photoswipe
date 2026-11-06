package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [SettingsRepository] en mémoire : [ReminderSettings.DEFAULT] tant que rien n'est enregistré,
 * comme DataStore ; [observeReminderSettings] émet la valeur actuelle puis chaque nouvelle valeur.
 *
 * Il en va de même du thème ([ThemeMode.DEFAULT] au départ).
 *
 * @param reminder réglages présents au départ.
 * @param themeMode thème présent au départ.
 */
class FakeSettingsRepository(
    reminder: ReminderSettings = ReminderSettings.DEFAULT,
    themeMode: ThemeMode = ThemeMode.DEFAULT,
) : SettingsRepository {
    private val stored = MutableStateFlow(reminder)
    private val storedTheme = MutableStateFlow(themeMode)

    /** Thème enregistré, pour les vérifications du test. */
    val themeMode: ThemeMode get() = storedTheme.value

    /** Réglages du rappel enregistrés, pour les vérifications du test. */
    val reminder: ReminderSettings get() = stored.value

    override suspend fun reminderSettings(): ReminderSettings = stored.value

    override fun observeReminderSettings(): Flow<ReminderSettings> = stored.asStateFlow()

    override suspend fun saveReminderSettings(settings: ReminderSettings) {
        stored.value = settings
    }

    override fun observeThemeMode(): Flow<ThemeMode> = storedTheme.asStateFlow()

    override suspend fun saveThemeMode(mode: ThemeMode) {
        storedTheme.value = mode
    }
}
