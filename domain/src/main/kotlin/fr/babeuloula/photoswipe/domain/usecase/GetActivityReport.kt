package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.ActivityReport
import fr.babeuloula.photoswipe.domain.model.MonthlyActivity
import fr.babeuloula.photoswipe.domain.port.ActivityHistory
import java.time.Clock
import java.time.YearMonth

/**
 * Activité de tri des [MONTHS] derniers mois, mois en cours compris (PS-154) : un total par mois,
 * sans trou, pour les graphiques de la page « Progression ».
 */
class GetActivityReport(
    private val history: ActivityHistory,
    private val clock: Clock,
) {
    /** Lit l'historique et le regroupe par mois. */
    suspend operator fun invoke(): ActivityReport {
        val current = YearMonth.now(clock)
        val months = (MONTHS - 1 downTo 0).map { current.minusMonths(it.toLong()) }
        val byMonth = history.since(months.first().atDay(1)).groupBy { YearMonth.from(it.day) }
        return ActivityReport(
            months =
                months.map { month ->
                    val days = byMonth[month].orEmpty()
                    MonthlyActivity(
                        month = month,
                        keptCount = days.sumOf { it.keptCount },
                        favoriteCount = days.sumOf { it.favoriteCount },
                        trashedCount = days.sumOf { it.trashedCount },
                        freedBytes = days.sumOf { it.freedBytes },
                    )
                },
            since = history.firstDay(),
        )
    }

    companion object {
        /** Nombre de mois montrés par les graphiques. */
        const val MONTHS: Int = 6
    }
}
