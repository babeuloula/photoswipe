package fr.babeuloula.photoswipe.i18n

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Vérifie le lecteur de ressources sur des fixtures, pour que LocalizationTest ne passe pas à tort. */
class TranslatableResourcesTest {
    private val fixture = TranslatableResources(fixtureDir("incomplete"))

    @Test
    fun `reads strings, plurals and string arrays but skips non translatable ones`() {
        assertEquals(
            setOf("string/greeting", "string/farewell", "plurals/photo_count", "string-array/days"),
            fixture.defaultKeys(),
        )
    }

    @Test
    fun `reports keys missing from a translation`() {
        assertEquals(setOf("string/farewell", "string-array/days"), fixture.missingIn("fr"))
    }

    @Test
    fun `reports translated keys that no longer exist in English`() {
        assertEquals(setOf("string/obsolete"), fixture.orphansIn("fr"))
    }

    @Test
    fun `lists only values folders that hold translations`() {
        assertEquals(setOf("fr"), fixture.translatedLocales())
    }

    private fun fixtureDir(name: String): File =
        File(requireNotNull(javaClass.getResource("/fixtures/i18n/$name/res")).toURI())
}
