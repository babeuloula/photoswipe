package fr.babeuloula.photoswipe.data.quality

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.Size
import fr.babeuloula.photoswipe.data.di.IoDispatcher
import fr.babeuloula.photoswipe.domain.model.GrayImage
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.port.PhotoAnalyzer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

/**
 * [PhotoAnalyzer] sur les vignettes de MediaStore (PS-164) : une vignette d'au plus
 * [THUMBNAIL_SIZE] pixels, convertie en niveaux de gris, hors du thread principal. Rien n'est
 * gardé de l'image elle-même.
 */
class ThumbnailPhotoAnalyzer
    @Inject
    constructor(
        private val contentResolver: ContentResolver,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : PhotoAnalyzer {
        override suspend fun grayThumbnail(media: MediaItem): GrayImage? =
            withContext(ioDispatcher) {
                try {
                    val bitmap =
                        contentResolver.loadThumbnail(Uri.parse(media.uri), Size(THUMBNAIL_SIZE, THUMBNAIL_SIZE), null)
                    gray(bitmap).also { bitmap.recycle() }
                } catch (_: IOException) {
                    null
                } catch (_: SecurityException) {
                    null
                }
            }

        private companion object {
            /** Côté maximal de la vignette mesurée. */
            const val THUMBNAIL_SIZE = 256
        }
    }

/** [bitmap] en niveaux de gris (luminance Rec. 601). */
internal fun gray(bitmap: Bitmap): GrayImage {
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    val luma =
        IntArray(pixels.size) { i ->
            val pixel = pixels[i]
            (RED * Color.red(pixel) + GREEN * Color.green(pixel) + BLUE * Color.blue(pixel)).toInt()
        }
    return GrayImage(bitmap.width, bitmap.height, luma)
}

private const val RED = 0.299
private const val GREEN = 0.587
private const val BLUE = 0.114
