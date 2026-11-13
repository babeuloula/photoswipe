package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.Streak
import fr.babeuloula.photoswipe.domain.port.SettingsRepository

/**
 * Série de jours telle que l'accueil l'a comptée la dernière fois (PS-155), pour la page
 * « Progression », qui s'ouvre toujours après lui.
 */
class GetStreak(
    private val settingsRepository: SettingsRepository,
) {
    /** Lit la série enregistrée. */
    suspend operator fun invoke(): Streak = settingsRepository.streakState().streak
}
