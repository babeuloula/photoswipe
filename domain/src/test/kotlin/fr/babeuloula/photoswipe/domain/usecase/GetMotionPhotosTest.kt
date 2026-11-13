package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.fixtures.FakeMotionPhotoReader
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.fixtures.aMotionPhoto
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class GetMotionPhotosTest {
    private val motion = aMotionPhoto()
    private val reader = FakeMotionPhotoReader(mapOf(MediaId(1) to motion, MediaId(3) to motion))
    private val getMotionPhotos = GetMotionPhotos(reader)

    @Test
    fun `returns the motion photos among the photos`() =
        runTest {
            val media = (1L..2L).map { aMediaItem(id = it) }

            assertEquals(mapOf(MediaId(1) to motion), getMotionPhotos(media))
        }

    @Test
    fun `reads each photo once and never a video`() =
        runTest {
            val video = aMediaItem(id = 3, type = MediaType.VIDEO, duration = Duration.ofSeconds(3))

            getMotionPhotos(listOf(aMediaItem(id = 1), aMediaItem(id = 1), video))

            assertEquals(listOf(MediaId(1)), reader.read)
        }
}
