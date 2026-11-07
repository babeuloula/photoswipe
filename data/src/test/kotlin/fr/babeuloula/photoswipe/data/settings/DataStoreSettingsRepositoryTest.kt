package fr.babeuloula.photoswipe.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.model.RatingInvitation
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import fr.babeuloula.photoswipe.domain.model.Tutorial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/** DataStore réel, sur un fichier temporaire : aucune dépendance au framework Android. */
class DataStoreSettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `reads the default reminder settings when nothing is stored`() =
        runTest {
            val repository = DataStoreSettingsRepository(dataStore())

            assertEquals(ReminderSettings.DEFAULT, repository.reminderSettings())
            assertEquals(ReminderSettings(enabled = false, time = LocalTime.of(18, 0)), repository.reminderSettings())
        }

    @Test
    fun `reads back the saved reminder settings`() =
        runTest {
            val repository = DataStoreSettingsRepository(dataStore())
            val settings = ReminderSettings(enabled = true, time = LocalTime.of(7, 45))

            repository.saveReminderSettings(settings)

            assertEquals(settings, repository.reminderSettings())
        }

    @Test
    fun `keeps the saved settings for a new repository on the same file`() =
        runTest {
            val file = File(folder.root, "settings.preferences_pb")
            val settings = ReminderSettings(enabled = true, time = LocalTime.of(23, 59))
            // DataStore n'admet qu'une instance active par fichier : la première est arrêtée avant d'ouvrir la seconde.
            val firstStore = Job()
            DataStoreSettingsRepository(dataStore(file, CoroutineScope(backgroundScope.coroutineContext + firstStore)))
                .saveReminderSettings(settings)
            firstStore.cancelAndJoin()

            assertEquals(settings, DataStoreSettingsRepository(dataStore(file)).reminderSettings())
        }

    @Test
    fun `emits the current settings then each saved value`() =
        runTest {
            val repository = DataStoreSettingsRepository(dataStore())
            val emissions = mutableListOf<ReminderSettings>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeReminderSettings().toList(emissions)
            }
            val enabled = ReminderSettings(enabled = true, time = ReminderSettings.DEFAULT_TIME)

            repository.saveReminderSettings(enabled)
            repository.saveReminderSettings(enabled)
            repository.saveReminderSettings(enabled.copy(time = LocalTime.of(9, 0)))
            testScheduler.advanceUntilIdle()

            assertEquals(listOf(ReminderSettings.DEFAULT, enabled, enabled.copy(time = LocalTime.of(9, 0))), emissions)
        }

    @Test
    fun `falls back to the default time when the stored time is out of range`() =
        runTest {
            val dataStore = dataStore()
            dataStore.edit { it[intPreferencesKey("reminder_minute_of_day")] = 24 * 60 }

            assertEquals(ReminderSettings.DEFAULT_TIME, DataStoreSettingsRepository(dataStore).reminderSettings().time)
        }

    @Test
    fun `follows the phone theme when no theme is stored`() =
        runTest {
            assertEquals(ThemeMode.SYSTEM, DataStoreSettingsRepository(dataStore()).observeThemeMode().first())
        }

    @Test
    fun `emits the current theme then each saved theme, apart from the reminder`() =
        runTest {
            val repository = DataStoreSettingsRepository(dataStore())
            val emissions = mutableListOf<ThemeMode>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                repository.observeThemeMode().toList(emissions)
            }

            repository.saveThemeMode(ThemeMode.DARK)
            repository.saveReminderSettings(ReminderSettings.DEFAULT.copy(enabled = true))
            repository.saveThemeMode(ThemeMode.LIGHT)
            testScheduler.advanceUntilIdle()

            assertEquals(listOf(ThemeMode.SYSTEM, ThemeMode.DARK, ThemeMode.LIGHT), emissions)
            assertEquals(ReminderSettings.DEFAULT.copy(enabled = true), repository.reminderSettings())
        }

    @Test
    fun `falls back to the phone theme when the stored theme is unknown`() =
        runTest {
            val dataStore = dataStore()
            dataStore.edit { it[stringPreferencesKey("theme_mode")] = "SEPIA" }

            assertEquals(ThemeMode.SYSTEM, DataStoreSettingsRepository(dataStore).observeThemeMode().first())
        }

    @Test
    fun `stores the media management tip, never offered by default`() =
        runTest {
            val repository = DataStoreSettingsRepository(dataStore())
            assertEquals(ManageMediaTip.NOT_OFFERED, repository.observeManageMediaTip().first())

            repository.saveManageMediaTip(ManageMediaTip.DISMISSED)

            assertEquals(ManageMediaTip.DISMISSED, repository.observeManageMediaTip().first())
        }

    @Test
    fun `stores the rating invitation, nothing sorted nor asked by default`() =
        runTest {
            val repository = DataStoreSettingsRepository(dataStore())
            assertEquals(RatingInvitation.DEFAULT, repository.ratingInvitation())

            val saved = RatingInvitation(sortedDays = 2, lastSortedDay = LocalDate.of(2026, 11, 7), asked = true)
            repository.saveRatingInvitation(saved)

            assertEquals(saved, repository.ratingInvitation())
        }

    @Test
    fun `stores the seen tutorial version, never seen by default, for a new repository too`() =
        runTest {
            val file = File(folder.root, "tutorial.preferences_pb")
            val firstStore = Job()
            val repository =
                DataStoreSettingsRepository(
                    dataStore(
                        file,
                        CoroutineScope(backgroundScope.coroutineContext + firstStore),
                    ),
                )
            assertEquals(Tutorial.NEVER_SEEN, repository.tutorialVersionSeen())

            repository.saveTutorialVersionSeen(Tutorial.CURRENT_VERSION)
            firstStore.cancelAndJoin()

            assertEquals(Tutorial.CURRENT_VERSION, DataStoreSettingsRepository(dataStore(file)).tutorialVersionSeen())
        }

    private fun TestScope.dataStore(
        file: File = File(folder.root, "settings-${System.nanoTime()}.preferences_pb"),
        scope: CoroutineScope = backgroundScope,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
}
