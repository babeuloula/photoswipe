package fr.babeuloula.photoswipe.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Filet de sécurité de l'i18n (SPEC §9), en plus de `MissingTranslation` d'Android Lint :
 * chaque clé existe en anglais et en français, et chaque langue traduite est proposée
 * dans le réglage de langue par application.
 *
 * Les chemins sont relatifs au module `:app`, dossier de travail des tests unitaires Gradle.
 */
class LocalizationTest {
    private val resources = TranslatableResources(File(RES_DIR))

    @Test
    fun `translates every English key into French`() {
        assertEquals(emptySet<String>(), resources.missingIn("fr"))
    }

    @Test
    fun `has no French key missing from English`() {
        assertEquals(emptySet<String>(), resources.orphansIn("fr"))
    }

    @Test
    fun `offers English and every translated language in the per-app language setting`() {
        assertEquals(setOf("en") + resources.translatedLocales(), localesInConfig())
        assertTrue("fr" in resources.translatedLocales())
    }

    @Test
    fun `declares the locale config in the manifest`() {
        val application = parse(File(MANIFEST)).getElementsByTagName("application").item(0)
        val localeConfig = application.attributes.getNamedItem("android:localeConfig")?.nodeValue

        assertEquals("@xml/locales_config", localeConfig)
    }

    private fun localesInConfig(): Set<String> {
        val locales = parse(File(RES_DIR, "xml/locales_config.xml")).getElementsByTagName("locale")
        return (0 until locales.length)
            .map {
                locales
                    .item(it)
                    .attributes
                    .getNamedItem("android:name")
                    .nodeValue
            }.toSet()
    }

    private fun parse(file: File) = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

    private companion object {
        const val RES_DIR = "src/main/res"
        const val MANIFEST = "src/main/AndroidManifest.xml"
    }
}
