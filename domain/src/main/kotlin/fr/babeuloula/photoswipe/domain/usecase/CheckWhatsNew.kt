package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.ReleaseVersion
import fr.babeuloula.photoswipe.domain.port.SettingsRepository

/**
 * Versions dont l'utilisateur n'a pas encore vu les nouveautés (PS-173), de la plus récente à la
 * plus ancienne : celles qui en ont, plus récentes que la dernière version vue et pas plus récentes
 * que la version installée.
 *
 * À la première installation ([RecordFirstInstall]), rien n'est annoncé. Sans version vue enregistrée
 * mais avec le tutoriel déjà vu, l'utilisateur vient d'une version qui ne l'enregistrait pas : toutes
 * les nouveautés jusqu'à la version installée sont annoncées. Sans rien à annoncer, la version
 * installée est retenue tout de suite ; sinon, elle l'est quand la feuille se ferme
 * ([MarkWhatsNewSeen]).
 *
 * @param currentVersionName nom de la version installée (`versionName`), un build de développement
 * valant la dernière version publiée qu'il contient ([ReleaseVersion.parse]).
 * @param releasesWithNews versions qui ont des nouveautés, dans n'importe quel ordre.
 */
class CheckWhatsNew(
    private val settingsRepository: SettingsRepository,
    private val currentVersionName: String,
    private val releasesWithNews: List<ReleaseVersion>,
) {
    private val recordFirstInstall = RecordFirstInstall(settingsRepository, currentVersionName)
    private val markSeen = MarkWhatsNewSeen(settingsRepository, currentVersionName)

    /** Versions à annoncer ; vide s'il n'y a rien de nouveau. */
    suspend operator fun invoke(): List<ReleaseVersion> {
        val current = ReleaseVersion.parse(currentVersionName)
        if (current == null || recordFirstInstall()) return emptyList()
        val seen = settingsRepository.whatsNewVersionSeen()?.let(ReleaseVersion::parse)
        val missed =
            releasesWithNews
                .filter { (seen == null || it > seen) && it <= current }
                .distinct()
                .sortedDescending()
        if (missed.isEmpty()) markSeen()
        return missed
    }
}
