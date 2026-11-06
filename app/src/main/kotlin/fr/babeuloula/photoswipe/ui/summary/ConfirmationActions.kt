package fr.babeuloula.photoswipe.ui.summary

import fr.babeuloula.photoswipe.domain.usecase.PrepareFavorites
import fr.babeuloula.photoswipe.domain.usecase.PrepareTrash
import fr.babeuloula.photoswipe.domain.usecase.RecordFavoriteResult
import fr.babeuloula.photoswipe.domain.usecase.RecordTrashResult
import fr.babeuloula.photoswipe.permission.OrphanCleanup
import javax.inject.Inject

/**
 * Use cases de la confirmation de fin de session, regroupés pour le ViewModel du résumé :
 * préparer puis enregistrer les dialogues de corbeille, puis ceux des favoris (SPEC §3.5), et
 * nettoyer les orphelins après une suppression (SPEC §6).
 *
 * @property prepareTrash prépare les dialogues de corbeille, par lots.
 * @property recordTrashResult enregistre la réponse à un dialogue de corbeille.
 * @property prepareFavorites prépare les dialogues de favoris, par lots.
 * @property recordFavoriteResult enregistre la réponse à un dialogue de favoris.
 * @property orphanCleanup nettoie les décisions des médias disparus.
 */
class ConfirmationActions
    @Inject
    constructor(
        val prepareTrash: PrepareTrash,
        val recordTrashResult: RecordTrashResult,
        val prepareFavorites: PrepareFavorites,
        val recordFavoriteResult: RecordFavoriteResult,
        val orphanCleanup: OrphanCleanup,
    )
