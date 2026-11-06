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
): Duration {
    val now = ZonedDateTime.now(clock)
    val today = now.with(time)
    val next =
        if (today.isAfter(now)) {
            today
        } else {
            now
                .toLocalDate()
                .plusDays(1)
                .atTime(time)
                .atZone(clock.zone)
        }
    return Duration.between(now, next)
}
