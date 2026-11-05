package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.model.SessionDecision
import fr.babeuloula.photoswipe.domain.port.DecisionJournal
import fr.babeuloula.photoswipe.domain.port.ReviewRepository

/**
 * Annule la décision la plus récente de la session (SPEC §3.4).
 *
 * Les décisions s'annulent une à une, de la plus récente à la plus ancienne. L'undo remet le
 * couple (média, année de revue) dans l'état d'avant la décision : l'entrée remplacée est
 * restaurée, ou l'entrée est supprimée si le média était à trier, et la décision quitte le journal
 * du jour, d'un seul coup (ADR 0019).
 *
 * Une suppression confirmée (`TRASHED`) ne s'annule pas : le média est déjà dans la corbeille
 * système. Le résultat est alors [Result.AlreadyConfirmed] et la session reste inchangée.
 */
class UndoLastDecision(
    private val reviewRepository: ReviewRepository,
    private val journal: DecisionJournal,
) {
    /** Annule la dernière décision de [session]. */
    suspend operator fun invoke(session: ReviewSession): Result {
        val decision = session.lastDecision ?: return Result.NothingToUndo
        val current = reviewRepository.entriesFor(decision.reviewYear, setOf(decision.media.id)).singleOrNull()
        return if (current?.decision == ReviewDecision.TRASHED) {
            Result.AlreadyConfirmed
        } else {
            journal.undo(decision.record)
            Result.Undone(decision, session.withoutLastDecision())
        }
    }

    /** Issue d'un undo. */
    sealed interface Result {
        /** [decision] est annulée : son média revient sur la pile ; [session] est la session à garder. */
        data class Undone(
            val decision: SessionDecision,
            val session: ReviewSession,
        ) : Result

        /** La session n'a aucune décision à annuler. */
        data object NothingToUndo : Result

        /** La décision la plus récente a déjà été confirmée (média dans la corbeille) : rien n'est annulé. */
        data object AlreadyConfirmed : Result
    }
}
