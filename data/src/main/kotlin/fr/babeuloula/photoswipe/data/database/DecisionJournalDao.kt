package fr.babeuloula.photoswipe.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import fr.babeuloula.photoswipe.domain.model.ReviewDecision

/**
 * Accès au journal des décisions (`decision_journal`) et, dans les mêmes transactions, aux
 * décisions (`review_entry`) : une décision et sa ligne de journal changent ensemble (ADR 0019).
 * L'activité du jour (`daily_activity`, ADR 0037) suit dans la même transaction : un média gardé y
 * entre avec sa décision et en sort si elle est annulée.
 */
@Dao
abstract class DecisionJournalDao {
    /**
     * Écrit la décision de [record] et l'ajoute au journal ; renvoie l'identifiant de la ligne. Un
     * média gardé compte dans l'activité du jour [activityDay] (jours depuis l'epoch), celui où la
     * décision est prise.
     */
    @Transaction
    open suspend fun record(
        record: DecisionRecordEntity,
        activityDay: Long,
    ): Long {
        upsertEntry(record.made)
        if (record.made.decision == ReviewDecision.KEPT) addKept(activityDay, 1, record.made.favorites(1))
        return insert(record)
    }

    /**
     * Annule [record] : restaure l'entrée remplacée, ou supprime l'entrée, et retire la ligne du
     * journal. Un média gardé quitte l'activité du jour [activityDay], celui où il avait été décidé.
     */
    @Transaction
    open suspend fun undo(
        record: DecisionRecordEntity,
        activityDay: Long,
    ) {
        val previous = record.previous
        if (previous == null) deleteEntry(record.made.mediaId, record.made.reviewYear) else upsertEntry(previous)
        if (record.made.decision == ReviewDecision.KEPT) addKept(activityDay, -1, record.made.favorites(-1))
        delete(record.id)
    }

    /** Journal du jour [day] (jours depuis l'epoch), dans l'ordre des décisions. */
    @Query("SELECT * FROM decision_journal WHERE day = :day ORDER BY id")
    abstract suspend fun recordsFor(day: Long): List<DecisionRecordEntity>

    /** Retire du journal toutes les décisions des médias [mediaIds]. */
    @Query("DELETE FROM decision_journal WHERE made_media_id IN (:mediaIds)")
    abstract suspend fun forget(mediaIds: List<Long>)

    /** Retire du journal les jours antérieurs à [day] (jours depuis l'epoch). */
    @Query("DELETE FROM decision_journal WHERE day < :day")
    abstract suspend fun forgetDaysBefore(day: Long)

    @Insert
    protected abstract suspend fun insert(record: DecisionRecordEntity): Long

    @Query("DELETE FROM decision_journal WHERE id = :id")
    protected abstract suspend fun delete(id: Long)

    @Upsert
    protected abstract suspend fun upsertEntry(entry: ReviewEntryEntity)

    // Crée le jour au besoin. Jamais sous zéro : une décision prise avant la version 5 peut être
    // annulée sans avoir été comptée.
    @Query(
        "INSERT INTO daily_activity (day, kept_count, favorite_count, trashed_count, freed_bytes) " +
            "VALUES (:day, MAX(0, :kept), MAX(0, :favorites), 0, 0) " +
            "ON CONFLICT(day) DO UPDATE SET kept_count = MAX(0, kept_count + :kept), " +
            "favorite_count = MAX(0, favorite_count + :favorites)",
    )
    protected abstract suspend fun addKept(
        day: Long,
        kept: Int,
        favorites: Int,
    )

    @Query("DELETE FROM review_entry WHERE media_id = :mediaId AND review_year = :reviewYear")
    protected abstract suspend fun deleteEntry(
        mediaId: Long,
        reviewYear: Int,
    )
}

/** Variation du nombre de favoris du jour pour une entrée gardée : [sign] si elle est aussi un favori. */
private fun ReviewEntryEntity.favorites(sign: Int): Int = if (favoritePending) sign else 0
