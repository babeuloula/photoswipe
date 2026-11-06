package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.domain.service.SameDayMatcher
import java.time.Clock
import java.time.LocalDate

/**
 * Pile des médias marqués pour suppression et pas encore confirmés, ouverte depuis le résumé pour
 * les revoir en grand avant de confirmer (PS-112).
 *
 * Une carte par média, dans l'ordre des vignettes du résumé (`GetMarkedPreviews` : décidés le plus
 * récemment d'abord), en commençant par le média touché. Un média marqué pour plusieurs années de
 * revue garde tous ses jours de revue : le garder doit lever chacun de ses marquages, sans quoi la
 * confirmation l'enverrait quand même à la corbeille. Les médias qui ne sont plus dans la galerie
 * sont laissés de côté.
 *
 * @param clock horloge de l'appareil : son fuseau donne le jour de revue de chaque média.
 */
class GetMarkedDeck(
    private val reviewRepository: ReviewRepository,
    private val mediaSource: MediaSource,
    private val sameDayMatcher: SameDayMatcher,
    private val clock: Clock,
) {
    /** Médias marqués, [first] en tête s'il est marqué, avec les jours de revue de leurs marquages. */
    suspend operator fun invoke(first: MediaId? = null): List<MarkedMedia> {
        val marked = reviewRepository.entriesMarkedForDeletion().sortedByDescending { it.decidedAt }
        if (marked.isEmpty()) return emptyList()

        val yearsById = marked.groupBy({ it.mediaId }, { it.reviewYear })
        val media = mediaSource.mediaByIds(yearsById.keys).associateBy { it.id }
        val ordered = yearsById.keys.sortedByDescending { it == first }
        return ordered.mapNotNull { id ->
            media[id]?.let { item ->
                MarkedMedia(
                    media = item,
                    reviewDays =
                        yearsById
                            .getValue(id)
                            .distinct()
                            .sortedDescending()
                            .map { sameDayMatcher.reviewDay(item, it, clock.zone) },
                )
            }
        }
    }
}

/**
 * Média de la pile des marqués.
 *
 * @property media média marqué.
 * @property reviewDays jours de revue de ses marquages, du plus récent au plus ancien ; jamais vide.
 */
data class MarkedMedia(
    val media: MediaItem,
    val reviewDays: List<LocalDate>,
) {
    init {
        require(reviewDays.isNotEmpty()) { "a marked media has at least one review day" }
    }

    /** Identifiant du média. */
    val id: MediaId get() = media.id
}
