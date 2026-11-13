package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
import java.time.Duration

/**
 * Repère les photos proches (PS-140, ADR 0035) : des photos prises à la suite, chacune moins de
 * [maxGap] après la précédente, comme une rafale ou plusieurs essais de la même scène. Les vidéos ne
 * comptent pas ; les photos sont rangées par prise de vue, sans regarder leur contenu.
 *
 * Service pur : la règle se lit sur la seule date de prise de vue, sans rien décoder ni enregistrer.
 *
 * @param maxGap écart maximal entre deux photos consécutives d'un même groupe.
 */
class SimilarMediaGrouping(
    private val maxGap: Duration = DEFAULT_MAX_GAP,
) {
    init {
        require(!maxGap.isNegative) { "maxGap must not be negative: $maxGap" }
    }

    /**
     * Place de chaque photo de [media] qui appartient à un groupe d'au moins deux photos proches,
     * avec la photo prise juste avant elle dans le groupe.
     */
    fun groups(media: List<MediaItem>): Map<MediaId, SimilarGroup> {
        val photos = media.filter { it.type == MediaType.IMAGE }.distinctBy { it.id }.sortedBy { it.takenAt }
        val runs = mutableListOf<MutableList<MediaItem>>()
        photos.forEach { photo ->
            val last = runs.lastOrNull()?.last()
            if (last != null && Duration.between(last.takenAt, photo.takenAt) <= maxGap) {
                runs.last() += photo
            } else {
                runs += mutableListOf(photo)
            }
        }
        return runs
            .filter { it.size >= 2 }
            .flatMap { run ->
                run.mapIndexed { index, photo ->
                    photo.id to SimilarGroup(index + 1, run.size, previousUri = run.getOrNull(index - 1)?.uri)
                }
            }.toMap()
    }

    companion object {
        /** Écart par défaut : 30 secondes entre deux photos consécutives. */
        val DEFAULT_MAX_GAP: Duration = Duration.ofSeconds(30)
    }
}
