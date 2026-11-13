package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.model.DailyActivity
import fr.babeuloula.photoswipe.domain.port.ActivityHistory
import java.time.LocalDate

/**
 * [ActivityHistory] en mémoire : un total par jour, les suppressions confirmées s'ajoutant au jour
 * donné. Les médias gardés, que le journal Room compte dans sa transaction, se posent ici par
 * [initial].
 */
class FakeActivityHistory(
    initial: List<DailyActivity> = emptyList(),
) : ActivityHistory {
    private val byDay = initial.associateBy { it.day }.toSortedMap()

    /** Jours enregistrés, du plus ancien au plus récent, pour les vérifications du test. */
    val days: List<DailyActivity> get() = byDay.values.toList()

    override suspend fun recordTrashed(
        day: LocalDate,
        count: Int,
        bytes: Long,
    ) {
        val current = byDay[day] ?: DailyActivity(day)
        byDay[day] =
            current.copy(trashedCount = current.trashedCount + count, freedBytes = current.freedBytes + bytes)
    }

    override suspend fun removeTrashed(
        day: LocalDate,
        count: Int,
        bytes: Long,
    ) {
        val current = byDay[day] ?: return
        byDay[day] =
            current.copy(
                trashedCount = (current.trashedCount - count).coerceAtLeast(0),
                freedBytes = (current.freedBytes - bytes).coerceAtLeast(0),
            )
    }

    override suspend fun since(day: LocalDate): List<DailyActivity> = byDay.tailMap(day).values.toList()

    override suspend fun firstDay(): LocalDate? = if (byDay.isEmpty()) null else byDay.firstKey()
}
