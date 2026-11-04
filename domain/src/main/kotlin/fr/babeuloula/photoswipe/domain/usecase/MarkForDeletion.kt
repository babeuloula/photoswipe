package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import java.time.Clock
import java.time.LocalDate

/**
 * Marque un média pour suppression pendant la revue d'un jour (swipe à gauche, SPEC §2 et §3.3).
 *
 * Rien n'est supprimé ici : seule la décision `MARKED_FOR_DELETION` est écrite, pour le couple
 * (média, année du jour cible), avant de rendre la main (SPEC §3.4). Le média n'est envoyé à la
 * corbeille qu'en fin de session, après confirmation (SPEC §3.5) ; jusque-là, l'utilisateur peut
 * changer d'avis. La taille du média est mémorisée pour l'espace libéré estimé du résumé.
 *
 * @param clock horloge de l'appareil : l'instant de la décision ordonne l'undo de la session.
 */
class MarkForDeletion(
    private val reviewRepository: ReviewRepository,
    private val clock: Clock,
) {
    /** Marque [media], présenté dans la pile du jour cible [day], pour suppression. */
    suspend operator fun invoke(
        media: MediaItem,
        day: LocalDate,
    ) {
        reviewRepository.save(
            ReviewEntry(
                mediaId = media.id,
                reviewYear = day.year,
                decision = ReviewDecision.MARKED_FOR_DELETION,
                sizeBytes = media.sizeBytes,
                decidedAt = clock.instant(),
            ),
        )
    }
}
