package fr.babeuloula.photoswipe.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Accès à la table `confirmed_deletion`, l'historique des suppressions confirmées (ADR 0034). */
@Dao
interface ConfirmedDeletionDao {
    /** Ajoute [deletions] à l'historique. */
    @Insert
    suspend fun insert(deletions: List<ConfirmedDeletionEntity>)

    /** Suppressions de l'historique, dans l'ordre d'enregistrement, réémises après chaque écriture. */
    @Query("SELECT * FROM confirmed_deletion ORDER BY id")
    fun observeAll(): Flow<List<ConfirmedDeletionEntity>>

    /** Retire les suppressions confirmées avant [instant] (millisecondes epoch). */
    @Query("DELETE FROM confirmed_deletion WHERE confirmed_at < :instant")
    suspend fun deleteConfirmedBefore(instant: Long)
}
