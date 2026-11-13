package fr.babeuloula.photoswipe.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** État d'un jour de revue pour la série (PS-155). */
enum class DayStatus {
    /** Le jour avait des souvenirs et tous sont triés. */
    SORTED,

    /** Le jour n'a aucun souvenir : il ne casse pas la série, sans pouvoir en commencer une. */
    EMPTY,

    /** Il reste des souvenirs à trier ce jour-là. */
    PENDING,
}

/**
 * Série de jours sans souvenir en retard (PS-155), comptée en jours du calendrier.
 *
 * @property current série en cours ; 0 s'il n'y en a pas.
 * @property best plus longue série atteinte.
 */
data class Streak(
    val current: Int = 0,
    val best: Int = 0,
)

/**
 * Ce que l'application retient de la série entre deux ouvertures (PS-155).
 *
 * Une série commence par un jour entièrement trié et continue tant que chaque jour suivant est
 * trié ou sans souvenir. Aujourd'hui, pas encore trié, ne la casse pas : elle s'arrête alors à hier.
 * Les jours de la fenêtre de rattrapage sont recomptés à chaque fois : rattraper un jour manqué
 * répare la série. Un jour sorti de la fenêtre garde le dernier état connu ; un jour jamais vu
 * (application fermée pendant toute la fenêtre) la casse.
 *
 * @property runStart premier jour de la série qui atteint [settledThrough], `null` sans série.
 * @property settledThrough dernier jour sorti de la fenêtre et donc figé, `null` avant la première mise à jour.
 * @property recent état des jours de la fenêtre à la dernière mise à jour.
 * @property streak série en cours et meilleure série, à la dernière mise à jour.
 */
data class StreakState(
    val runStart: LocalDate? = null,
    val settledThrough: LocalDate? = null,
    val recent: Map<LocalDate, DayStatus> = emptyMap(),
    val streak: Streak = Streak(),
) {
    /**
     * État après avoir vu la fenêtre [window] le jour [today]. [window] contient [today] et les
     * jours à rattraper ; sans jour, rien ne change.
     */
    fun updated(
        today: LocalDate,
        window: Map<LocalDate, DayStatus>,
    ): StreakState {
        val oldest = window.keys.minOrNull() ?: return this
        // Les jours sortis de la fenêtre depuis la dernière fois sont figés dans leur dernier état connu.
        var start = runStart
        var day = settledThrough?.plusDays(1) ?: oldest
        while (day < oldest) {
            if (recent.keys.none { it >= day }) {
                // Plus aucun jour connu jusqu'à la fenêtre : la série est cassée, inutile de les parcourir.
                start = null
                break
            }
            start = start.after(day, recent[day] ?: DayStatus.PENDING)
            day = day.plusDays(1)
        }
        val settledStart = start
        // Les jours de la fenêtre, du plus ancien à aujourd'hui.
        var end = oldest.minusDays(1)
        var live = start
        window.toSortedMap().forEach { (date, status) ->
            if (date == today && status == DayStatus.PENDING) return@forEach
            live = live.after(date, status)
            end = date
        }
        val current = live?.let { ChronoUnit.DAYS.between(it, end).toInt() + 1 }?.coerceAtLeast(0) ?: 0
        return StreakState(
            runStart = settledStart,
            settledThrough = oldest.minusDays(1),
            recent = window,
            streak = Streak(current = current, best = maxOf(streak.best, current)),
        )
    }

    /** Début de la série après le jour [day] d'état [status], connaissant le début d'avant. */
    private fun LocalDate?.after(
        day: LocalDate,
        status: DayStatus,
    ): LocalDate? =
        when (status) {
            DayStatus.SORTED -> this ?: day
            DayStatus.EMPTY -> this
            DayStatus.PENDING -> null
        }
}
