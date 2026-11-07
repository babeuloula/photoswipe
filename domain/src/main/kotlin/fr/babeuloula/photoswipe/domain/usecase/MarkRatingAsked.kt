package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.port.SettingsRepository

/** Retient que l'invitation à noter l'application a été montrée : elle ne revient plus (PS-121). */
class MarkRatingAsked(
    private val settingsRepository: SettingsRepository,
) {
    /** Marque l'invitation comme montrée. */
    suspend operator fun invoke() {
        settingsRepository.saveRatingInvitation(settingsRepository.ratingInvitation().copy(asked = true))
    }
}
