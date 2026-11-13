package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import java.time.LocalDate

/**
 * Tri par lot depuis la vue d'ensemble d'un jour (PS-159) : garder ou marquer plusieurs souvenirs
 * d'un coup, avec les mêmes use cases que la pile, puis annuler le lot entier.
 *
 * Un souvenir déjà trié change de décision, comme dans la pile ; garder lève aussi ses marquages
 * des autres années (PS-113). Pas de favori par lot : sans l'accès « Gestion des médias », Android
 * demanderait un dialogue par média.
 */
class SortInBatch(
    private val keepMedia: KeepMedia,
    private val markForDeletion: MarkForDeletion,
    private val undoLastDecision: UndoLastDecision,
) {
    /** Garde [media], souvenirs du jour [day] ; renvoie le lot, pour l'annuler. */
    suspend fun keep(
        media: List<MediaItem>,
        day: LocalDate,
    ): ReviewSession = media.fold(ReviewSession()) { session, item -> session.record(keepMedia(item, day)) }

    /** Marque [media], souvenirs du jour [day], pour suppression ; renvoie le lot, pour l'annuler. */
    suspend fun mark(
        media: List<MediaItem>,
        day: LocalDate,
    ): ReviewSession =
        media.fold(ReviewSession()) { session, item -> session.record(markForDeletion.withCompanion(item, day)) }

    /**
     * Annule toutes les décisions du lot [batch], de la plus récente à la plus ancienne. Une
     * décision déjà confirmée (média dans la corbeille) arrête l'annulation, comme dans la pile.
     */
    suspend fun undo(batch: ReviewSession) {
        var session = batch
        while (true) {
            session =
                when (val result = undoLastDecision(session)) {
                    is UndoLastDecision.Result.Undone -> result.session
                    UndoLastDecision.Result.NothingToUndo, UndoLastDecision.Result.AlreadyConfirmed -> return
                }
        }
    }
}
