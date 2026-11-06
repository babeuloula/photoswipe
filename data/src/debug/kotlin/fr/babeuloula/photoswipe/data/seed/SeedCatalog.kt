package fr.babeuloula.photoswipe.data.seed

import fr.babeuloula.photoswipe.domain.model.MediaType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** Un média d'exemple à insérer dans MediaStore : son type, sa date de prise de vue et son nom de fichier. */
data class SeedMedia(
    val type: MediaType,
    val takenAt: ZonedDateTime,
) {
    /** Nom du fichier, daté pour être reconnaissable dans la galerie. */
    val displayName: String
        get() = "$FILE_PREFIX${FILE_DATE.format(takenAt)}.${if (type == MediaType.VIDEO) "mp4" else "jpg"}"

    companion object {
        /** Début du nom de chaque fichier du seeder, qui permet de les retrouver. */
        const val FILE_PREFIX: String = "photoswipe-seed-"
        private val FILE_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")
    }
}

/**
 * Plan du seeder MediaStore (SPEC §10, niveau 3) : pour aujourd'hui et les 6 jours précédents, une
 * photo par année sur [YEARS_BACK] ans, plus une vidéo par jour, d'une année qui change chaque jour.
 * Les heures diffèrent d'une année à l'autre pour que l'ordre de la pile soit lisible.
 */
object SeedCatalog {
    /** Nombre d'années de souvenirs pour chaque jour de la fenêtre. */
    const val YEARS_BACK: Int = 5

    /** Jours couverts : aujourd'hui et les 6 précédents, comme la fenêtre de l'accueil. */
    const val WINDOW_DAYS: Int = 7

    private const val FIRST_PHOTO_HOUR = 9
    private val VIDEO_TIME: LocalTime = LocalTime.of(19, 30)

    /** Médias à insérer pour la fenêtre qui finit le jour [today], dans le fuseau [zone]. */
    fun plan(
        today: LocalDate,
        zone: ZoneId,
    ): List<SeedMedia> =
        (0 until WINDOW_DAYS).flatMap { daysAgo ->
            val day = today.minusDays(daysAgo.toLong())
            val photos =
                (1..YEARS_BACK).map { yearsAgo ->
                    val time = LocalTime.of(FIRST_PHOTO_HOUR + yearsAgo, 0)
                    SeedMedia(MediaType.IMAGE, day.minusYears(yearsAgo.toLong()).atTime(time).atZone(zone))
                }
            val videoYearsAgo = 1L + daysAgo % YEARS_BACK
            photos + SeedMedia(MediaType.VIDEO, day.minusYears(videoYearsAgo).atTime(VIDEO_TIME).atZone(zone))
        }
}
