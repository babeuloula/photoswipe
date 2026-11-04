package fr.babeuloula.photoswipe.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant

class MediaItemTest {
    @Test
    fun `media ids with the same value are equal`() {
        assertEquals(MediaId(42), MediaId(42))
        assertNotEquals(MediaId(42), MediaId(43))
    }

    @Test
    fun `accepts an image without duration`() {
        assertDoesNotThrow { anImage() }
    }

    @Test
    fun `accepts a video with or without a known duration`() {
        assertDoesNotThrow { aVideo(duration = Duration.ofSeconds(12)) }
        assertDoesNotThrow { aVideo(duration = null) }
    }

    @Test
    fun `accepts an empty file`() {
        assertDoesNotThrow { anImage(sizeBytes = 0) }
    }

    @Test
    fun `rejects a negative size`() {
        assertThrows<IllegalArgumentException> { anImage(sizeBytes = -1) }
    }

    @Test
    fun `rejects an image with a duration`() {
        assertThrows<IllegalArgumentException> {
            anImage(duration = Duration.ofSeconds(1))
        }
    }

    @Test
    fun `rejects a video with a negative duration`() {
        assertThrows<IllegalArgumentException> {
            aVideo(duration = Duration.ofSeconds(-1))
        }
    }

    @Test
    fun `items with the same values are equal`() {
        assertEquals(anImage(), anImage())
    }

    private fun anImage(
        sizeBytes: Long = 1_024,
        duration: Duration? = null,
    ) = MediaItem(
        id = MediaId(1),
        uri = "content://media/external/images/media/1",
        type = MediaType.IMAGE,
        takenAt = Instant.parse("2024-11-04T10:15:30Z"),
        sizeBytes = sizeBytes,
        duration = duration,
    )

    private fun aVideo(duration: Duration?) =
        MediaItem(
            id = MediaId(2),
            uri = "content://media/external/video/media/2",
            type = MediaType.VIDEO,
            takenAt = Instant.parse("2024-11-04T10:15:30Z"),
            sizeBytes = 2_048,
            duration = duration,
        )
}
