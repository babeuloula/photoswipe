package fr.babeuloula.photoswipe.ui.deck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeDetectorTest {
    private val detector = ShakeDetector()

    /** Mesure de [g] fois la gravité sur un seul axe, prise à [millis] ms. */
    private fun measure(
        g: Float,
        millis: Long,
    ): Boolean = detector.onAcceleration(0f, g * ShakeDetector.STANDARD_GRAVITY, 0f, millis * NANOS_PER_MILLI)

    /** Secousse franche : trois mesures à 3 g, à 20 ms d'écart, à partir de [startMillis]. */
    private fun shake(startMillis: Long): List<Boolean> = List(3) { measure(SHAKE_G, startMillis + it * 20L) }

    @Test
    fun `ignores a phone at rest or carried while walking`() {
        val readings = (0 until 100).map { measure(if (it % 2 == 0) 1f else 1.8f, it * 20L) }

        assertTrue(readings.none { it })
    }

    @Test
    fun `ignores a single bump, like a phone put down a bit hard`() {
        assertFalse(measure(4f, 0))
        assertFalse(measure(1f, 20))
        assertFalse(measure(1f, 40))
    }

    @Test
    fun `recognizes a shake on its third reading above 2_7 g within half a second`() {
        assertEquals(listOf(false, false, true), shake(startMillis = 0))
    }

    @Test
    fun `counts the acceleration on every axis`() {
        // 2 g sur deux axes : 2,8 g en tout, au-delà du seuil, alors qu'aucun axe ne l'atteint seul.
        val axis = 2f * ShakeDetector.STANDARD_GRAVITY
        val readings = List(3) { detector.onAcceleration(axis, axis, 0f, it * 20L * NANOS_PER_MILLI) }

        assertEquals(listOf(false, false, true), readings)
    }

    @Test
    fun `ignores readings just under the threshold`() {
        val readings = List(10) { measure(2.6f, it * 20L) }

        assertTrue(readings.none { it })
    }

    @Test
    fun `ignores strong readings spread over more than half a second`() {
        val readings =
            listOf(measure(SHAKE_G, 0), measure(SHAKE_G, 400), measure(SHAKE_G, 800), measure(SHAKE_G, 1_200))

        assertTrue(readings.none { it })
    }

    @Test
    fun `undoes once per shake, waiting a moment before the next one`() {
        assertTrue(shake(startMillis = 0).last())

        // On secoue encore dans la foulée : rien pendant une seconde et demie.
        assertTrue(shake(startMillis = 100).none { it })
        assertTrue(shake(startMillis = 1_400).none { it })

        assertTrue(shake(startMillis = 1_600).last())
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L

        /** Accélération d'une secousse franche, en multiples de la gravité. */
        const val SHAKE_G = 3f
    }
}
