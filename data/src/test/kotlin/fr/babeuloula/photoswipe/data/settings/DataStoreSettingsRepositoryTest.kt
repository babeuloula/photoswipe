package fr.babeuloula.photoswipe.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
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

    private fun TestScope.dataStore(
        file: File = File(folder.root, "settings-${System.nanoTime()}.preferences_pb"),
        scope: CoroutineScope = backgroundScope,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
}
