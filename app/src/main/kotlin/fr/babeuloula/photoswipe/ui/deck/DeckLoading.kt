package fr.babeuloula.photoswipe.ui.deck

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.usecase.GetDaySession
import fr.babeuloula.photoswipe.domain.usecase.GetDeckForDay
import fr.babeuloula.photoswipe.domain.usecase.GetMarkedDeck
import fr.babeuloula.photoswipe.domain.usecase.GetPreviousDecisions
import fr.babeuloula.photoswipe.domain.usecase.MarkedMedia
import fr.babeuloula.photoswipe.ui.navigation.DeckRoute
import java.time.LocalDate
import javax.inject.Inject

/**
 * Chargement de la pile pour son ViewModel : le paquet d'un jour, son historique d'annulation
 * (ADR 0019) et les décisions précédentes d'un jour retrié (ADR 0026), ou la pile des médias
 * marqués ouverte depuis le résumé (PS-112).
 *
 * @property getDeckForDay paquet des médias à trier.
 * @property getDaySession décisions du jour encore annulables.
 * @property getPreviousDecisions décisions d'avant, à montrer sur les cartes d'un jour rouvert.
 * @property getMarkedDeck pile des médias marqués, ouverte depuis le résumé (PS-112).
 */
class DeckLoading
    @Inject
    constructor(
        val getDeckForDay: GetDeckForDay,
        val getDaySession: GetDaySession,
        val getPreviousDecisions: GetPreviousDecisions,
        val getMarkedDeck: GetMarkedDeck,
    ) {
        /** Pile de [route] : celle de son jour, ou celle des marqués. */
        suspend fun load(route: DeckRoute): LoadedDeck =
            route.day?.let { loadDay(it) } ?: loadMarked(route.firstMarkedMediaId)

        private suspend fun loadDay(day: LocalDate): LoadedDeck {
            val cards = getDeckForDay(day)
            val previous =
                getPreviousDecisions(day, cards.mapTo(HashSet()) { it.id }).mapValues { it.value.decision }
            return LoadedDeck(cards, getDaySession(day), previous)
        }

        /** Le média touché dans le résumé en tête ; chaque carte porte la pastille « marqué ». */
        private suspend fun loadMarked(first: Long?): LoadedDeck {
            val markedMedia = getMarkedDeck(first?.let(::MediaId))
            return LoadedDeck(
                cards = markedMedia.map { it.media },
                session = ReviewSession(),
                previousDecisions = markedMedia.associate { it.id to ReviewDecision.MARKED_FOR_DELETION },
                marked = markedMedia.associateBy { it.id },
            )
        }
    }

/**
 * Pile chargée.
 *
 * @property cards médias à trier, dans l'ordre de la pile.
 * @property session décisions encore annulables.
 * @property previousDecisions décision d'avant à montrer sur chaque carte qui en a une.
 * @property marked pile des marqués : jours de revue des marquages de chaque média ; vide sinon.
 */
data class LoadedDeck(
    val cards: List<MediaItem>,
    val session: ReviewSession,
    val previousDecisions: Map<MediaId, ReviewDecision> = emptyMap(),
    val marked: Map<MediaId, MarkedMedia> = emptyMap(),
)
