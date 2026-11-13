package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.fixtures.aJpeg
import fr.babeuloula.photoswipe.domain.fixtures.aMicroVideoXmp
import fr.babeuloula.photoswipe.domain.fixtures.aMotionPhotoXmp
import fr.babeuloula.photoswipe.domain.fixtures.aMp4
import fr.babeuloula.photoswipe.domain.fixtures.aSamsungTrailer
import fr.babeuloula.photoswipe.domain.model.MotionPhoto
import fr.babeuloula.photoswipe.domain.model.MotionPhotoFormat
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MotionPhotoDetectorTest {
    private val detector = MotionPhotoDetector()
    private val video = aMp4()

    private fun detect(file: ByteArray): MotionPhoto? =
        detector.detect(file.size.toLong()) { position, length ->
            val from = position.coerceIn(0, file.size.toLong()).toInt()
            file.copyOfRange(from, minOf(file.size, from + length))
        }

    private fun ByteArray.video(motion: MotionPhoto): ByteArray =
        copyOfRange(motion.videoStart.toInt(), (motion.videoStart + motion.videoLength).toInt())

    @Test
    fun `finds the video of a Google micro video from the offset of its XMP`() {
        val file = aJpeg(aMicroVideoXmp(offset = video.size), trailer = video)

        val motion = detect(file)!!

        assertEquals(MotionPhotoFormat.GOOGLE_XMP, motion.format)
        assertArrayEquals(video, file.video(motion))
    }

    @Test
    fun `finds the video of a Google motion photo from its container`() {
        val file = aJpeg(aMotionPhotoXmp(videoLength = video.size), trailer = video)

        val motion = detect(file)!!

        assertEquals(MotionPhotoFormat.GOOGLE_XMP, motion.format)
        assertArrayEquals(video, file.video(motion))
    }

    @Test
    fun `finds the video of a Samsung motion photo in its trailer`() {
        val file = aJpeg(trailer = aSamsungTrailer(video))

        val motion = detect(file)!!

        assertEquals(MotionPhotoFormat.SAMSUNG_TRAILER, motion.format)
        assertArrayEquals(video, file.video(motion))
    }

    @Test
    fun `an ordinary JPEG is not a motion photo`() {
        assertNull(detect(aJpeg(trailer = ByteArray(200))))
    }

    @Test
    fun `a motion photo flag set to 0 is not a motion photo`() {
        assertNull(detect(aJpeg(aMicroVideoXmp(offset = video.size, flag = "0"), trailer = video)))
    }

    @Test
    fun `an announced video that is not an MP4 leaves an ordinary photo`() {
        val notAVideo = ByteArray(video.size) { 0x33 }

        assertNull(detect(aJpeg(aMicroVideoXmp(offset = notAVideo.size), trailer = notAVideo)))
        assertNull(detect(aJpeg(trailer = aSamsungTrailer(notAVideo))))
    }

    @Test
    fun `an offset beyond the file leaves an ordinary photo`() {
        assertNull(detect(aJpeg(aMicroVideoXmp(offset = 1_000_000), trailer = video)))
    }

    @Test
    fun `a file that is not a JPEG is not a motion photo, whatever its trailer`() {
        val file = byteArrayOf(0x89.toByte(), 0x50) + ByteArray(100) + aSamsungTrailer(video)

        assertNull(detect(file))
    }

    @Test
    fun `a truncated Samsung trailer leaves an ordinary photo`() {
        val trailer = aSamsungTrailer(video)

        assertNull(detect(aJpeg(trailer = trailer.copyOfRange(0, trailer.size - 20) + trailer.takeLast(8))))
    }
}
