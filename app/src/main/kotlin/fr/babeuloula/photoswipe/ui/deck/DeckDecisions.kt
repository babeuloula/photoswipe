package fr.babeuloula.photoswipe.ui.deck

import fr.babeuloula.photoswipe.domain.usecase.FavoriteMedia
import fr.babeuloula.photoswipe.domain.usecase.KeepMedia
import fr.babeuloula.photoswipe.domain.usecase.MarkForDeletion
import fr.babeuloula.photoswipe.domain.usecase.UndoLastDecision
import javax.inject.Inject

/**
 * Use cases de décision de la pile, regroupés pour son ViewModel : garder, marquer pour suppression,
 * mettre en favori, annuler la dernière décision (SPEC §2, §3.4).
 */
class DeckDecisions
    @Inject
    constructor(
        val keepMedia: KeepMedia,
        val markForDeletion: MarkForDeletion,
        val favoriteMedia: FavoriteMedia,
        val undoLastDecision: UndoLastDecision,
    )
