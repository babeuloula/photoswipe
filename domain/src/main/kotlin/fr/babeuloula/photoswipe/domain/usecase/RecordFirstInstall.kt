package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.ReleaseVersion
import fr.babeuloula.photoswipe.domain.model.Tutorial
import fr.babeuloula.photoswipe.domain.port.SettingsRepository

/**
 * Première installation (PS-173) : aucune version des nouveautés n'est enregistrée et le tutoriel
 * n'a jamais été vu. La version installée est alors retenue comme vue, pour ne rien annoncer à qui
 * découvre l'application.
 *
 * Un utilisateur qui a déjà vu le tutoriel mais n'a aucune version enregistrée vient d'une version
 * qui ne l'enregistrait pas encore : ce n'est pas une première installation, il verra les
 * nouveautés. C'est pourquoi l'écran du tutoriel lance ce use case avant de le marquer comme vu.
 *
 * @param currentVersionName nom de la version installée (`versionName`).
 */
class RecordFirstInstall(
    private val settingsRepository: SettingsRepository,
    private val currentVersionName: String,
) {
    /** Vrai s'il s'agit d'une première installation, dont la version vient d'être enregistrée. */
    suspend operator fun invoke(): Boolean {
        val firstInstall =
            settingsRepository.whatsNewVersionSeen() == null &&
                settingsRepository.tutorialVersionSeen() == Tutorial.NEVER_SEEN
        if (firstInstall) {
            ReleaseVersion.parse(currentVersionName)?.let { settingsRepository.saveWhatsNewVersionSeen(it.toString()) }
        }
        return firstInstall
    }
}
