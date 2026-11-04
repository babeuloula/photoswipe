package fr.babeuloula.photoswipe.i18n

import fr.babeuloula.photoswipe.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Vérifie que le pluriel d'exemple suit les règles de chaque langue (SPEC §9). */
@RunWith(RobolectricTestRunner::class)
class PluralsTest {
    @Test
    @Config(qualifiers = "en")
    fun `uses singular and plural forms in English`() {
        assertEquals("1 media item", mediaCount(1))
        assertEquals("3 media items", mediaCount(3))
    }

    @Test
    @Config(qualifiers = "fr")
    fun `treats zero and one as singular in French`() {
        assertEquals("0 média", mediaCount(0))
        assertEquals("1 média", mediaCount(1))
        assertEquals("3 médias", mediaCount(3))
    }

    private fun mediaCount(count: Int): String =
        RuntimeEnvironment
            .getApplication()
            .resources
            .getQuantityString(R.plurals.media_count, count, count)
}
