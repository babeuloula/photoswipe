package fr.babeuloula.photoswipe.domain.usecase

import fr.babeuloula.photoswipe.domain.model.ManageMediaTip
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Propose l'accès « Gestion des médias » sur l'accueil après qu'un dialogue d'Android a été validé
 * (corbeille ou favori, SPEC §3.5). Un conseil déjà proposé ou fermé par l'utilisateur ne change pas.
 */
class OfferManageMediaTip(
    private val settingsRepository: SettingsRepository,
) {
    /** Passe le conseil à [ManageMediaTip.OFFERED] s'il n'a jamais été proposé. */
    suspend operator fun invoke() {
        if (settingsRepository.observeManageMediaTip().first() == ManageMediaTip.NOT_OFFERED) {
            settingsRepository.saveManageMediaTip(ManageMediaTip.OFFERED)
        }
    }
}
