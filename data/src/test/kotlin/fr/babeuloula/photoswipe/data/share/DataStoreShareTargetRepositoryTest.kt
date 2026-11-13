package fr.babeuloula.photoswipe.data.share

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** DataStore réel, sur un fichier temporaire. */
class DataStoreShareTargetRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `stores the last app chosen to share, none at first`() =
        runTest {
            val dataStore =
                PreferenceDataStoreFactory.create(
                    scope = backgroundScope,
                    produceFile = { File(folder.root, "share.preferences_pb") },
                )
            val repository = DataStoreShareTargetRepository(dataStore)
            assertNull(repository.lastTarget())

            repository.saveLastTarget("com.whatsapp/.ContactPicker")

            assertEquals("com.whatsapp/.ContactPicker", repository.lastTarget())
        }
}
