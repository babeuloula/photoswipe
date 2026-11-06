package fr.babeuloula.photoswipe.data.seed

import java.nio.ByteBuffer
import java.time.Instant

/**
 * Date de création d'un fichier MP4, écrite dans sa boîte `moov/mvhd`.
 *
 * MediaStore date une vidéo par ses métadonnées (la date de création du `mvhd`), pas par la valeur
 * `DATE_TAKEN` passée à l'insertion, qu'il recalcule en indexant le fichier. Le seeder réécrit donc
 * cette date dans une copie de la vidéo d'exemple. Les dates MP4 comptent les secondes depuis le
 * 1er janvier 1904, en UTC, sur 32 bits (version 0 du `mvhd`) ou 64 bits (version 1).
 */
object Mp4CreationTime {
    /** Secondes entre le 1er janvier 1904 et le 1er janvier 1970. */
    private const val SECONDS_FROM_1904_TO_1970 = 2_082_844_800L
    private const val HEADER_BYTES = 8
    private const val LARGE_SIZE_BYTES = 8
    private const val VERSION_AND_FLAGS_BYTES = 4

    /** Copie de [mp4] dont la création et la modification sont datées de [at], à la seconde. */
    fun stamp(
        mp4: ByteArray,
        at: Instant,
    ): ByteArray {
        val copy = mp4.copyOf()
        val buffer = ByteBuffer.wrap(copy)
        val moov = requireNotNull(findBox(buffer, 0, copy.size, "moov")) { "no moov box" }
        val mvhd = requireNotNull(findBox(buffer, moov.contentStart, moov.end, "mvhd")) { "no mvhd box" }
        val seconds = at.epochSecond + SECONDS_FROM_1904_TO_1970
        val times = mvhd.contentStart + VERSION_AND_FLAGS_BYTES
        if (buffer.get(mvhd.contentStart).toInt() == 1) {
            buffer.putLong(times, seconds)
            buffer.putLong(times + Long.SIZE_BYTES, seconds)
        } else {
            val unsigned = seconds.toUInt().toInt()
            buffer.putInt(times, unsigned)
            buffer.putInt(times + Int.SIZE_BYTES, unsigned)
        }
        return copy
    }

    /** Date de création lue dans le `mvhd` de [mp4], à la seconde. */
    fun read(mp4: ByteArray): Instant {
        val buffer = ByteBuffer.wrap(mp4)
        val moov = requireNotNull(findBox(buffer, 0, mp4.size, "moov")) { "no moov box" }
        val mvhd = requireNotNull(findBox(buffer, moov.contentStart, moov.end, "mvhd")) { "no mvhd box" }
        val times = mvhd.contentStart + VERSION_AND_FLAGS_BYTES
        val seconds =
            if (buffer.get(mvhd.contentStart).toInt() == 1) {
                buffer.getLong(times)
            } else {
                buffer.getInt(times).toUInt().toLong()
            }
        return Instant.ofEpochSecond(seconds - SECONDS_FROM_1904_TO_1970)
    }

    /** Boîte : début de son contenu (après l'en-tête) et fin. */
    private class Box(
        val contentStart: Int,
        val end: Int,
    )

    /** Première boîte de type [type] entre [start] et [end], au même niveau. */
    private fun findBox(
        buffer: ByteBuffer,
        start: Int,
        end: Int,
        type: String,
    ): Box? {
        var position = start
        while (position + HEADER_BYTES <= end) {
            val size = buffer.getInt(position).toUInt().toLong()
            val name =
                String(ByteArray(Int.SIZE_BYTES) { buffer.get(position + Int.SIZE_BYTES + it) }, Charsets.US_ASCII)
            val (headerBytes, boxSize) =
                when (size) {
                    0L -> HEADER_BYTES to (end - position).toLong()
                    1L -> HEADER_BYTES + LARGE_SIZE_BYTES to buffer.getLong(position + HEADER_BYTES)
                    else -> HEADER_BYTES to size
                }
            if (boxSize < headerBytes || position + boxSize > end) return null
            val boxEnd = (position + boxSize).toInt()
            if (name == type) return Box(position + headerBytes, boxEnd)
            position = boxEnd
        }
        return null
    }
}
