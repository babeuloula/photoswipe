package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.SessionDecision
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
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
 * Un média encore marqué pour suppression une autre année est aussi gardé pour ces années-là
 * (PS-113) : sinon la confirmation l'enverrait à la corbeille alors qu'il vient d'être gardé.
 *
 * @param journal journal du jour, pour l'undo (ADR 0019).
 * @param clock horloge de l'appareil : l'instant de la décision.
 */
class KeepMedia(
    reviewRepository: ReviewRepository,
    journal: DecisionJournal,
    clock: Clock,
) {
    private val recorder = DecisionRecorder(reviewRepository, journal, clock)

    /**
     * Garde [media], présenté dans la pile du jour cible [day] ; renvoie les décisions à empiler pour
     * l'undo, celle du jour en dernier (les marquages levés des autres années avant elle).
     */
    suspend operator fun invoke(
        media: MediaItem,
        day: LocalDate,
    ): List<SessionDecision> = recorder.keepOtherMarkings(media, day) + recorder.record(media, day, ReviewDecision.KEPT)
}
