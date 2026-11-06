package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.port.ReminderNotifier
import kotlinx.coroutines.flow.first

/**
 * Rappel quotidien (SPEC §3.6) : compte les médias à trier sur toute la fenêtre de revue, et
 * n'envoie la notification que s'il en reste, pour ne pas déranger pour rien.
 *
 * Sans accès aux médias, la galerie paraît vide : rien n'est envoyé.
 */
class SendDailyReminder(
    private val getReviewDays: GetReviewDays,
    private val notifier: ReminderNotifier,
) {
    /** Envoie la notification s'il reste des médias à trier ; renvoie leur nombre. */
    suspend operator fun invoke(): Int {
        val toSort = getReviewDays().first().sumOf { it.remainingCount }
        if (toSort > 0) notifier.notify(toSort)
        return toSort
    }
}
