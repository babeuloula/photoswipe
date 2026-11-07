package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.port.ReminderScheduler
import java.time.Clock
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Reprogramme le rappel d'aujourd'hui à une autre heure (SPEC §3.6), depuis la notification :
 * un rappel unique est planifié aujourd'hui à cette heure, le rappel quotidien ne change pas.
 * Comme le rappel quotidien, il ne s'affiche que s'il reste des médias à trier.
 */
class SnoozeReminderToday(
    private val reminderScheduler: ReminderScheduler,
    private val clock: Clock,
) {
    /** Résultat d'une reprogrammation. */
    enum class Result {
        /** Le rappel est planifié aujourd'hui à l'heure choisie. */
        SCHEDULED,

        /** L'heure choisie est déjà passée aujourd'hui : rien n'est planifié. */
        TIME_PASSED,
    }

    /** Heure proposée par défaut : dans une heure, à la minute, sans dépasser la fin de la journée. */
    fun suggestedTime(): LocalTime {
        val now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES)
        val inOneHour = now.plusHours(1)
        return if (inOneHour.toLocalDate() == now.toLocalDate()) inOneHour.toLocalTime() else LAST_MINUTE
    }

    /** Planifie le rappel aujourd'hui à [time], s'il est encore à venir. */
    operator fun invoke(time: LocalTime): Result {
        val now = LocalDateTime.now(clock)
        val at = now.toLocalDate().atTime(time)
        if (!at.isAfter(now)) return Result.TIME_PASSED
        reminderScheduler.scheduleOnce(at)
        return Result.SCHEDULED
    }

    private companion object {
        val LAST_MINUTE: LocalTime = LocalTime.of(23, 59)
    }
}
