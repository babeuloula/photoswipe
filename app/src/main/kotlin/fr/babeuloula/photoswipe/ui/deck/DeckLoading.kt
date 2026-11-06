package fr.babeuloula.photoswipe.ui.deck

import fr.babeuloula.photoswipe.domain.usecase.GetDaySession
import fr.babeuloula.photoswipe.domain.usecase.GetDeckForDay
import fr.babeuloula.photoswipe.domain.usecase.GetPreviousDecisions
import javax.inject.Inject

/**
 * Use cases de chargement de la pile, regroupés pour son ViewModel : le paquet du jour, son
 * historique d'annulation (ADR 0019) et les décisions précédentes d'un jour retrié (ADR 0026).
 *
 * @property getDeckForDay paquet des médias à trier.
 * @property getDaySession décisions du jour encore annulables.
 * @property getPreviousDecisions décisions d'avant, à montrer sur les cartes d'un jour rouvert.
 */
class DeckLoading
    @Inject
    constructor(
        val getDeckForDay: GetDeckForDay,
        val getDaySession: GetDaySession,
        val getPreviousDecisions: GetPreviousDecisions,
    )
