package fr.babeuloula.photoswipe.data.media

import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import fr.babeuloula.photoswipe.domain.model.MediaId
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Instant

/** Suppression définitive d'Android 10, sans dialogue ni corbeille (PS-184), face au faux MediaStore. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class LegacyMediaDeleterTest {
    private val provider = Robolectric.setupContentProvider(FakeMediaStoreProvider::class.java, MediaStore.AUTHORITY)
    private val resolver = RuntimeEnvironment.getApplication().contentResolver
    private val deleter = LegacyMediaDeleter(resolver, UnconfinedTestDispatcher())
    private val source = MediaStoreMediaSource(resolver, UnconfinedTestDispatcher())
    private val taken = Instant.parse("2025-07-15T10:00:00Z")

    private val images = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    private val videos = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    private fun request(vararg media: Pair<Long, Uri>) =
        DirectDeletionRequest(
            media.mapTo(LinkedHashSet()) { MediaId(it.first) },
            media.map { (id, collection) -> ContentUris.withAppendedId(collection, id) },
        )

    private suspend fun remainingIds(): List<Long> =
        source.mediaTakenIn(listOf(taken..<taken.plusSeconds(1))).map { it.id.value }.sorted()

    @Test
    fun `deletes photos and videos for good and returns them`() =
        runTest {
            provider.addImage(id = 1, dateTaken = taken.toEpochMilli())
            provider.addVideo(id = 2, dateTaken = taken.toEpochMilli())
            provider.addImage(id = 3, dateTaken = taken.toEpochMilli())

            val deleted = deleter.delete(request(1L to images, 2L to videos))

            assertEquals(setOf(MediaId(1), MediaId(2)), deleted?.mediaIds)
            assertEquals(listOf(3L), remainingIds())
        }

    @Test
    fun `leaves out a media that Android refuses to delete, or that is already gone`() =
        runTest {
            provider.addImage(id = 1, dateTaken = taken.toEpochMilli())
            provider.addImage(id = 2, dateTaken = taken.toEpochMilli())
            provider.protectedIds += 2

            val deleted = deleter.delete(request(1L to images, 2L to images, 9L to images))

            assertEquals(setOf(MediaId(1)), deleted?.mediaIds)
            assertEquals(listOf(ContentUris.withAppendedId(images, 1)), deleted?.uris)
            assertEquals(listOf(2L), remainingIds())
        }

    @Test
    fun `returns nothing when no media could be deleted`() =
        runTest {
            provider.addImage(id = 1, dateTaken = taken.toEpochMilli())
            provider.protectedIds += 1

            assertNull(deleter.delete(request(1L to images)))
            assertTrue(remainingIds().isNotEmpty())
        }
}
