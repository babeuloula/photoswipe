package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import java.time.Clock
import java.time.LocalDate

/**
 * Retient que la pile d'aujourd'hui est entièrement triée (PS-121) : un jour de plus vers
 * l'invitation à noter l'application, un même jour ne comptant qu'une fois (retrier ne compte pas).
 * Renvoie vrai s'il est temps de montrer l'invitation.
 */
class RecordTodaySorted(
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) {
    /** Compte aujourd'hui et dit si l'invitation est due. */
    suspend operator fun invoke(): Boolean {
        val current = settingsRepository.ratingInvitation()
        val updated = current.afterSorting(LocalDate.now(clock))
        if (updated != current) settingsRepository.saveRatingInvitation(updated)
        return updated.due
    }
}
