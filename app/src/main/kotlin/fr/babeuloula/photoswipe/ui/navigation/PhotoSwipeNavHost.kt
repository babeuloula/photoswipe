package fr.babeuloula.photoswipe.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.ShellScreen
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
        composable<HomeRoute> {
            HomeDestination(
                onOpenDeck = { day -> navController.navigate(DeckRoute.of(day)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenSummary = { navController.navigate(SummaryRoute()) },
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
                onGoHome = { navController.popBackStack(HomeRoute, inclusive = false) },
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
