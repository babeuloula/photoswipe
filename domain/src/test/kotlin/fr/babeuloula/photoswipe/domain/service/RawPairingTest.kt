package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RawPairingTest {
    @Test
    fun `turns a raw and jpeg pair into the jpeg carrying its raw`() {
        val jpeg = file(id = 1, "IMG_0042.JPG")
        val raw = file(id = 2, "IMG_0042.DNG")

        val paired = RawPairing.pair(listOf(jpeg, raw))

        assertEquals(listOf(jpeg.copy(rawCompanion = raw)), paired)
    }

    @Test
    fun `pairs whatever the order and the case of the extensions`() {
        val raw = file(id = 1, "dsc_1000.nef")
        val heic = file(id = 2, "DSC_1000.heic")

        val paired = RawPairing.pair(listOf(raw, heic))

        assertEquals(listOf(2L), paired.map { it.id.value })
        assertEquals(raw, paired.single().rawCompanion)
    }

    @Test
    fun `keeps a raw without jpeg as a photo of its own`() {
        val raw = file(id = 1, "IMG_0042.CR3")
        val other = file(id = 2, "IMG_0043.JPG")

        assertEquals(listOf(raw, other), RawPairing.pair(listOf(raw, other)))
    }

    @Test
    fun `does not pair files of the same name in different folders`() {
        val jpeg = file(id = 1, "IMG_0042.JPG", folder = "DCIM/Camera/")
        val raw = file(id = 2, "IMG_0042.DNG", folder = "Pictures/Raw/")

        assertEquals(listOf(jpeg, raw), RawPairing.pair(listOf(jpeg, raw)))
    }

    @Test
    fun `leaves media without file name alone`() {
        val media = listOf(aMediaItem(id = 1), aMediaItem(id = 2))

        assertEquals(media, RawPairing.pair(media))
    }

    @Test
    fun `never pairs a video with a raw`() {
        val video = file(id = 1, "IMG_0042.jpg", type = MediaType.VIDEO)
        val raw = file(id = 2, "IMG_0042.dng")

        val paired = RawPairing.pair(listOf(video, raw))

        assertEquals(listOf(video, raw), paired)
        assertNull(paired.first().rawCompanion)
    }

    @Test
    fun `adds the size of the raw to the size of the pair`() {
        val pair = RawPairing.pair(listOf(file(id = 1, "a.jpg", size = 3_000), file(id = 2, "a.arw", size = 25_000)))

        assertEquals(28_000, pair.single().totalSizeBytes)
    }

    @Test
    fun `recognizes raw files by their extension`() {
        assertTrue(RawPairing.isRaw(file(id = 1, "photo.RAF")))
        assertFalse(RawPairing.isRaw(file(id = 1, "photo.jpg")))
    }

    private fun file(
        id: Long,
        name: String,
        folder: String = "DCIM/Camera/",
        type: MediaType = MediaType.IMAGE,
        size: Long = 1_000,
    ): MediaItem = aMediaItem(id = id, type = type, sizeBytes = size).copy(fileName = name, folder = folder)
}
