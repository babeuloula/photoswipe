package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.port.ReminderNotifier

/** [ReminderNotifier] en mémoire : retient chaque notification envoyée, dans l'ordre. */
class FakeReminderNotifier : ReminderNotifier {
    /** Nombres de médias annoncés par chaque notification envoyée. */
    val sent: List<Int> get() = notifications.toList()

    private val notifications = mutableListOf<Int>()

    override fun notify(toSortCount: Int) {
        require(toSortCount > 0) { "a reminder must announce media to sort: $toSortCount" }
        notifications += toSortCount
    }
}
