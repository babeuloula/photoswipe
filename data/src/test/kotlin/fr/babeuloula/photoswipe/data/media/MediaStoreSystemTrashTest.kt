package fr.babeuloula.photoswipe.data.media

import android.os.Build
import android.provider.MediaStore
import fr.babeuloula.photoswipe.domain.model.MediaId
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * [MediaStoreSystemTrash] sur Android 10, qui n'a pas de corbeille (PS-181). La lecture de la
 * corbeille d'Android 11+ est vérifiée sur un vrai MediaStore par `TrashInstrumentedTest`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class MediaStoreSystemTrashTest {
    private val provider = Robolectric.setupContentProvider(FakeMediaStoreProvider::class.java, MediaStore.AUTHORITY)
    private val trash =
        MediaStoreSystemTrash(RuntimeEnvironment.getApplication().contentResolver, UnconfinedTestDispatcher())

    @Test
    fun `finds nothing in the trash on android 10, without querying the gallery`() =
        runTest {
            provider.addImage(id = 1, dateTaken = 0)

            assertTrue(trash.trashed(setOf(MediaId(1))).isEmpty())
            assertTrue(provider.queries.isEmpty())
        }
}
