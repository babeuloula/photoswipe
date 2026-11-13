package fr.babeuloula.photoswipe.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ligne de la table `daily_activity` : l'activité de tri d'une journée (PS-154, ADR 0037).
 *
 * @property day jour où les décisions ont été prises, en jours depuis l'epoch.
 * @property keptCount médias gardés ce jour-là, favoris compris.
 * @property favoriteCount médias gardés et mis en favori.
 * @property trashedCount médias envoyés à la corbeille par une confirmation.
 * @property freedBytes poids de ces médias, en octets.
 */
@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey @ColumnInfo(name = "day") val day: Long,
    @ColumnInfo(name = "kept_count") val keptCount: Int,
    @ColumnInfo(name = "favorite_count") val favoriteCount: Int,
    @ColumnInfo(name = "trashed_count") val trashedCount: Int,
    @ColumnInfo(name = "freed_bytes") val freedBytes: Long,
)
