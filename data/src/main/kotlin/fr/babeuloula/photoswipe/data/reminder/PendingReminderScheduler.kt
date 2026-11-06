package fr.babeuloula.photoswipe.data.reminder

import fr.babeuloula.photoswipe.domain.port.ReminderScheduler
import java.time.LocalTime
import javax.inject.Inject

/**
 * [ReminderScheduler] provisoire : ne planifie rien. Les réglages du rappel sont déjà enregistrés
 * (PS-59) ; la tâche WorkManager quotidienne qui le déclenchera la remplace avec PS-61, qui
 * replanifiera alors le rappel à partir des réglages enregistrés.
 */
class PendingReminderScheduler
    @Inject
    constructor() : ReminderScheduler {
        override fun schedule(time: LocalTime) = Unit

        override fun cancel() = Unit
    }
