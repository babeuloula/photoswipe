package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.fixtures.aCheckerboard
import fr.babeuloula.photoswipe.domain.fixtures.aFlatImage
import fr.babeuloula.photoswipe.domain.model.GrayImage
import fr.babeuloula.photoswipe.domain.model.QualityHint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PhotoQualityRulesTest {
    private val rules = PhotoQualityRules()

    @Test
    fun `measures no detail and the brightness of a flat image`() {
        val measure = rules.measure(aFlatImage(128))

        assertEquals(0.0, measure.sharpness)
        assertEquals(128.0, measure.brightness)
    }

    @Test
    fun `measures a lot of detail in a sharp checkerboard`() {
        assertTrue(rules.measure(aCheckerboard()).sharpness > 10_000)
    }

    @Test
    fun `measures a soft gradient as having little detail`() {
        val gradient = GrayImage(32, 32, IntArray(32 * 32) { i -> 60 + (i % 32) * 4 })

        assertTrue(rules.measure(gradient).sharpness < PhotoQualityRules.DEFAULT_BLUR_THRESHOLD)
    }

    @Test
    fun `says a featureless bright image may be blurry, a near black one very dark, a sharp one nothing`() {
        assertEquals(QualityHint.BLURRY, rules.hint(rules.measure(aFlatImage(150))))
        assertEquals(QualityHint.DARK, rules.hint(rules.measure(aFlatImage(10))))
        assertNull(rules.hint(rules.measure(aCheckerboard())))
    }

    @Test
    fun `does not measure detail in an image too small for it`() {
        assertEquals(0.0, rules.measure(aFlatImage(200, width = 2, height = 2)).sharpness)
    }

    @Test
    fun `refuses an image whose pixels do not match its size`() {
        assertThrows<IllegalArgumentException> { GrayImage(4, 4, IntArray(3)) }
    }
}
