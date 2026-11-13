package fr.babeuloula.photoswipe.ui.whatsnew

import fr.babeuloula.photoswipe.domain.usecase.CheckWhatsNew
import fr.babeuloula.photoswipe.domain.usecase.MarkWhatsNewSeen

/**
 * Ce que l'accueil utilise pour annoncer les nouveautés après une mise à jour (PS-173), fourni par
 * `di/WhatsNewModule`.
 *
 * @property check versions dont les nouveautés n'ont pas été vues.
 * @property markSeen retient que la feuille a été fermée.
 * @property releases nouveautés de chaque version, [WHATS_NEW] dans l'application.
 */
class WhatsNewActions(
    val check: CheckWhatsNew,
    val markSeen: MarkWhatsNewSeen,
    val releases: List<WhatsNewRelease>,
) {
    /** Nouveautés à annoncer, de la plus récente à la plus ancienne. */
    suspend fun missed(): List<WhatsNewRelease> {
        val versions = check()
        return releases.filter { it.version in versions }.sortedByDescending { it.version }
    }
}
