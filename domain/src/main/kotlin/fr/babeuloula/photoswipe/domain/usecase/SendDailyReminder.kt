package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.port.ReminderNotifier
import kotlinx.coroutines.flow.first

/**
 * Rappel quotidien (SPEC §3.6) : compte les médias à trier sur toute la fenêtre de revue, et
 * n'envoie la notification que s'il en reste, pour ne pas déranger pour rien. Un appui ouvre la
 * pile du jour le plus récent qui a encore des médias à trier : aujourd'hui, sinon le rattrapage.
 *
 * Sans accès aux médias, la galerie paraît vide : rien n'est envoyé.
 */
class SendDailyReminder(
    private val getReviewDays: GetReviewDays,
    private val notifier: ReminderNotifier,
) {
    /** Envoie la notification s'il reste des médias à trier ; renvoie leur nombre. */
    suspend operator fun invoke(): Int {
        val days = getReviewDays().first()
        val toSort = days.sumOf { it.remainingCount }
        // Jours d'aujourd'hui au plus ancien : la pile ouverte est la plus récente qui reste à trier.
        days.firstOrNull { it.remainingCount > 0 }?.let { notifier.notify(toSort, it.date) }
        return toSort
    }
}
