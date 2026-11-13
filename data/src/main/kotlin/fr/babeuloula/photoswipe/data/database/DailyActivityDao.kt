package fr.babeuloula.photoswipe.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

/**
 * Accès à la table `daily_activity`, l'activité de tri par jour (PS-154, ADR 0037). Les médias
 * gardés y sont comptés par [DecisionJournalDao], dans la transaction de la décision.
 */
@Dao
abstract class DailyActivityDao {
    /** Ajoute au jour [day] (jours depuis l'epoch) une confirmation de [count] médias pesant [bytes] octets. */
    @Transaction
    open suspend fun addTrashed(
        day: Long,
        count: Int,
        bytes: Long,
    ) {
        ensureDay(day)
        increaseTrashed(day, count, bytes)
    }

    /** Jours enregistrés depuis [day] compris, du plus ancien au plus récent. */
    @Query("SELECT * FROM daily_activity WHERE day >= :day ORDER BY day")
    abstract suspend fun since(day: Long): List<DailyActivityEntity>

    /** Premier jour enregistré, `null` si la table est vide. */
    @Query("SELECT MIN(day) FROM daily_activity")
    abstract suspend fun firstDay(): Long?

    @Query(
        "INSERT OR IGNORE INTO daily_activity (day, kept_count, favorite_count, trashed_count, freed_bytes) " +
            "VALUES (:day, 0, 0, 0, 0)",
    )
    protected abstract suspend fun ensureDay(day: Long)

    @Query(
        "UPDATE daily_activity SET trashed_count = trashed_count + :count, freed_bytes = freed_bytes + :bytes " +
            "WHERE day = :day",
    )
    protected abstract suspend fun increaseTrashed(
        day: Long,
        count: Int,
        bytes: Long,
    )
}
