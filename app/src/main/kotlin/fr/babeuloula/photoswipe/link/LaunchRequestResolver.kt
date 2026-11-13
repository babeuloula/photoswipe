package fr.babeuloula.photoswipe.link

import fr.babeuloula.photoswipe.domain.model.DayOverview
import fr.babeuloula.photoswipe.domain.usecase.GetDayOverview
import fr.babeuloula.photoswipe.ui.navigation.DayOverviewRoute
import fr.babeuloula.photoswipe.ui.navigation.DeckRoute
import fr.babeuloula.photoswipe.ui.navigation.HomeRoute
import fr.babeuloula.photoswipe.ui.navigation.SummaryRoute
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Traduit une [LaunchRequest] en route de navigation (ADR 0038), une fois l'accès aux médias
 * accordé : la galerie est relue pour ne pas ouvrir un écran vide.
 *
 * - la pile du jour s'ouvre s'il reste un souvenir à trier aujourd'hui, sinon l'accueil ;
 * - la vue d'ensemble d'une date s'ouvre si ce jour a au moins un souvenir, sinon l'accueil ;
 * - le résumé et la pile demandée par le rappel s'ouvrent toujours ;
 * - une galerie illisible mène à l'accueil.
 *
 * @param clock horloge de l'appareil : elle donne le jour courant.
 */
class LaunchRequestResolver
    @Inject
    constructor(
        private val getDayOverview: GetDayOverview,
        private val clock: Clock,
    ) {
        /** Route à ouvrir pour [request] : [HomeRoute] pour l'accueil. */
        suspend fun routeFor(request: LaunchRequest): Any =
            when (request) {
                LaunchRequest.Home -> HomeRoute
                LaunchRequest.Summary -> SummaryRoute
                is LaunchRequest.Deck -> DeckRoute.of(request.day)
                LaunchRequest.TodayDeck -> todayDeck()
                is LaunchRequest.DayOverview -> dayOverview(request.day)
            }

        private suspend fun todayDeck(): Any {
            val today = LocalDate.now(clock)
            val toSort = overviewOf(today)?.years?.any { year -> year.memories.any { it.decision == null } } == true
            return if (toSort) DeckRoute.of(today) else HomeRoute
        }

        private suspend fun dayOverview(day: LocalDate): Any =
            if ((overviewOf(day)?.memoryCount ?: 0) > 0) DayOverviewRoute.of(day) else HomeRoute

        /** Souvenirs du jour [day], `null` si la galerie ou la base n'a pas pu être lue. */
        private suspend fun overviewOf(day: LocalDate): DayOverview? {
            val overview = runCatching { getDayOverview(day) }.getOrNull()
            // runCatching attrape aussi l'annulation : elle doit rester une annulation.
            currentCoroutineContext().ensureActive()
            return overview
        }
    }
