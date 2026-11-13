package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.fixtures.aCheckerboard
import fr.babeuloula.photoswipe.domain.model.GrayImage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PhotoFingerprintTest {
    /** Dégradé horizontal de [width] × [height], du clair au sombre, éclairci de [shift]. */
    private fun gradient(
        width: Int,
        height: Int,
        shift: Int = 0,
    ) = GrayImage(
        width,
        height,
        IntArray(width * height) { i -> (250 - (i % width) * 200 / width + shift).coerceIn(0, 255) },
    )

    @Test
    fun `gives the same fingerprint to a copy scaled down and slightly brighter`() {
        val original = PhotoFingerprint.of(gradient(256, 192))
        val copy = PhotoFingerprint.of(gradient(64, 48, shift = 5))

        assertTrue(PhotoFingerprint.sameImage(original, copy))
    }

    @Test
    fun `tells apart two different images`() {
        val darkening = PhotoFingerprint.of(gradient(64, 64))
        val brightening = PhotoFingerprint.of(GrayImage(64, 64, IntArray(64 * 64) { i -> 50 + (i % 64) * 3 }))

        assertFalse(PhotoFingerprint.sameImage(darkening, brightening))
        assertTrue(
            PhotoFingerprint.distance(darkening, PhotoFingerprint.of(aCheckerboard(64))) >
                PhotoFingerprint.DUPLICATE_DISTANCE,
        )
    }

    @Test
    fun `counts the differing bits`() {
        assertEquals(0, PhotoFingerprint.distance(0b1011, 0b1011))
        assertEquals(2, PhotoFingerprint.distance(0b1011, 0b0001))
    }
}
