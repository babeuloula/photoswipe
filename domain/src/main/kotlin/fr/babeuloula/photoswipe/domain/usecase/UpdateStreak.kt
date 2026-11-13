package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.DayStatus
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.domain.model.Streak
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import java.time.Clock
import java.time.LocalDate

/**
 * Met à jour la série de jours (PS-155) d'après les jours de la fenêtre que l'accueil vient de
 * lire, l'enregistre et la renvoie. Un jour vidé par ses suppressions confirmées compte comme trié.
 */
class UpdateStreak(
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) {
    /** Recompte la série avec [days], aujourd'hui et les jours à rattraper. */
    suspend operator fun invoke(days: List<ReviewDay>): Streak {
        val current = settingsRepository.streakState()
        val updated = current.updated(LocalDate.now(clock), days.associate { it.date to it.status() })
        if (updated != current) settingsRepository.saveStreakState(updated)
        return updated.streak
    }

    private fun ReviewDay.status(): DayStatus =
        when {
            remainingCount > 0 -> DayStatus.PENDING
            memoryCount > 0 || deletedCount > 0 -> DayStatus.SORTED
            else -> DayStatus.EMPTY
        }
}
