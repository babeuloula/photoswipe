package fr.babeuloula.photoswipe.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/** Accès à la table `photo_measure` (PS-164). */
@Dao
interface PhotoMeasureDao {
    /** Mesures des photos [mediaIds]. */
    @Query("SELECT * FROM photo_measure WHERE media_id IN (:mediaIds)")
    suspend fun measuresOf(mediaIds: List<Long>): List<PhotoMeasureEntity>

    /** Ajoute ou remplace [measures]. */
    @Upsert
    suspend fun save(measures: List<PhotoMeasureEntity>)

    /** Oublie les mesures des photos [mediaIds]. */
    @Query("DELETE FROM photo_measure WHERE media_id IN (:mediaIds)")
    suspend fun forget(mediaIds: List<Long>)
}
