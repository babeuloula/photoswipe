package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.PhotoMeasure

/** Mesures déjà prises (PS-164), pour ne mesurer chaque photo qu'une fois. Implémenté sur Room. */
interface PhotoMeasureCache {
    /** Mesures connues parmi [ids]. */
    suspend fun measures(ids: Set<MediaId>): Map<MediaId, PhotoMeasure>

    /** Retient [measures]. */
    suspend fun save(measures: Map<MediaId, PhotoMeasure>)

    /** Oublie les mesures de [ids] (médias disparus de la galerie). */
    suspend fun forget(ids: Set<MediaId>)
}
