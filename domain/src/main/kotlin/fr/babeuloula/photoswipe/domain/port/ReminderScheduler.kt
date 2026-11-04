package fr.babeuloula.photoswipe.domain.port

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

    /** Annule le rappel ; sans effet s'il n'était pas planifié. */
    fun cancel()
}
