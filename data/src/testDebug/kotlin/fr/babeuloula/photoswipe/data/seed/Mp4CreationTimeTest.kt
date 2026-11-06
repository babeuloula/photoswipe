package fr.babeuloula.photoswipe.data.seed

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.time.Instant

class Mp4CreationTimeTest {
    private val sampleVideo = File("src/debug/res/raw/seed_video.mp4").readBytes()
    private val takenAt = Instant.parse("2021-11-06T18:30:00Z")

    @Test
    fun `writes the creation date into the sample video`() {
        val stamped = Mp4CreationTime.stamp(sampleVideo, takenAt)

        assertEquals(takenAt, Mp4CreationTime.read(stamped))
        assertEquals(sampleVideo.size, stamped.size)
    }

    @Test
    fun `leaves the original video untouched`() {
        val original = sampleVideo.copyOf()

        Mp4CreationTime.stamp(sampleVideo, takenAt)

        assertArrayEquals(original, sampleVideo)
    }

    @Test
    fun `writes 64-bit dates into a version 1 movie header`() {
        val stamped = Mp4CreationTime.stamp(mp4(mvhdVersion = 1), takenAt)

        assertEquals(takenAt, Mp4CreationTime.read(stamped))
    }

    @Test
    fun `writes 32-bit dates into a version 0 movie header`() {
        val stamped = Mp4CreationTime.stamp(mp4(mvhdVersion = 0), takenAt)

        assertEquals(takenAt, Mp4CreationTime.read(stamped))
    }

    @Test
    fun `rejects a file without movie header`() {
        assertThrows(IllegalArgumentException::class.java) {
            Mp4CreationTime.stamp(box("ftyp", ByteArray(8)), takenAt)
        }
    }

    /** MP4 minimal : `ftyp`, puis `moov` contenant un autre enfant et le `mvhd`. */
    private fun mp4(mvhdVersion: Int): ByteArray {
        val timesBytes = if (mvhdVersion == 1) 16 else 8
        val mvhd = box("mvhd", byteArrayOf(mvhdVersion.toByte(), 0, 0, 0) + ByteArray(timesBytes + 88))
        return box("ftyp", ByteArray(8)) + box("moov", box("udta", ByteArray(4)) + mvhd)
    }

    private fun box(
        type: String,
        content: ByteArray,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).apply {
            writeInt(8 + content.size)
            writeBytes(type)
            write(content)
        }
        return output.toByteArray()
    }
}
