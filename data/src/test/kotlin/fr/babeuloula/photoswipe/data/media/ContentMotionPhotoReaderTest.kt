package fr.babeuloula.photoswipe.data.media

import android.net.Uri
import fr.babeuloula.photoswipe.domain.fixtures.aJpeg
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aMicroVideoXmp
import fr.babeuloula.photoswipe.domain.fixtures.aMp4
import fr.babeuloula.photoswipe.domain.fixtures.aSamsungTrailer
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MotionPhotoFormat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

/** Photos animées lues sur leur fichier par le `ContentResolver` (PS-167). */
@RunWith(RobolectricTestRunner::class)
class ContentMotionPhotoReaderTest {
    @get:Rule
    val folder = TemporaryFolder()

    private fun photo(
        id: Long,
        bytes: ByteArray,
    ): Pair<File, MediaItem> {
        val file = folder.newFile("$id.jpg").apply { writeBytes(bytes) }
        return file to aMediaItem(id = id).copy(uri = Uri.fromFile(file).toString(), sizeBytes = file.length())
    }

    @Test
    fun `reads a Google and a Samsung motion photo, not an ordinary photo`() =
        runTest {
            val reader =
                ContentMotionPhotoReader(
                    RuntimeEnvironment.getApplication().contentResolver,
                    StandardTestDispatcher(testScheduler),
                )
            val video = aMp4()

            val google = reader.motionPhoto(photo(1, aJpeg(aMicroVideoXmp(video.size), trailer = video)).second)
            val samsung = reader.motionPhoto(photo(2, aJpeg(trailer = aSamsungTrailer(video))).second)

            assertEquals(MotionPhotoFormat.GOOGLE_XMP, google?.format)
            assertEquals(video.size.toLong(), google?.videoLength)
            assertEquals(MotionPhotoFormat.SAMSUNG_TRAILER, samsung?.format)
            assertNull(reader.motionPhoto(photo(3, aJpeg()).second))
        }

    @Test
    fun `keeps what it read, and a missing file is an ordinary photo`() =
        runTest {
            val reader =
                ContentMotionPhotoReader(
                    RuntimeEnvironment.getApplication().contentResolver,
                    StandardTestDispatcher(testScheduler),
                )
            val video = aMp4()
            val (file, media) = photo(1, aJpeg(aMicroVideoXmp(video.size), trailer = video))

            val first = reader.motionPhoto(media)
            file.delete()

            assertEquals(first, reader.motionPhoto(media))
            assertNull(reader.motionPhoto(media.copy(sizeBytes = 1)))
        }
}
