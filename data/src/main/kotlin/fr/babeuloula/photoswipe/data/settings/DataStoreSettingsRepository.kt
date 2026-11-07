package fr.babeuloula.photoswipe.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.model.RatingInvitation
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import fr.babeuloula.photoswipe.domain.model.Tutorial
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/**
 * [SettingsRepository] sur DataStore Preferences (SPEC §5.3).
 *
 * Une valeur absente prend celle de [ReminderSettings.DEFAULT] : rappel désactivé, 18:00
 * (ADR 0007). L'heure est stockée en minutes depuis minuit ; une valeur hors limites, ou un fichier
 * illisible, retombe sur la valeur par défaut au lieu de faire échouer l'écran. Le thème est stocké
 * par son nom ; un nom inconnu retombe sur [ThemeMode.DEFAULT], le thème du téléphone. De même pour
 * le conseil « Gestion des médias » ([ManageMediaTip.DEFAULT]). La version du tutoriel vue vaut
 * [Tutorial.NEVER_SEEN] tant que rien n'est enregistré, l'invitation à noter l'application
 * [RatingInvitation.DEFAULT] (jour trié stocké en jours depuis l'epoch).
 *
 * @param dataStore magasin de préférences, unique pour son fichier : il est fourni par l'injection
 * de dépendances, jamais créé ici.
 */
class DataStoreSettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : SettingsRepository {
        override suspend fun reminderSettings(): ReminderSettings = observeReminderSettings().first()

        override fun observeReminderSettings(): Flow<ReminderSettings> =
            preferences().map { it.toReminderSettings() }.distinctUntilChanged()

        override fun observeThemeMode(): Flow<ThemeMode> =
            preferences()
                .map { preferences ->
                    ThemeMode.entries.find { it.name == preferences[THEME_MODE] } ?: ThemeMode.DEFAULT
                }.distinctUntilChanged()

        override suspend fun saveThemeMode(mode: ThemeMode) {
            dataStore.edit { it[THEME_MODE] = mode.name }
        }

        override fun observeManageMediaTip(): Flow<ManageMediaTip> =
            preferences()
                .map { preferences ->
                    ManageMediaTip.entries.find { it.name == preferences[MANAGE_MEDIA_TIP] } ?: ManageMediaTip.DEFAULT
                }.distinctUntilChanged()

        override suspend fun saveManageMediaTip(tip: ManageMediaTip) {
            dataStore.edit { it[MANAGE_MEDIA_TIP] = tip.name }
        }

        override suspend fun tutorialVersionSeen(): Int =
            preferences().first()[TUTORIAL_VERSION_SEEN] ?: Tutorial.NEVER_SEEN

        override suspend fun saveTutorialVersionSeen(version: Int) {
            dataStore.edit { it[TUTORIAL_VERSION_SEEN] = version }
        }

        override suspend fun ratingInvitation(): RatingInvitation {
            val preferences = preferences().first()
            return RatingInvitation(
                sortedDays = preferences[RATING_SORTED_DAYS] ?: 0,
                lastSortedDay = preferences[RATING_LAST_SORTED_DAY]?.let(LocalDate::ofEpochDay),
                asked = preferences[RATING_ASKED] ?: false,
            )
        }

        override suspend fun saveRatingInvitation(invitation: RatingInvitation) {
            dataStore.edit { preferences ->
                preferences[RATING_SORTED_DAYS] = invitation.sortedDays
                invitation.lastSortedDay?.let { preferences[RATING_LAST_SORTED_DAY] = it.toEpochDay() }
                    ?: preferences.remove(RATING_LAST_SORTED_DAY)
                preferences[RATING_ASKED] = invitation.asked
            }
        }

        /** Préférences enregistrées ; un fichier illisible se lit comme vide. */
        private fun preferences(): Flow<Preferences> =
            dataStore.data.catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }

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
            val THEME_MODE = stringPreferencesKey("theme_mode")
            val MANAGE_MEDIA_TIP = stringPreferencesKey("manage_media_tip")
            val TUTORIAL_VERSION_SEEN = intPreferencesKey("tutorial_version_seen")
            val RATING_SORTED_DAYS = intPreferencesKey("rating_sorted_days")
            val RATING_LAST_SORTED_DAY = longPreferencesKey("rating_last_sorted_day")
            val RATING_ASKED = booleanPreferencesKey("rating_asked")
            const val MINUTES_PER_HOUR = 60
            const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
        }
    }
