package fr.babeuloula.photoswipe.ui.deck

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import fr.babeuloula.photoswipe.domain.fixtures.aMotionPhoto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** La vidéo d'une photo animée se lit comme un fichier à part, dans la plage du fichier de la photo (PS-167). */
@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class MotionPhotoVideoTest {
    private val photo = "content://media/external/images/media/7"

    /** Source qui note ce qu'on lui demande d'ouvrir. */
    private class RecordingDataSource : DataSource {
        val opened = mutableListOf<DataSpec>()

        override fun addTransferListener(transferListener: TransferListener) = Unit

        override fun open(dataSpec: DataSpec): Long {
            opened += dataSpec
            return dataSpec.length
        }

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int = 0

        override fun getUri(): Uri? = opened.lastOrNull()?.uri

        override fun close() = Unit
    }

    @Test
    fun `describes the video range in a URI and reads it back`() {
        val uri = motionVideoUri(photo, aMotionPhoto(videoStart = 1_000, videoLength = 500))

        assertEquals(MotionVideoRange(photo, 1_000, 500), MotionVideoRange.of(uri))
        assertNull(MotionVideoRange.of(photo))
        assertNull(MotionVideoRange.of("$photo#other"))
    }

    @Test
    fun `opens the video range of the photo file, from where the player asks`() {
        val upstream = RecordingDataSource()
        val source = MotionPhotoDataSource(upstream)
        val uri = Uri.parse(MotionVideoRange(photo, 1_000, 500).toUri())

        assertEquals(500L, source.open(DataSpec(uri)))
        assertEquals(
            400L,
            source.open(
                DataSpec
                    .Builder()
                    .setUri(uri)
                    .setPosition(100)
                    .build(),
            ),
        )
        source.open(
            DataSpec
                .Builder()
                .setUri(uri)
                .setPosition(100)
                .setLength(50)
                .build(),
        )

        assertEquals(listOf(photo), upstream.opened.map { it.uri.toString() }.distinct())
        assertEquals(listOf(1_000L, 1_100L, 1_100L), upstream.opened.map { it.position })
        assertEquals(listOf(500L, 400L, 50L), upstream.opened.map { it.length })
    }

    @Test
    fun `passes any other URI as it is`() {
        val upstream = RecordingDataSource()
        val spec = DataSpec(Uri.parse("content://media/external/video/media/3"))

        MotionPhotoDataSource(upstream).open(spec)

        assertEquals(spec, upstream.opened.single())
    }
}
