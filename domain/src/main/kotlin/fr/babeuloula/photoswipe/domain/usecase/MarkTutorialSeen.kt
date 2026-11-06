package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.Tutorial
import fr.babeuloula.photoswipe.domain.port.SettingsRepository

/**
 * Retient que le tutoriel a été vu, terminé ou passé (SPEC §4), dans sa version [currentVersion] :
 * il ne revient plus tant que cette version ne change pas.
 *
 * @param currentVersion version du contenu du tutoriel, [Tutorial.CURRENT_VERSION] dans l'app.
 */
class MarkTutorialSeen(
    private val settingsRepository: SettingsRepository,
    private val currentVersion: Int = Tutorial.CURRENT_VERSION,
) {
    /** Enregistre [currentVersion] comme version vue. */
    suspend operator fun invoke() {
        settingsRepository.saveTutorialVersionSeen(currentVersion)
    }
}
