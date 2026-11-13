package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.ReleaseVersion
import fr.babeuloula.photoswipe.domain.port.SettingsRepository

/**
 * Retient que les nouveautés de la version installée ont été vues (PS-173) : la feuille fermée ne
 * revient plus. Une version plus ancienne que celle déjà enregistrée (retour en arrière, build de
 * développement) ne la remplace pas, pour ne pas montrer de nouveau ce qui a été vu.
 *
 * @param currentVersionName nom de la version installée (`versionName`).
 */
class MarkWhatsNewSeen(
    private val settingsRepository: SettingsRepository,
    private val currentVersionName: String,
) {
    /** Enregistre la version installée comme vue, si elle est plus récente. */
    suspend operator fun invoke() {
        val current = ReleaseVersion.parse(currentVersionName) ?: return
        val seen = settingsRepository.whatsNewVersionSeen()?.let(ReleaseVersion::parse)
        if (seen == null || current > seen) settingsRepository.saveWhatsNewVersionSeen(current.toString())
    }
}
