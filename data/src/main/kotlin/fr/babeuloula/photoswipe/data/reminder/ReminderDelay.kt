package fr.babeuloula.photoswipe.data.reminder

import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * Délai jusqu'au prochain rappel à [time], dans le fuseau de [clock] : aujourd'hui si l'heure est
 * encore à venir, sinon demain. Calculé sur la date locale, il suit les changements d'heure : la
 * veille du passage à l'heure d'été, le délai jusqu'à 18:00 le lendemain compte une heure de moins.
 */
internal fun delayUntilNext(
    time: LocalTime,
    clock: Clock,
): Duration = Duration.between(ZonedDateTime.now(clock), nextOccurrence(time, clock))

/**
 * Prochaine occurrence de [time] dans le fuseau de [clock] : aujourd'hui si elle est encore à
 * venir, sinon demain. Heure absolue, à la milliseconde près : une alarme exacte réglée dessus ne
 * sonne pas une milliseconde avant, ce qui replanifierait le même jour (PS-125).
 */
internal fun nextOccurrence(
    time: LocalTime,
    clock: Clock,
): ZonedDateTime {
    val now = ZonedDateTime.now(clock)
    val today = now.with(time)
    return if (today.isAfter(now)) {
        today
    } else {
        now
            .toLocalDate()
            .plusDays(1)
            .atTime(time)
            .atZone(clock.zone)
    }
}
