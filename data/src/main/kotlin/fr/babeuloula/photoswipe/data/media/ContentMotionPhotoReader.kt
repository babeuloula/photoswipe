package fr.babeuloula.photoswipe.data.media

import android.content.ContentResolver
import android.net.Uri
import fr.babeuloula.photoswipe.data.di.IoDispatcher
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MotionPhoto
import fr.babeuloula.photoswipe.domain.port.MotionPhotoReader
import fr.babeuloula.photoswipe.domain.service.MotionPhotoDetector
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [MotionPhotoReader] sur le fichier de la photo, ouvert par le [ContentResolver] hors du thread
 * principal (PS-167) : [MotionPhotoDetector] n'en lit que le début, la fin et l'en-tête de la
 * vidéo. Le résultat est gardé en mémoire pour la durée du process, par média et par taille : une
 * photo retouchée (autre taille) est relue. Un fichier illisible n'est pas une photo animée.
 */
@Singleton
class ContentMotionPhotoReader
    @Inject
    constructor(
        private val contentResolver: ContentResolver,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : MotionPhotoReader {
        private val detector = MotionPhotoDetector()

        /** Ce qui a déjà été lu, photo ordinaire comprise. */
        private val known = ConcurrentHashMap<Pair<MediaId, Long>, Detected>()

        override suspend fun motionPhoto(media: MediaItem): MotionPhoto? {
            val key = media.id to media.sizeBytes
            known[key]?.let { return it.motionPhoto }
            val motionPhoto = withContext(ioDispatcher) { read(media) }
            known[key] = Detected(motionPhoto)
            return motionPhoto
        }

        private fun read(media: MediaItem): MotionPhoto? =
            try {
                contentResolver.openFileDescriptor(Uri.parse(media.uri), "r")?.use { descriptor ->
                    FileInputStream(descriptor.fileDescriptor).channel.use { channel ->
                        detector.detect(channel.size()) { position, length -> channel.readAt(position, length) }
                    }
                }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }

        /** Résultat gardé d'une lecture, photo animée ou non. */
        private data class Detected(
            val motionPhoto: MotionPhoto?,
        )
    }

/** Jusqu'à [length] octets à partir de [position], moins à la fin du fichier. */
private fun FileChannel.readAt(
    position: Long,
    length: Int,
): ByteArray {
    if (position < 0 || position >= size() || length <= 0) return ByteArray(0)
    val buffer = ByteBuffer.allocate(minOf(length.toLong(), size() - position).toInt())
    var at = position
    while (buffer.hasRemaining()) {
        val read = read(buffer, at)
        if (read < 0) break
        at += read
    }
    return buffer.array().copyOf(buffer.position())
}
