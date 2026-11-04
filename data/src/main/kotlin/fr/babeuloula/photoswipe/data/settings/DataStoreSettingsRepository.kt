package fr.babeuloula.photoswipe.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalTime

/**
 * [SettingsRepository] sur DataStore Preferences (SPEC §5.3).
 *
 * Une valeur absente prend celle de [ReminderSettings.DEFAULT] : rappel désactivé, 18:00
 * (ADR 0007). L'heure est stockée en minutes depuis minuit ; une valeur hors limites, ou un fichier
 * illisible, retombe sur la valeur par défaut au lieu de faire échouer l'écran.
 *
 * @param dataStore magasin de préférences, unique pour son fichier : il est fourni par l'injection
 * de dépendances, jamais créé ici.
 */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override suspend fun reminderSettings(): ReminderSettings = observeReminderSettings().first()

    override fun observeReminderSettings(): Flow<ReminderSettings> =
        dataStore.data
            .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
            .map { it.toReminderSettings() }
            .distinctUntilChanged()

    override suspend fun saveReminderSettings(settings: ReminderSettings) {
        dataStore.edit { preferences ->
            preferences[REMINDER_ENABLED] = settings.enabled
            preferences[REMINDER_MINUTE_OF_DAY] = settings.time.hour * MINUTES_PER_HOUR + settings.time.minute
        }
    }

    private fun Preferences.toReminderSettings(): ReminderSettings {
        val minuteOfDay = this[REMINDER_MINUTE_OF_DAY]
        val time =
            if (minuteOfDay != null && minuteOfDay in 0 until MINUTES_PER_DAY) {
                LocalTime.of(minuteOfDay / MINUTES_PER_HOUR, minuteOfDay % MINUTES_PER_HOUR)
            } else {
                ReminderSettings.DEFAULT_TIME
            }
        return ReminderSettings(enabled = this[REMINDER_ENABLED] ?: ReminderSettings.DEFAULT.enabled, time = time)
    }

    private companion object {
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_MINUTE_OF_DAY = intPreferencesKey("reminder_minute_of_day")
        const val MINUTES_PER_HOUR = 60
        const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
    }
}
