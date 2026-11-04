package fr.babeuloula.photoswipe.i18n

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Lit les clés traduisibles d'un dossier de ressources Android (`res/`), sans passer par Android.
 *
 * Une clé est notée `type/nom` (par exemple `string/home_placeholder` ou `plurals/media_count`) ;
 * les ressources marquées `translatable="false"` sont ignorées, comme le fait Android Lint.
 */
class TranslatableResources(
    private val resDir: File,
) {
    /** Clés traduisibles déclarées dans `values/` (anglais, langue par défaut). */
    fun defaultKeys(): Set<String> = keysIn("values")

    /** Clés déclarées dans `values-<locale>/`. */
    fun keysFor(locale: String): Set<String> = keysIn("values-$locale")

    /** Clés présentes en anglais mais absentes de la traduction `locale`. */
    fun missingIn(locale: String): Set<String> = defaultKeys() - keysFor(locale)

    /** Clés traduites dans `locale` qui n'existent plus en anglais. */
    fun orphansIn(locale: String): Set<String> = keysFor(locale) - defaultKeys()

    /** Langues traduites : les dossiers `values-xx` qui contiennent au moins une clé traduisible. */
    fun translatedLocales(): Set<String> =
        resDir
            .listFiles { file -> file.isDirectory && file.name.startsWith("values-") }
            .orEmpty()
            .map { it.name.removePrefix("values-") }
            .filter { keysFor(it).isNotEmpty() }
            .toSet()

    private fun keysIn(folder: String): Set<String> =
        File(resDir, folder)
            .listFiles { file -> file.extension == "xml" }
            .orEmpty()
            .flatMap(::keysInFile)
            .toSet()

    private fun keysInFile(file: File): List<String> {
        val root =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(file)
                .documentElement
        return root
            .childElements()
            .filter { it.tagName in TRANSLATABLE_TYPES && it.getAttribute("translatable") != "false" }
            .map { "${it.tagName}/${it.getAttribute("name")}" }
    }

    private fun Element.childElements(): List<Element> =
        (0 until childNodes.length).map(childNodes::item).filterIsInstance<Element>()

    private companion object {
        val TRANSLATABLE_TYPES = setOf("string", "plurals", "string-array")
    }
}
