package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.port.ReminderScheduler
import java.time.LocalTime

/**
 * [ReminderScheduler] en mémoire : retient l'heure planifiée, comme une tâche WorkManager unique
 * qu'une nouvelle planification remplace.
 */
class FakeReminderScheduler : ReminderScheduler {
    /** Heure du rappel planifié, `null` si aucun rappel n'est planifié. */
    var scheduledTime: LocalTime? = null
        private set

    override fun schedule(time: LocalTime) {
        scheduledTime = time
    }

    override fun cancel() {
        scheduledTime = null
    }
}
