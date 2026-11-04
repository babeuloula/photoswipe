package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import java.time.Clock
import java.time.LocalDate

/**
 * Garde un média pendant la revue d'un jour (swipe à droite, SPEC §2 et §3.3).
 *
 * La décision `KEPT` est rattachée au couple (média, année de revue), l'année de revue étant celle
 * du jour cible : le média n'est plus proposé cette année-là, mais redevient à trier l'année
 * suivante, faute d'entrée pour cette nouvelle année. Elle remplace une éventuelle décision de la
 * même année (changement d'avis) et est écrite avant de rendre la main (SPEC §3.4).
 *
 * @param clock horloge de l'appareil : l'instant de la décision ordonne l'undo de la session.
 */
class KeepMedia(
    private val reviewRepository: ReviewRepository,
    private val clock: Clock,
) {
    /** Garde [media], présenté dans la pile du jour cible [day]. */
    suspend operator fun invoke(
        media: MediaItem,
        day: LocalDate,
    ) {
        reviewRepository.save(
            ReviewEntry(
                mediaId = media.id,
                reviewYear = day.year,
                decision = ReviewDecision.KEPT,
                sizeBytes = media.sizeBytes,
                decidedAt = clock.instant(),
            ),
        )
    }
}
