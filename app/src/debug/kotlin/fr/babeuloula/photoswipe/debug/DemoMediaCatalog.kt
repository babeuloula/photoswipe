package fr.babeuloula.photoswipe.debug

import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.MediaType
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/**
 * Jeu de données de démo (SPEC §10, niveau 2) : des souvenirs pour aujourd'hui et les 6 jours
 * précédents, sur plusieurs années, avec les cas limites de la SPEC.
 *
 * - Chaque jour de la fenêtre reçoit des médias de [YEARS_BACK] années précédentes, dont un jour
 *   avec plusieurs médias la même année.
 * - Une photo du 29 février 2024 (montrée le 28 février des années non bissextiles).
 * - Une grosse vidéo (environ 2 Go).
 * - Un média sans `DATE_TAKEN` : MediaStore le daterait par `DATE_ADDED` ; il est donc daté ici de son
 *   ajout, à une heure ronde, comme le ferait le repli de `MediaStoreMediaSource`.
 *
 * Les médias n'ont pas de fichier réel : leurs URI ne s'ouvrent pas. Le jeu sert à l'accueil et
 * aux compteurs ; les vraies images viendront du seeder MediaStore (epic 10).
 */
object DemoMediaCatalog {
    /** Nombre d'années de souvenirs générées pour chaque jour de la fenêtre. */
    const val YEARS_BACK: Int = 8

    /** Taille de la grosse vidéo : 2 Go. */
    const val BIG_VIDEO_BYTES: Long = 2_000_000_000

    private const val WINDOW_DAYS = 7L
    private const val PHOTO_BYTES = 3_500_000L
    private const val VIDEO_BYTES = 45_000_000L

    /** Médias de démo pour la fenêtre qui finit le jour courant de [clock]. */
    fun generate(clock: Clock): List<MediaItem> {
        val zone = clock.zone
        val today = LocalDate.now(clock)
        var nextId = 1L

        fun media(
            day: LocalDate,
            time: LocalTime,
            type: MediaType = MediaType.IMAGE,
            sizeBytes: Long = PHOTO_BYTES,
            duration: Duration? = null,
        ): MediaItem =
            aMediaItem(
                id = nextId++,
                type = type,
                takenAt = day.atTime(time).atZone(zone).toInstant(),
                sizeBytes = sizeBytes,
                duration = duration,
            )

        val window = (0 until WINDOW_DAYS).map { today.minusDays(it) }
        val memories =
            window.flatMap { day ->
                (1..YEARS_BACK).map { yearsAgo ->
                    media(day.minusYears(yearsAgo.toLong()), LocalTime.of(9 + yearsAgo, 0))
                }
            }
        val today3YearsAgo = today.minusYears(3)
        val sameDay =
            listOf(
                media(today3YearsAgo, LocalTime.of(7, 30)),
                media(today3YearsAgo, LocalTime.of(18, 45), MediaType.VIDEO, VIDEO_BYTES, Duration.ofSeconds(32)),
            )
        val edgeCases =
            listOf(
                media(LocalDate.of(2024, 2, 29), LocalTime.NOON),
                media(
                    today.minusYears(1),
                    LocalTime.of(20, 0),
                    MediaType.VIDEO,
                    BIG_VIDEO_BYTES,
                    Duration.ofMinutes(25),
                ),
                media(today.minusDays(1).minusYears(2), LocalTime.MIDNIGHT),
            )
        return memories + sameDay + edgeCases
    }
}
