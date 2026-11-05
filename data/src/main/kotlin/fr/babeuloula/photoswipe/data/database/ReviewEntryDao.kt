package fr.babeuloula.photoswipe.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Accès à la table `review_entry` (SPEC §6).
 *
 * Les listes passées à `IN (…)` deviennent autant de paramètres SQLite : au-delà de
 * [MAX_IDS_PER_QUERY], l'appelant les découpe (limite de paramètres d'une requête).
 */
@Dao
interface ReviewEntryDao {
    /** Insère [entry], ou remplace l'entrée du même couple (média, année de revue). */
    @Upsert
    suspend fun upsert(entry: ReviewEntryEntity)

    /** Entrées de l'année de revue [reviewYear] pour les médias [mediaIds] (pile et compteurs d'un jour). */
    @Query("SELECT * FROM review_entry WHERE review_year = :reviewYear AND media_id IN (:mediaIds)")
    suspend fun entriesFor(
        reviewYear: Int,
        mediaIds: List<Long>,
    ): List<ReviewEntryEntity>

    /** Entrées des années de revue [reviewYears], réémises après chaque écriture dans la table. */
    @Query("SELECT * FROM review_entry WHERE review_year IN (:reviewYears)")
    fun observeEntries(reviewYears: List<Int>): Flow<List<ReviewEntryEntity>>

    /** Supprime l'entrée du couple ([mediaId], [reviewYear]). */
    @Query("DELETE FROM review_entry WHERE media_id = :mediaId AND review_year = :reviewYear")
    suspend fun delete(
        mediaId: Long,
        reviewYear: Int,
    )

    /** Entrées marquées pour suppression et pas encore confirmées, toutes années confondues. */
    @Query("SELECT * FROM review_entry WHERE decision = 'MARKED_FOR_DELETION'")
    suspend fun entriesMarkedForDeletion(): List<ReviewEntryEntity>

    /** Entrées dont le favori système est demandé mais pas encore appliqué. */
    @Query("SELECT * FROM review_entry WHERE favorite_pending = 1")
    suspend fun entriesWithPendingFavorite(): List<ReviewEntryEntity>

    /** Passe à `TRASHED` les entrées marquées pour suppression des médias [mediaIds]. */
    @Query(
        "UPDATE review_entry SET decision = 'TRASHED' WHERE decision = 'MARKED_FOR_DELETION' AND media_id IN (:mediaIds)",
    )
    suspend fun markTrashed(mediaIds: List<Long>)

    /** Retire le favori en attente des entrées des médias [mediaIds], toutes années confondues. */
    @Query("UPDATE review_entry SET favorite_pending = 0 WHERE media_id IN (:mediaIds)")
    suspend fun markFavoritesApplied(mediaIds: List<Long>)

    /** Identifiants de tous les médias qui ont au moins une entrée. */
    @Query("SELECT DISTINCT media_id FROM review_entry")
    suspend fun knownMediaIds(): List<Long>

    /** Supprime toutes les entrées des médias [mediaIds], quelle que soit l'année. */
    @Query("DELETE FROM review_entry WHERE media_id IN (:mediaIds)")
    suspend fun deleteAllFor(mediaIds: List<Long>)

    companion object {
        /**
         * Nombre maximal d'identifiants par requête : loin sous la limite de paramètres de SQLite
         * (999 avant SQLite 3.32), en gardant de la place pour les autres paramètres.
         */
        const val MAX_IDS_PER_QUERY: Int = 500
    }
}
