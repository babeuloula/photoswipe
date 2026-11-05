package fr.babeuloula.photoswipe.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.ShellScreen
import fr.babeuloula.photoswipe.ui.home.ConfirmationAnnouncement
import fr.babeuloula.photoswipe.ui.home.HomeNavigation
import fr.babeuloula.photoswipe.ui.deck.DeckRoute as DeckDestination
import fr.babeuloula.photoswipe.ui.home.HomeRoute as HomeDestination
import fr.babeuloula.photoswipe.ui.summary.SummaryRoute as SummaryDestination

/**
 * Graphe de navigation de l'application (SPEC §5.4), en routes typées : accueil, pile d'un jour,
 * résumé, réglages. Les écrans pas encore réalisés sont des coquilles, remplacées ticket par ticket.
 *
 * L'accès aux médias n'est pas une destination : `MediaAccessGate` entoure ce graphe, pour qu'un
 * accès retiré ramène à l'explication quel que soit l'écran (ADR 0014).
 *
 */
@Composable
fun PhotoSwipeNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = HomeRoute, modifier = modifier) {
        composable<HomeRoute> { entry ->
            val trashedCount by entry.savedStateHandle.getStateFlow(TRASHED_COUNT_KEY, 0).collectAsState()
            val favoritedCount by entry.savedStateHandle.getStateFlow(FAVORITED_COUNT_KEY, 0).collectAsState()
            HomeDestination(
                navigation =
                    HomeNavigation(
                        openDeck = { day -> navController.navigate(DeckRoute.of(day)) },
                        openSettings = { navController.navigate(SettingsRoute) },
                        openSummary = { navController.navigate(SummaryRoute()) },
                    ),
                confirmed =
                    if (trashedCount > 0 || favoritedCount > 0) {
                        ConfirmationAnnouncement(trashedCount, favoritedCount) {
                            entry.savedStateHandle[TRASHED_COUNT_KEY] = 0
                            entry.savedStateHandle[FAVORITED_COUNT_KEY] = 0
                        }
                    } else {
                        null
                    },
            )
        }
        composable<DeckRoute> {
            DeckDestination(
                onBack = navController::popBackStack,
                onOpenSummary = { keptCount -> navController.navigate(SummaryRoute(keptCount)) },
            )
        }
        composable<SummaryRoute> {
            SummaryDestination(
                onGoHome = { trashedCount, favoritedCount ->
                    // L'accueil annonce ce que la confirmation a changé (option choisie par Jérémy).
                    if (trashedCount > 0 || favoritedCount > 0) {
                        val home = navController.getBackStackEntry(HomeRoute).savedStateHandle
                        home[TRASHED_COUNT_KEY] = trashedCount
                        home[FAVORITED_COUNT_KEY] = favoritedCount
                    }
                    navController.popBackStack(HomeRoute, inclusive = false)
                },
                onBack = navController::popBackStack,
            )
        }
        composable<SettingsRoute> {
            ShellScreen(
                title = stringResource(R.string.settings_title),
                onBack = navController::popBackStack,
                actions = emptyList(),
            )
        }
    }
}

/** Clé du nombre de médias envoyés à la corbeille, transmis du résumé à l'accueil. */
private const val TRASHED_COUNT_KEY = "trashed_count"

/** Clé du nombre de médias ajoutés aux favoris de la galerie, transmis du résumé à l'accueil. */
private const val FAVORITED_COUNT_KEY = "favorited_count"
