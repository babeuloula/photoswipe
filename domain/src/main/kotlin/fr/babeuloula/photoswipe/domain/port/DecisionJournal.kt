package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.DecisionRecord
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.ReviewEntry
import java.time.LocalDate

/**
 * Journal des décisions de chaque jour, pour que l'undo survive à la sortie de la pile et à la
 * mort du process (SPEC §3.4, ADR 0019). Implémenté dans `:data` sur Room, dans la même base que
 * les décisions.
 *
 * Écrire une décision et la journaliser, ou l'annuler et l'effacer du journal, se fait d'un seul
 * coup (transaction) : après un arrêt brutal, le journal et les décisions restent d'accord.
 */
interface DecisionJournal {
    /**
     * Écrit [made] à la place de [previous] (`null` si le média était à trier), et l'ajoute au
     * journal du jour [day] ; renvoie la ligne du journal.
     */
    suspend fun record(
        day: LocalDate,
        made: ReviewEntry,
        previous: ReviewEntry?,
    ): DecisionRecord

    /** Annule [record] : restaure l'entrée remplacée, ou supprime l'entrée, et le retire du journal. */
    suspend fun undo(record: DecisionRecord)

    /** Journal du jour [day], de la décision la plus ancienne à la plus récente. */
    suspend fun recordsFor(day: LocalDate): List<DecisionRecord>

    /** Retire du journal toutes les décisions des médias [mediaIds] (suppression confirmée, orphelins). */
    suspend fun forget(mediaIds: Set<MediaId>)

    /** Retire du journal les jours antérieurs à [day], sortis de la fenêtre de revue. */
    suspend fun forgetDaysBefore(day: LocalDate)
}
