package fr.babeuloula.photoswipe.ui.deck

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import fr.babeuloula.photoswipe.domain.model.MotionPhoto

/**
 * Vidéo d'une photo animée (PS-167, ADR 0040) : octets [start] à [start] + [length] du fichier
 * [uri]. Le lecteur de la pile la reçoit sous la forme d'une URI à part ([toUri]), que
 * [MotionPhotoDataSource] ramène à cette plage du fichier.
 *
 * @property uri URI `content://` de la photo.
 * @property start position du premier octet de la vidéo.
 * @property length nombre d'octets de la vidéo.
 */
internal data class MotionVideoRange(
    val uri: String,
    val start: Long,
    val length: Long,
) {
    /** URI de la vidéo : celle de la photo, suivie d'un fragment qui donne la plage. */
    fun toUri(): String = "$uri#$FRAGMENT_PREFIX$start-$length"

    companion object {
        /** Préfixe du fragment, que MediaStore n'emploie pas. */
        private const val FRAGMENT_PREFIX = "photoswipe-motion="

        /** Plage décrite par [motionUri], `null` si ce n'est pas l'URI de la vidéo d'une photo animée. */
        fun of(motionUri: String): MotionVideoRange? {
            val fragment = motionUri.substringAfter('#', "")
            if (!fragment.startsWith(FRAGMENT_PREFIX)) return null
            val bounds = fragment.removePrefix(FRAGMENT_PREFIX).split('-').mapNotNull { it.toLongOrNull() }
            return bounds
                .takeIf { it.size == 2 && it[0] >= 0 && it[1] > 0 }
                ?.let { (start, length) -> MotionVideoRange(motionUri.substringBefore('#'), start, length) }
        }
    }
}

/** URI de la vidéo de la photo animée [motion], rangée dans le fichier [mediaUri]. */
internal fun motionVideoUri(
    mediaUri: String,
    motion: MotionPhoto,
): String = MotionVideoRange(mediaUri, motion.videoStart, motion.videoLength).toUri()

/**
 * Source de Media3 qui lit la vidéo d'une photo animée comme un fichier MP4 à part : une URI de
 * [MotionVideoRange] devient la plage d'octets de la vidéo dans le fichier de la photo ; toute
 * autre URI passe telle quelle à [upstream].
 */
@OptIn(UnstableApi::class)
internal class MotionPhotoDataSource(
    private val upstream: DataSource,
) : DataSource {
    override fun addTransferListener(transferListener: TransferListener) =
        upstream.addTransferListener(transferListener)

    override fun open(dataSpec: DataSpec): Long {
        val range = MotionVideoRange.of(dataSpec.uri.toString()) ?: return upstream.open(dataSpec)
        val available = (range.length - dataSpec.position).coerceAtLeast(0)
        val length = if (dataSpec.length == C.LENGTH_UNSET.toLong()) available else minOf(dataSpec.length, available)
        return upstream.open(
            dataSpec
                .buildUpon()
                .setUri(Uri.parse(range.uri))
                .setPosition(range.start + dataSpec.position)
                .setLength(length)
                .build(),
        )
    }

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int = upstream.read(buffer, offset, length)

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() = upstream.close()

    /** Fabrique pour le lecteur de la pile, autour de [upstream]. */
    class Factory(
        private val upstream: DataSource.Factory,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = MotionPhotoDataSource(upstream.createDataSource())
    }
}
