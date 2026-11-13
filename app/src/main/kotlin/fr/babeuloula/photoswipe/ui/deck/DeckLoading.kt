package fr.babeuloula.photoswipe.ui.deck

import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.ReviewSession
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
import fr.babeuloula.photoswipe.domain.usecase.GetDaySession
import fr.babeuloula.photoswipe.domain.usecase.GetDeckForDay
import fr.babeuloula.photoswipe.domain.usecase.GetMarkedDeck
import fr.babeuloula.photoswipe.domain.usecase.GetPreviousDecisions
import fr.babeuloula.photoswipe.domain.usecase.GetQualityHints
import fr.babeuloula.photoswipe.domain.usecase.GetSimilarMemories
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
 * @property getSimilarMemories photos du jour prises à la suite (PS-140).
 * @property getMarkedDeck pile des médias marqués, ouverte depuis le résumé (PS-112).
 * @property getQualityHints photos peut-être floues ou très sombres (PS-164), mesurées après
 * l'affichage de la pile : la première mesure d'une photo lit sa vignette.
 */
class DeckLoading
    @Inject
    constructor(
        val getDeckForDay: GetDeckForDay,
        val getDaySession: GetDaySession,
        val getPreviousDecisions: GetPreviousDecisions,
        val getMarkedDeck: GetMarkedDeck,
        val getSimilarMemories: GetSimilarMemories,
        val getQualityHints: GetQualityHints,
    ) {
        /** Pile de [route] : celle de son jour, ou celle des marqués. */
        suspend fun load(route: DeckRoute): LoadedDeck =
            route.day?.let { loadDay(it, route.firstMediaId) } ?: loadMarked(route.firstMediaId)

        /** Pile du jour [day] ; le média [first], touché dans la vue d'ensemble, passe en tête (PS-139). */
        private suspend fun loadDay(
            day: LocalDate,
            first: Long?,
        ): LoadedDeck {
            val deck = getDeckForDay(day)
            val cards = deck.sortedByDescending { it.id.value == first }
            val previous =
                getPreviousDecisions(day, cards.mapTo(HashSet()) { it.id }).mapValues { it.value.decision }
            // Les photos proches ne sont qu'une aide : sans elles, la pile reste utilisable.
            val similar = runCatching { getSimilarMemories(day) }.getOrDefault(emptyMap())
            return LoadedDeck(cards, getDaySession(day), previous, similar = similar)
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
 * @property similar place des photos prises à la suite (PS-140) ; vide pour la pile des marqués.
 */
data class LoadedDeck(
    val cards: List<MediaItem>,
    val session: ReviewSession,
    val previousDecisions: Map<MediaId, ReviewDecision> = emptyMap(),
    val marked: Map<MediaId, MarkedMedia> = emptyMap(),
    val similar: Map<MediaId, SimilarGroup> = emptyMap(),
)
