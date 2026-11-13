package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.model.MotionPhoto
import fr.babeuloula.photoswipe.domain.model.MotionPhotoFormat

/** Octets d'un fichier, lus à la demande : moins que [length] octets à la fin du fichier. */
fun interface FileBytes {
    /** Jusqu'à [length] octets à partir de [position]. */
    fun read(
        position: Long,
        length: Int,
    ): ByteArray
}

/**
 * Reconnaît une photo animée à ses métadonnées (PS-167, ADR 0040), sans se fier au nom du
 * fichier : un JPEG dont le XMP de Google (`GCamera:MicroVideo`, `GCamera:MotionPhoto`,
 * `Camera:MotionPhoto`) ou le trailer `SEFT` de Samsung annonce une vidéo. Seuls le début et la
 * fin du fichier sont lus, plus l'en-tête de la vidéo annoncée : une vidéo qui ne commence pas
 * par une boîte MP4 `ftyp` n'est pas retenue, la photo reste une photo ordinaire.
 */
class MotionPhotoDetector {
    /** Vidéo de la photo animée de [size] octets lue par [bytes], `null` si ce n'en est pas une. */
    fun detect(
        size: Long,
        bytes: FileBytes,
    ): MotionPhoto? {
        if (size < MIN_SIZE) return null
        val head = bytes.read(0, minOf(size, HEAD_BYTES.toLong()).toInt())
        val jpeg = head.size >= JPEG_SOI.size && JPEG_SOI.indices.all { head[it] == JPEG_SOI[it] }
        return when {
            !jpeg -> {
                null
            }

            else -> {
                googleVideo(String(head, Charsets.ISO_8859_1), size)?.takeIf { isMp4(it, bytes) }
                    ?: samsungVideo(size, bytes)?.takeIf { isMp4(it, bytes) }
            }
        }
    }

    /** Vidéo annoncée par le XMP de Google, placée à la fin du fichier. */
    private fun googleVideo(
        xmp: String,
        size: Long,
    ): MotionPhoto? {
        if (!MOTION_FLAG.containsMatchIn(xmp)) return null
        val microVideo =
            MICRO_VIDEO_OFFSET
                .find(xmp)
                ?.groupValues
                ?.drop(1)
                ?.firstOrNull { it.isNotEmpty() }
        return microVideo?.toLongOrNull()?.let { motionPhoto(MotionPhotoFormat.GOOGLE_XMP, size - it, it, size) }
            ?: containerVideo(xmp, size)
    }

    /**
     * Format « Motion Photo » : après la photo, les éléments du conteneur se suivent dans l'ordre
     * du XMP, jusqu'à la fin du fichier ; la vidéo est celui dont le rôle est `MotionPhoto`.
     */
    private fun containerVideo(
        xmp: String,
        size: Long,
    ): MotionPhoto? {
        // Le premier élément est la photo elle-même, au début du fichier.
        val items =
            CONTAINER_ITEM
                .findAll(xmp)
                .map { attributes(it.value) }
                .drop(1)
                .toList()
        val lengths = items.mapNotNull { it["Length"]?.toLongOrNull() }
        val index = items.indexOfFirst { it["Semantic"] == "MotionPhoto" }
        if (index < 0 || lengths.size != items.size) return null
        val length = lengths[index]
        return motionPhoto(MotionPhotoFormat.GOOGLE_XMP, size - lengths.drop(index + 1).sum() - length, length, size)
    }

    /** Attributs `Item:Nom` ou `GContainerItem:Nom` d'un élément du conteneur, par nom. */
    private fun attributes(tag: String): Map<String, String> =
        ITEM_ATTRIBUTE.findAll(tag).associate { it.groupValues[1] to it.groupValues[2] }

    /**
     * Vidéo du bloc `MotionPhoto_Data` du trailer de Samsung : le fichier finit par la taille du
     * répertoire `SEFH` puis `SEFT` ; chaque entrée du répertoire donne la distance de son bloc
     * avant le répertoire et sa taille.
     */
    private fun samsungVideo(
        size: Long,
        bytes: FileBytes,
    ): MotionPhoto? {
        val end = bytes.read(size - TRAILER_END, TRAILER_END)
        val length = if (end.tag(TAG_SIZE) == "SEFT") end.le32(0) else 0L
        val start = size - TRAILER_END - length
        if (length !in DIRECTORY_HEADER..MAX_DIRECTORY || start <= 0) return null
        val directory = bytes.read(start, length.toInt())
        val entries = if (directory.tag(0) == "SEFH") minOf(directory.le32(TAG_SIZE * 2), MAX_ENTRIES) else 0L
        return (0 until entries.toInt()).firstNotNullOfOrNull { samsungEntry(directory, start, it, size, bytes) }
    }

    /**
     * Entrée [index] du répertoire [directory], qui commence à [directoryStart] : la vidéo si son
     * bloc s'appelle `MotionPhoto_Data`. Un bloc commence par 4 octets (dont son type), la
     * longueur de son nom puis son nom ; ses données suivent.
     */
    private fun samsungEntry(
        directory: ByteArray,
        directoryStart: Long,
        index: Int,
        size: Long,
        bytes: FileBytes,
    ): MotionPhoto? {
        val entry = DIRECTORY_HEADER.toInt() + index * ENTRY_SIZE
        val blockStart = directoryStart - directory.le32(entry + TAG_SIZE)
        val blockLength = directory.le32(entry + TAG_SIZE * 2)
        val block = if (blockStart > 0 && blockLength > 0) bytes.read(blockStart, BLOCK_HEAD) else ByteArray(0)
        val named =
            block.le32(TAG_SIZE) == SAMSUNG_VIDEO_NAME.length.toLong() &&
                block.tag(BLOCK_NAME, SAMSUNG_VIDEO_NAME.length) == SAMSUNG_VIDEO_NAME
        val header = BLOCK_NAME + SAMSUNG_VIDEO_NAME.length
        return if (named) {
            motionPhoto(
                MotionPhotoFormat.SAMSUNG_TRAILER,
                blockStart + header,
                blockLength - header,
                size,
            )
        } else {
            null
        }
    }

    /** Photo animée si la vidéo tient dans le fichier, après la photo. */
    private fun motionPhoto(
        format: MotionPhotoFormat,
        start: Long,
        length: Long,
        size: Long,
    ): MotionPhoto? =
        if (start > 0 && length > 0 &&
            start + length <= size
        ) {
            MotionPhoto(format, start, length)
        } else {
            null
        }

    /** Vrai si la vidéo commence par une boîte `ftyp`, comme tout fichier MP4. */
    private fun isMp4(
        motion: MotionPhoto,
        bytes: FileBytes,
    ): Boolean = bytes.read(motion.videoStart, TAG_SIZE * 2).tag(TAG_SIZE) == "ftyp"

    private companion object {
        /** Début du fichier lu pour trouver le XMP, rangé dans les premiers segments du JPEG. */
        const val HEAD_BYTES = 128 * 1024

        /** Plus petit fichier qui puisse contenir une photo et une vidéo. */
        const val MIN_SIZE = 64L

        /** Taille d'une étiquette (`SEFT`, `ftyp`…) ou d'un entier des en-têtes. */
        const val TAG_SIZE = 4

        /** Taille du répertoire puis `SEFT`, à la fin d'un fichier de Samsung. */
        const val TRAILER_END = 8

        /** `SEFH`, version et nombre d'entrées. */
        const val DIRECTORY_HEADER = 12L

        /** Taille d'une entrée : 4 octets dont son type, position et taille de son bloc. */
        const val ENTRY_SIZE = 12

        /** Répertoire plus grand : ce n'est pas un trailer de Samsung. */
        const val MAX_DIRECTORY = 64L * 1024

        /** Plus d'entrées : ce n'est pas un trailer de Samsung. */
        const val MAX_ENTRIES = 256L

        /** Position du nom dans un bloc, après son type et la longueur du nom. */
        const val BLOCK_NAME = 8

        /** Octets lus au début d'un bloc : de quoi lire son nom. */
        const val BLOCK_HEAD = 64

        /** Nom du bloc vidéo d'une photo animée de Samsung. */
        const val SAMSUNG_VIDEO_NAME = "MotionPhoto_Data"

        /** Marqueur de début d'image d'un JPEG. */
        val JPEG_SOI = byteArrayOf(0xFF.toByte(), 0xD8.toByte())

        /** `GCamera:MotionPhoto="1"` ou `<GCamera:MicroVideo>1</…>`, avec ou sans le préfixe `G`. */
        val MOTION_FLAG = Regex("""\bG?Camera:(?:MotionPhoto|MicroVideo)\s*(?:=\s*["']\s*1\s*["']|>\s*1\s*<)""")

        /** Taille de la vidéo du format « Micro Video », comptée depuis la fin du fichier. */
        val MICRO_VIDEO_OFFSET = Regex("""\bG?Camera:MicroVideoOffset\s*(?:=\s*["']\s*(\d+)\s*["']|>\s*(\d+)\s*<)""")

        /** Élément du conteneur du format « Motion Photo », reconnu à son attribut `Semantic`. */
        val CONTAINER_ITEM = Regex("""<[^<>]*\b(?:Item|GContainerItem):Semantic\s*=[^<>]*>""")

        /** Attribut d'un élément du conteneur. */
        val ITEM_ATTRIBUTE = Regex("""\b(?:Item|GContainerItem):(\w+)\s*=\s*["']([^"']*)["']""")
    }
}

/** [length] caractères à partir de [from], vide s'ils dépassent. */
private fun ByteArray.tag(
    from: Int,
    length: Int = 4,
): String = if (from < 0 || from + length > size) "" else String(this, from, length, Charsets.ISO_8859_1)

/** Entier de 32 bits non signé, petit-boutiste, à [index] ; -1 s'il dépasse. */
private fun ByteArray.le32(index: Int): Long =
    if (index < 0 || index + INT_BYTES > size) {
        -1L
    } else {
        (0 until INT_BYTES).fold(0L) { value, byte ->
            value or ((this[index + byte].toLong() and BYTE_MASK) shl (Byte.SIZE_BITS * byte))
        }
    }

private const val INT_BYTES = 4

private const val BYTE_MASK = 0xFFL
