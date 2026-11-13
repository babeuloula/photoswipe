package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.PhotoMeasureCache
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import fr.babeuloula.photoswipe.domain.service.SimilarMediaGrouping
import java.time.Clock
import java.time.LocalDate

/**
 * Photos proches parmi tous les souvenirs d'un jour, triés ou non (PS-140, ADR 0035) : la pile dit
 * ainsi qu'une photo a des voisines, même si celles-ci ont déjà été triées.
 *
 * @param clock horloge de l'appareil : son fuseau sert à apparier les médias.
 */
class GetSimilarMemories(
    private val sameDayMatcher: SameDayMatcher,
    private val mediaSource: MediaSource,
    private val clock: Clock,
    private val grouping: SimilarMediaGrouping = SimilarMediaGrouping(),
    private val measures: PhotoMeasureCache? = null,
) {
    /**
     * Place de chaque photo du jour [day] qui a des voisines prises à la suite, ou une copie de la
     * même image d'après les empreintes déjà mesurées (PS-165).
     */
    suspend operator fun invoke(day: LocalDate): Map<MediaId, SimilarGroup> {
        val media = mediaSource.mediaTakenIn(sameDayMatcher.rangesFor(day, clock.zone))
        val fingerprints =
            measures
                ?.measures(media.mapTo(HashSet()) { it.id })
                ?.mapNotNull { (id, measure) -> measure.fingerprint?.let { id to it } }
                ?.toMap()
                .orEmpty()
        return grouping.groups(media, fingerprints)
    }
}
