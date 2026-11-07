package fr.babeuloula.photoswipe.domain.port

import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Planification du rappel quotidien (SPEC §3.6). Implémenté dans `:data` avec WorkManager.
 *
 * Le rappel ne fait que vérifier, à son heure, s'il reste des médias à trier : la notification
 * n'est envoyée que dans ce cas.
 */
interface ReminderScheduler {
    /** Planifie le rappel chaque jour vers [time], en remplaçant toute planification existante. */
    fun schedule(time: LocalTime)

    /**
     * Planifie un rappel unique à [at], à côté du rappel quotidien qui reste inchangé ; remplace
     * le rappel unique précédent (reprogrammation du rappel d'aujourd'hui).
     */
    fun scheduleOnce(at: LocalDateTime)

    /** Annule le rappel quotidien et le rappel unique ; sans effet s'ils n'étaient pas planifiés. */
    fun cancel()
}
