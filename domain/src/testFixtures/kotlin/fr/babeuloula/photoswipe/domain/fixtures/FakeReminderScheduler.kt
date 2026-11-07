package fr.babeuloula.photoswipe.domain.fixtures

import fr.babeuloula.photoswipe.domain.port.ReminderScheduler
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * [ReminderScheduler] en mémoire : retient l'heure planifiée, comme une tâche WorkManager unique
 * qu'une nouvelle planification remplace, et le rappel unique à part.
 */
class FakeReminderScheduler : ReminderScheduler {
    /** Heure du rappel planifié, `null` si aucun rappel n'est planifié. */
    var scheduledTime: LocalTime? = null
        private set

    /** Moment du rappel unique, `null` s'il n'y en a pas. */
    var onceAt: LocalDateTime? = null
        private set

    override fun schedule(time: LocalTime) {
        scheduledTime = time
    }

    override fun scheduleOnce(at: LocalDateTime) {
        onceAt = at
    }

    override fun cancel() {
        scheduledTime = null
        onceAt = null
    }
}
