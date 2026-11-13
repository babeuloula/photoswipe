package fr.babeuloula.photoswipe.data.quality

import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import fr.babeuloula.photoswipe.data.database.PhotoSwipeDatabase
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.PhotoMeasure
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

/** Mesures des photos (PS-164) : cache Room en mémoire et conversion en niveaux de gris. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PhotoQualityDataTest {
    private val database =
        Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), PhotoSwipeDatabase::class.java).build()
    private val cache = RoomPhotoMeasureCache(database.photoMeasureDao())

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun `keeps the measures, replaces them, reads back only those asked and forgets some`() =
        runTest {
            cache.save(mapOf(MediaId(1) to PhotoMeasure(10.0, 20.0), MediaId(2) to PhotoMeasure(30.0, 40.0)))
            cache.save(mapOf(MediaId(2) to PhotoMeasure(31.0, 41.0)))

            assertEquals(mapOf(MediaId(2) to PhotoMeasure(31.0, 41.0)), cache.measures(setOf(MediaId(2), MediaId(3))))

            cache.forget(setOf(MediaId(1)))
            assertEquals(emptyMap<MediaId, PhotoMeasure>(), cache.measures(setOf(MediaId(1))))
        }

    @Test
    fun `reads many measures at once, beyond the query limit`() =
        runTest {
            val many = (1L..1_200L).associate { MediaId(it) to PhotoMeasure(it.toDouble(), 1.0) }
            cache.save(many)

            assertEquals(1_200, cache.measures(many.keys).size)
        }

    @Test
    fun `turns a bitmap into gray levels by luminance`() {
        val bitmap = Bitmap.createBitmap(2, 1, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.WHITE)
        bitmap.setPixel(1, 0, Color.rgb(255, 0, 0))

        val image = gray(bitmap)

        assertEquals(2, image.width)
        assertEquals(1, image.height)
        assertEquals(listOf(255, 76), image.luma.toList())
    }
}
