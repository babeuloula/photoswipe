package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType

/**
 * Paires RAW + JPEG (PS-166) : un appareil réglé ainsi enregistre deux fichiers pour une même
 * photo, de même nom et dans le même dossier, avec des extensions différentes. La paire devient une
 * seule photo, le JPEG, qui porte son RAW en compagnon ([MediaItem.rawCompanion]) ; le RAW seul
 * disparaît de la liste. Un RAW sans JPEG reste une photo comme une autre.
 */
object RawPairing {
    /** Extensions des fichiers RAW reconnus. */
    private val RAW_EXTENSIONS = setOf("dng", "cr2", "cr3", "nef", "nrw", "arw", "raf", "orf", "rw2", "pef", "srw")

    /** Extensions des photos qui peuvent accompagner un RAW. */
    private val PREVIEW_EXTENSIONS = setOf("jpg", "jpeg", "heic", "heif")

    /** [media], chaque paire RAW + JPEG réduite à son JPEG, dans l'ordre d'origine. */
    fun pair(media: List<MediaItem>): List<MediaItem> {
        val raws =
            media
                .filter { it.type == MediaType.IMAGE && it.extension() in RAW_EXTENSIONS }
                .groupBy { it.pairKey() }
                .filterKeys { it != null }
        if (raws.isEmpty()) return media
        val paired = HashSet<MediaItem>()
        val result =
            media.map { item ->
                val raw =
                    raws[item.pairKey()]
                        ?.firstOrNull()
                        ?.takeIf { item.type == MediaType.IMAGE && item.extension() in PREVIEW_EXTENSIONS }
                if (raw == null) {
                    item
                } else {
                    paired += raw
                    item.copy(rawCompanion = raw)
                }
            }
        return result.filterNot { it in paired }
    }

    /** Vrai si [media] est un fichier RAW. */
    fun isRaw(media: MediaItem): Boolean = media.extension() in RAW_EXTENSIONS

    private fun MediaItem.extension(): String? = fileName?.substringAfterLast('.', "")?.lowercase()?.ifEmpty { null }

    /** Dossier et nom sans extension : ce que partagent les deux fichiers d'une paire. */
    private fun MediaItem.pairKey(): String? {
        val name = fileName ?: return null
        return "${folder.orEmpty()}/${name.substringBeforeLast('.').lowercase()}"
    }
}
