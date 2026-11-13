package fr.babeuloula.photoswipe.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ligne de la table `photo_measure` : la netteté et la luminosité d'une photo, mesurées une fois
 * (PS-164, ADR 0039).
 *
 * @property mediaId identifiant MediaStore de la photo.
 * @property sharpness variance du laplacien de sa vignette.
 * @property brightness luminance moyenne de sa vignette, de 0 à 255.
 */
@Entity(tableName = "photo_measure")
data class PhotoMeasureEntity(
    @PrimaryKey @ColumnInfo(name = "media_id") val mediaId: Long,
    @ColumnInfo(name = "sharpness") val sharpness: Double,
    @ColumnInfo(name = "brightness") val brightness: Double,
)
