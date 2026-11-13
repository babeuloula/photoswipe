package fr.babeuloula.photoswipe.domain.port

import fr.babeuloula.photoswipe.domain.model.DailyActivity
import java.time.LocalDate

/**
 * Historique de l'activité de tri, un total par jour (PS-154, ADR 0037). Implémenté dans `:data`
 * sur Room, dans la même base que les décisions.
 *
 * Les médias gardés et les favoris y entrent avec la décision elle-même, et en sortent si elle est
 * annulée : c'est le journal des décisions ([DecisionJournal]) qui les compte, dans sa transaction.
 * Ce port n'ajoute que les suppressions confirmées et relit les totaux.
 */
interface ActivityHistory {
    /** Ajoute au jour [day] une confirmation de [count] médias pesant [bytes] octets. */
    suspend fun recordTrashed(
        day: LocalDate,
        count: Int,
        bytes: Long,
    )

    /**
     * Retire du jour [day] [count] médias pesant [bytes] octets, confirmés ce jour-là puis restaurés
     * depuis la corbeille (PS-168). Les totaux ne descendent jamais sous zéro.
     */
    suspend fun removeTrashed(
        day: LocalDate,
        count: Int,
        bytes: Long,
    )

    /** Jours enregistrés depuis [day] compris, du plus ancien au plus récent ; les jours sans tri sont absents. */
    suspend fun since(day: LocalDate): List<DailyActivity>

    /** Premier jour enregistré, `null` si l'historique est vide. */
    suspend fun firstDay(): LocalDate?
}
