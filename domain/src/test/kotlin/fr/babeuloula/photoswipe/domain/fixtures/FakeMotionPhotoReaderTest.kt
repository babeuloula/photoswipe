package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.MediaId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Le faux lecteur de photos animées respecte le contrat de son port. */
class FakeMotionPhotoReaderTest {
    @Test
    fun `gives the motion photos it was given and nothing for the others`() =
        runTest {
            val motion = aMotionPhoto()
            val reader = FakeMotionPhotoReader(mapOf(MediaId(1) to motion))

            assertEquals(motion, reader.motionPhoto(aMediaItem(id = 1)))
            assertNull(reader.motionPhoto(aMediaItem(id = 2)))
            assertEquals(listOf(MediaId(1), MediaId(2)), reader.read)
        }

    @Test
    fun `builds a JPEG that starts and ends like one, before its trailer`() {
        val jpeg = aJpeg(trailer = aMp4())

        assertEquals(listOf(0xFF, 0xD8), jpeg.take(2).map { it.toInt() and 0xFF })
        assertEquals("ftyp", String(jpeg, jpeg.size - aMp4().size + 4, 4, Charsets.US_ASCII))
    }
}
