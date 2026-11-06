package fr.babeuloula.photoswipe.buildlogic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppVersionTest {
    @Test
    fun `takes the version of the tag on the tagged commit`() {
        assertEquals("1.2.0", AppVersion.nameFrom("v1.2.0-0-g1a2b3c4\n"))
    }

    @Test
    fun `marks a development version after the tag`() {
        assertEquals("1.2.0-dev.5", AppVersion.nameFrom("v1.2.0-5-g1a2b3c4"))
    }

    @Test
    fun `falls back to a development version without tag or git`() {
        assertEquals(AppVersion.DEVELOPMENT, AppVersion.nameFrom(null))
        assertEquals(AppVersion.DEVELOPMENT, AppVersion.nameFrom(""))
        assertEquals(AppVersion.DEVELOPMENT, AppVersion.nameFrom("release-candidate-3-gabc"))
    }
}
