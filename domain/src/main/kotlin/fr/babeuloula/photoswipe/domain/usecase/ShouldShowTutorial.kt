package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.Tutorial
import fr.babeuloula.photoswipe.domain.port.SettingsRepository

/**
 * Dit s'il faut montrer le tutoriel au lancement (SPEC §4) : jamais vu, ou vu dans une version
 * plus ancienne que [currentVersion].
 *
 * @param currentVersion version du contenu du tutoriel, [Tutorial.CURRENT_VERSION] dans l'app.
 */
class ShouldShowTutorial(
    private val settingsRepository: SettingsRepository,
    private val currentVersion: Int = Tutorial.CURRENT_VERSION,
) {
    /** Vrai si la version vue est inférieure à la version courante. */
    suspend operator fun invoke(): Boolean = settingsRepository.tutorialVersionSeen() < currentVersion
}
