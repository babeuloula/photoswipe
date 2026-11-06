package fr.babeuloula.photoswipe.domain.service

import fr.babeuloula.photoswipe.domain.model.MediaItem
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.MonthDay
import java.time.ZoneId

/**
 * Retrouve les médias « du même jour » qu'un jour cible (SPEC §3.1).
 *
 * Un média correspond au jour cible `D` si sa date de prise de vue a le même mois et le même
 * jour que `D`, dans une année strictement antérieure à celle de `D`. Les médias de l'année de
 * `D` (et ceux datés après) sont exclus : ils ne sont pas encore des souvenirs.
 *
 * 29 février : les années non bissextiles, les médias du 29 février apparaissent le 28 février,
 * avec ceux du 28 février. Les années bissextiles, chaque jour garde ses propres médias.
 *
 * Service pur : il ne lit ni l'horloge ni le fuseau du système. Le fuseau est passé à chaque
 * appel, car la SPEC demande celui de l'appareil **au moment du calcul** : l'appelant le lit sur
 * sa `Clock` injectée (ADR 0006), ce qui suit un changement de fuseau sans recréer le service.
 *
 * Limite connue : la prise de vue est un instant sans fuseau d'origine. Une photo prise dans un
 * autre fuseau que celui de l'appareil peut donc tomber sur le jour voisin.
 *
 * [rangesFor] traduit la même règle en plages de dates, pour que `MediaSource` ne lise que les
 * médias utiles : un média est dans l'une de ces plages si et seulement s'il correspond (à partir
 * de [EARLIEST_YEAR]).
 */
class SameDayMatcher {
    /** Indique si [media] a été pris le même jour que [target], une année antérieure. */
    fun matches(
        media: MediaItem,
        target: LocalDate,
        zone: ZoneId,
    ): Boolean {
        val takenOn = media.takenAt.atZone(zone).toLocalDate()
        return takenOn.year < target.year && displayDay(takenOn, target.year) == MonthDay.from(target)
    }

    /** Garde, dans leur ordre d'origine, les médias de [media] qui correspondent à [target]. */
    fun select(
        media: Iterable<MediaItem>,
        target: LocalDate,
        zone: ZoneId,
    ): List<MediaItem> = media.filter { matches(it, target, zone) }

    /**
     * Plages de prise de vue des médias du jour [target] (SPEC §3.1 et §5.3) : une par année, de
     * [EARLIEST_YEAR] à l'année précédant celle de [target], chacune ouverte à droite
     * (`début..<fin`), dans l'ordre chronologique.
     *
     * Chaque plage va de minuit à minuit dans le fuseau [zone] : un jour de changement d'heure
     * dure donc 23 ou 25 heures. Pour un 28 février d'année non bissextile, la plage d'une année
     * bissextile s'étend jusqu'au 1er mars et couvre aussi son 29 février. Un 29 février ne
     * reprend que les années bissextiles.
     */
    fun rangesFor(
        target: LocalDate,
        zone: ZoneId,
    ): List<OpenEndRange<Instant>> =
        (EARLIEST_YEAR until target.year).mapNotNull { year ->
            val day = MonthDay.from(target)
            when {
                !day.isValidYear(year) -> {
                    null
                }

                day == FEBRUARY_28 && !LEAP_DAY.isValidYear(target.year) && LEAP_DAY.isValidYear(year) -> {
                    dayRange(day.atYear(year), days = 2, zone)
                }

                else -> {
                    dayRange(day.atYear(year), days = 1, zone)
                }
            }
        }

    /** Plage qui couvre [days] jours à partir de [start], de minuit à minuit dans le fuseau [zone]. */
    private fun dayRange(
        start: LocalDate,
        days: Long,
        zone: ZoneId,
    ): OpenEndRange<Instant> =
        start.atStartOfDay(zone).toInstant()..<start.plusDays(days).atStartOfDay(zone).toInstant()

    /**
     * Jour cible de l'année [reviewYear] dont [media] est un souvenir, dans le fuseau [zone] : le
     * même jour, ou le 28 février pour un 29 février d'une année non bissextile (ADR 0006).
     */
    fun reviewDay(
        media: MediaItem,
        reviewYear: Int,
        zone: ZoneId,
    ): LocalDate = displayDay(media.takenAt.atZone(zone).toLocalDate(), reviewYear).atYear(reviewYear)

    /** Jour où le média pris le [takenOn] est affiché pendant l'année [targetYear]. */
    private fun displayDay(
        takenOn: LocalDate,
        targetYear: Int,
    ): MonthDay {
        val day = MonthDay.from(takenOn)
        return if (day == LEAP_DAY && !LEAP_DAY.isValidYear(targetYear)) FEBRUARY_28 else day
    }

    companion object {
        /**
         * Première année cherchée par [rangesFor] : 1970, début de l'heure epoch de MediaStore.
         * Un média daté d'avant (photo scannée dont l'EXIF est ancien) n'est pas proposé (ADR 0008).
         */
        const val EARLIEST_YEAR: Int = 1970

        private val LEAP_DAY: MonthDay = MonthDay.of(Month.FEBRUARY, 29)
        private val FEBRUARY_28: MonthDay = MonthDay.of(Month.FEBRUARY, 28)
    }
}
