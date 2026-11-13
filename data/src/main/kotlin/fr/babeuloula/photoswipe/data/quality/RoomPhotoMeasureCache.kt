package fr.babeuloula.photoswipe.data.quality

import fr.babeuloula.photoswipe.data.database.PhotoMeasureDao
import fr.babeuloula.photoswipe.data.database.PhotoMeasureEntity
import fr.babeuloula.photoswipe.data.database.ReviewEntryDao
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.PhotoMeasure
import fr.babeuloula.photoswipe.domain.port.PhotoMeasureCache
import javax.inject.Inject

/** [PhotoMeasureCache] sur Room, table `photo_measure` ; les listes d'identifiants vont par lots. */
class RoomPhotoMeasureCache
    @Inject
    constructor(
        private val dao: PhotoMeasureDao,
    ) : PhotoMeasureCache {
        override suspend fun measures(ids: Set<MediaId>): Map<MediaId, PhotoMeasure> =
            ids
                .map { it.value }
                .chunked(ReviewEntryDao.MAX_IDS_PER_QUERY)
                .flatMap { dao.measuresOf(it) }
                .associate { MediaId(it.mediaId) to PhotoMeasure(it.sharpness, it.brightness) }

        override suspend fun save(measures: Map<MediaId, PhotoMeasure>) {
            dao.save(
                measures.map { (id, measure) ->
                    PhotoMeasureEntity(id.value, measure.sharpness, measure.brightness)
                },
            )
        }

        override suspend fun forget(ids: Set<MediaId>) {
            ids.map { it.value }.chunked(ReviewEntryDao.MAX_IDS_PER_QUERY).forEach { dao.forget(it) }
        }
    }
