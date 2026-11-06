package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.port.ReminderNotifier
import java.time.LocalDate

/** [ReminderNotifier] en mémoire : retient chaque notification envoyée, dans l'ordre. */
class FakeReminderNotifier : ReminderNotifier {
    /** Notification envoyée : le nombre de médias annoncés et le jour dont la pile s'ouvre. */
    data class Sent(
        val toSortCount: Int,
        val dayToSort: LocalDate,
    )

    /** Notifications envoyées. */
    val sent: List<Sent> get() = notifications.toList()

    private val notifications = mutableListOf<Sent>()

    override fun notify(
        toSortCount: Int,
        dayToSort: LocalDate,
    ) {
        require(toSortCount > 0) { "a reminder must announce media to sort: $toSortCount" }
        notifications += Sent(toSortCount, dayToSort)
    }
}
