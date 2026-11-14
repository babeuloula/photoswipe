package fr.babeuloula.photoswipe.ui.summary

import fr.babeuloula.photoswipe.domain.usecase.OfferManageMediaTip
import fr.babeuloula.photoswipe.domain.usecase.PrepareTrash
import fr.babeuloula.photoswipe.domain.usecase.RecordTrashResult
import fr.babeuloula.photoswipe.permission.MediaManagementReader
import fr.babeuloula.photoswipe.permission.OrphanCleanup
import fr.babeuloula.photoswipe.platform.PlatformFeatures
import fr.babeuloula.photoswipe.ui.common.DirectDeletion
import fr.babeuloula.photoswipe.ui.common.MediaActionIntents
import javax.inject.Inject

/**
 * Use cases de la confirmation de fin de session, regroupés pour le ViewModel du résumé :
 * préparer puis enregistrer les dialogues de corbeille (SPEC §3.5), et
 * nettoyer les orphelins après une suppression (SPEC §6).
 *
 * @property prepareTrash prépare les dialogues de corbeille, par lots.
 * @property recordTrashResult enregistre la réponse à un dialogue de corbeille.
 * @property orphanCleanup nettoie les décisions des médias disparus.
 * @property offerManageMediaTip propose l'accès « Gestion des médias » après un dialogue d'Android validé.
 */
class ConfirmationActions
    @Inject
    constructor(
        val prepareTrash: PrepareTrash,
        val recordTrashResult: RecordTrashResult,
        val orphanCleanup: OrphanCleanup,
        val offerManageMediaTip: OfferManageMediaTip,
    )

/**
 * Ce que le résumé sait de la galerie d'Android pour la confirmation, regroupé pour son ViewModel.
 *
 * @property mediaActionIntents dialogue d'Android de chaque demande de corbeille (ADR 0018).
 * @property mediaManagement accès « Gestion des médias » (ADR 0023).
 * @property directDeletion suppressions définitives d'Android 10, appliquées sans dialogue (PS-184).
 * @property platform fonctions d'Android du téléphone : sans corbeille, la suppression est définitive.
 */
class SummaryGallery
    @Inject
    constructor(
        val mediaActionIntents: MediaActionIntents,
        val mediaManagement: MediaManagementReader,
        val directDeletion: DirectDeletion,
        val platform: PlatformFeatures,
    )
