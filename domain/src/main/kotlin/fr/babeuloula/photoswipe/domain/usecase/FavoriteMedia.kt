package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.SessionDecision
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import java.time.Clock
import java.time.LocalDate

/**
 * Met un média en favori pendant la revue d'un jour (menu du swipe vers le haut, SPEC §2).
 *
 * Un favori est toujours gardé : la décision est `KEPT`, avec un favori système en attente
 * (`favorite_pending`). La pile demande aussitôt le favori à la galerie (`RequestFavorite`), et
 * `RecordFavoriteResult` l'applique si l'utilisateur accepte (SPEC §2, §3.5) ; ici, seule la
 * décision est écrite, avant de rendre la main (SPEC §3.4). Sur un média déjà gardé cette année, la décision gardée est remplacée et l'undo la
 * restaure.
 *
 * @param journal journal du jour, pour l'undo (ADR 0019).
 * @param clock horloge de l'appareil : l'instant de la décision.
 */
class FavoriteMedia(
    reviewRepository: ReviewRepository,
    journal: DecisionJournal,
    clock: Clock,
) {
    private val recorder = DecisionRecorder(reviewRepository, journal, clock)

    /**
     * Garde [media], présenté dans la pile du jour cible [day], et demande sa mise en favori ;
     * renvoie la décision à empiler pour l'undo.
     */
    suspend operator fun invoke(
        media: MediaItem,
        day: LocalDate,
    ): SessionDecision = recorder.record(media, day, ReviewDecision.KEPT, favoritePending = true)
}
