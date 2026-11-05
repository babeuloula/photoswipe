package fr.babeuloula.photoswipe.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

/**
 * Accès au journal des décisions (`decision_journal`) et, dans les mêmes transactions, aux
 * décisions (`review_entry`) : une décision et sa ligne de journal changent ensemble (ADR 0019).
 */
@Dao
abstract class DecisionJournalDao {
    /** Écrit la décision de [record] et l'ajoute au journal ; renvoie l'identifiant de la ligne. */
    @Transaction
    open suspend fun record(record: DecisionRecordEntity): Long {
        upsertEntry(record.made)
        return insert(record)
    }

    /** Annule [record] : restaure l'entrée remplacée, ou supprime l'entrée, et retire la ligne du journal. */
    @Transaction
    open suspend fun undo(record: DecisionRecordEntity) {
        val previous = record.previous
        if (previous == null) deleteEntry(record.made.mediaId, record.made.reviewYear) else upsertEntry(previous)
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

    @Query("DELETE FROM review_entry WHERE media_id = :mediaId AND review_year = :reviewYear")
    protected abstract suspend fun deleteEntry(
        mediaId: Long,
        reviewYear: Int,
    )
}
