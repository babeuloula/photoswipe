package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.GrayImage
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.PhotoMeasure
import fr.babeuloula.photoswipe.domain.port.PhotoAnalyzer
import fr.babeuloula.photoswipe.domain.port.PhotoMeasureCache

/** [PhotoAnalyzer] qui rend les images de [images] et compte ses lectures. */
class FakePhotoAnalyzer(
    private val images: Map<MediaId, GrayImage> = emptyMap(),
) : PhotoAnalyzer {
    /** Médias lus, dans l'ordre, pour les vérifications du test. */
    val read = mutableListOf<MediaId>()

    override suspend fun grayThumbnail(media: MediaItem): GrayImage? {
        read += media.id
        return images[media.id]
    }
}

/** [PhotoMeasureCache] en mémoire. */
class FakePhotoMeasureCache(
    initial: Map<MediaId, PhotoMeasure> = emptyMap(),
) : PhotoMeasureCache {
    private val stored = initial.toMutableMap()

    /** Mesures gardées, pour les vérifications du test. */
    val measures: Map<MediaId, PhotoMeasure> get() = stored

    override suspend fun measures(ids: Set<MediaId>): Map<MediaId, PhotoMeasure> = stored.filterKeys { it in ids }

    override suspend fun save(measures: Map<MediaId, PhotoMeasure>) {
        stored += measures
    }

    override suspend fun forget(ids: Set<MediaId>) {
        stored -= ids
    }
}

/** Image grise de [width] × [height], de luminance [luma] partout. */
fun aFlatImage(
    luma: Int,
    width: Int = 16,
    height: Int = 16,
): GrayImage = GrayImage(width, height, IntArray(width * height) { luma })

/** Damier de [size] × [size] en noir et blanc : très net. */
fun aCheckerboard(size: Int = 16): GrayImage =
    GrayImage(size, size, IntArray(size * size) { i -> if ((i % size + i / size) % 2 == 0) 0 else 255 })
