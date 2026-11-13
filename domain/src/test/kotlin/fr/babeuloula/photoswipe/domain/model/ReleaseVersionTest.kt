package fr.babeuloula.photoswipe.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReleaseVersionTest {
    @Test
    fun `reads a published version`() {
        assertEquals(ReleaseVersion(1, 4, 0), ReleaseVersion.parse("1.4.0"))
    }

    @Test
    fun `reads a development build as the last published version it contains`() {
        assertEquals(ReleaseVersion(1, 3, 0), ReleaseVersion.parse("1.3.0-dev.9"))
        assertEquals(ReleaseVersion(0, 0, 0), ReleaseVersion.parse("0.0.0-dev"))
    }

    @Test
    fun `refuses a name that is not a version`() {
        assertNull(ReleaseVersion.parse(""))
        assertNull(ReleaseVersion.parse("1.4"))
        assertNull(ReleaseVersion.parse("v1.4.0"))
        assertNull(ReleaseVersion.parse("1.4.x"))
        assertNull(ReleaseVersion.parse("99999999999.0.0"))
    }

    @Test
    fun `compares each number in turn, not as text`() {
        assertTrue(ReleaseVersion(1, 10, 0) > ReleaseVersion(1, 9, 0))
        assertTrue(ReleaseVersion(2, 0, 0) > ReleaseVersion(1, 99, 99))
        assertTrue(ReleaseVersion(1, 4, 1) > ReleaseVersion(1, 4, 0))
        assertEquals(0, ReleaseVersion(1, 4, 0).compareTo(ReleaseVersion(1, 4, 0)))
    }

    @Test
    fun `writes the version as it is stored`() {
        assertEquals("1.4.0", ReleaseVersion(1, 4, 0).toString())
    }
}
