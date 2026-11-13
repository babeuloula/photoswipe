package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.SessionDecision
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
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
 * @param journal journal du jour, pour l'undo (ADR 0019).
 * @param clock horloge de l'appareil : l'instant de la décision.
 */
class MarkForDeletion(
    reviewRepository: ReviewRepository,
    journal: DecisionJournal,
    clock: Clock,
) {
    private val recorder = DecisionRecorder(reviewRepository, journal, clock)

    /**
     * Marque [media], présenté dans la pile du jour cible [day], pour suppression ; renvoie la
     * décision à empiler pour l'undo.
     */
    suspend operator fun invoke(
        media: MediaItem,
        day: LocalDate,
    ): SessionDecision = recorder.record(media, day, ReviewDecision.MARKED_FOR_DELETION)

    /**
     * Marque [media] et, s'il en a un, son fichier RAW (PS-166) ; renvoie les décisions à empiler
     * pour l'undo, celle de la photo en dernier.
     */
    suspend fun withCompanion(
        media: MediaItem,
        day: LocalDate,
    ): List<SessionDecision> =
        recorder.recordCompanion(media, day, ReviewDecision.MARKED_FOR_DELETION) + invoke(media, day)
}
