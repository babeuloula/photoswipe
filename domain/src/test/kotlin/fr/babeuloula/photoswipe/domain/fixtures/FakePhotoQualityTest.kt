package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.PhotoMeasure
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Les fakes de la détection des photos ratées respectent le contrat de leurs ports. */
class FakePhotoQualityTest {
    @Test
    fun `keeps, reads back and forgets measures`() =
        runTest {
            val cache = FakePhotoMeasureCache()
            cache.save(mapOf(MediaId(1) to PhotoMeasure(10.0, 20.0), MediaId(2) to PhotoMeasure(30.0, 40.0)))

            assertEquals(mapOf(MediaId(2) to PhotoMeasure(30.0, 40.0)), cache.measures(setOf(MediaId(2), MediaId(3))))

            cache.forget(setOf(MediaId(2)))
            assertEquals(setOf(MediaId(1)), cache.measures.keys)
        }

    @Test
    fun `gives the images it was given and nothing for the others`() =
        runTest {
            val analyzer = FakePhotoAnalyzer(mapOf(MediaId(1) to aFlatImage(10)))

            assertEquals(10, analyzer.grayThumbnail(aMediaItem(id = 1))?.luma?.first())
            assertNull(analyzer.grayThumbnail(aMediaItem(id = 2)))
            assertEquals(listOf(MediaId(1), MediaId(2)), analyzer.read)
        }
}
