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
        private val FILE_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss")
    }
}

/**
 * Plan du seeder MediaStore (SPEC §10, niveau 3) : pour aujourd'hui et les 6 jours précédents, une
 * photo par année sur [YEARS_BACK] ans, plus une vidéo par jour, d'une année qui change chaque jour.
 * Les heures diffèrent d'une année à l'autre pour que l'ordre de la pile soit lisible.
 *
 * Pour les retours d'utilisateurs du 2026-11-11 : les jours [BURST_DAYS] ont en plus une rafale de
 * [BURST_SIZE] photos prises à quelques secondes d'intervalle (photos proches, PS-140) ; le jour
 * [SINGLE_PHOTO_DAY] n'a qu'une photo, de l'an dernier, et pas de vidéo : la supprimer vide le jour (PS-136).
 */
object SeedCatalog {
    /** Nombre d'années de souvenirs pour chaque jour de la fenêtre. */
    const val YEARS_BACK: Int = 5

    /** Jours couverts : aujourd'hui et les 6 précédents, comme la fenêtre de l'accueil. */
    const val WINDOW_DAYS: Int = 7

    /** Jours (en jours avant aujourd'hui) qui ont une rafale de photos. */
    val BURST_DAYS: Set<Int> = setOf(0, 4)

    /** Photos d'une rafale. */
    const val BURST_SIZE: Int = 3

    /** Jour (en jours avant aujourd'hui) qui n'a qu'une photo. */
    const val SINGLE_PHOTO_DAY: Int = 2

    private const val FIRST_PHOTO_HOUR = 9
    private const val BURST_YEARS_AGO = 2L
    private const val BURST_GAP_SECONDS = 4L
    private val BURST_TIME: LocalTime = LocalTime.of(15, 0)
    private val VIDEO_TIME: LocalTime = LocalTime.of(19, 30)

    /** Médias à insérer pour la fenêtre qui finit le jour [today], dans le fuseau [zone]. */
    fun plan(
        today: LocalDate,
        zone: ZoneId,
    ): List<SeedMedia> =
        (0 until WINDOW_DAYS).flatMap { daysAgo ->
            val day = today.minusDays(daysAgo.toLong())
            if (daysAgo == SINGLE_PHOTO_DAY) {
                return@flatMap listOf(photo(day, yearsAgo = 1, zone))
            }
            val photos = (1..YEARS_BACK).map { yearsAgo -> photo(day, yearsAgo, zone) }
            val burst = if (daysAgo in BURST_DAYS) burst(day, zone) else emptyList()
            val videoYearsAgo = 1L + daysAgo % YEARS_BACK
            photos + burst + SeedMedia(MediaType.VIDEO, day.minusYears(videoYearsAgo).atTime(VIDEO_TIME).atZone(zone))
        }

    /** Photo du jour [day], [yearsAgo] ans plus tôt, à une heure propre à son année. */
    private fun photo(
        day: LocalDate,
        yearsAgo: Int,
        zone: ZoneId,
    ): SeedMedia =
        SeedMedia(
            MediaType.IMAGE,
            day.minusYears(yearsAgo.toLong()).atTime(LocalTime.of(FIRST_PHOTO_HOUR + yearsAgo, 0)).atZone(zone),
        )

    /** Rafale du jour [day] : [BURST_SIZE] photos prises à [BURST_GAP_SECONDS] secondes d'intervalle. */
    private fun burst(
        day: LocalDate,
        zone: ZoneId,
    ): List<SeedMedia> {
        val start = day.minusYears(BURST_YEARS_AGO).atTime(BURST_TIME).atZone(zone)
        return (0 until BURST_SIZE).map { shot ->
            SeedMedia(MediaType.IMAGE, start.plusSeconds(shot * BURST_GAP_SECONDS))
        }
    }
}
