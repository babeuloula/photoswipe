package fr.babeuloula.photoswipe.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.babeuloula.photoswipe.ui.home.ConfirmationAnnouncement
import fr.babeuloula.photoswipe.ui.home.HomeNavigation
import fr.babeuloula.photoswipe.ui.licenses.LicensesScreen
import java.time.LocalDate
import fr.babeuloula.photoswipe.ui.deck.DeckRoute as DeckDestination
import fr.babeuloula.photoswipe.ui.home.HomeRoute as HomeDestination
import fr.babeuloula.photoswipe.ui.settings.SettingsRoute as SettingsDestination
import fr.babeuloula.photoswipe.ui.summary.SummaryRoute as SummaryDestination

/**
 * Graphe de navigation de l'application (SPEC §5.4), en routes typées : accueil, pile d'un jour,
 * résumé, réglages, licences. Le rappel quotidien peut demander d'ouvrir directement une pile
 * ([deckToOpen]), qui s'empile au-dessus de l'accueil. Les écrans s'enchaînent en axe partagé
 * horizontal ([SharedAxisTransitions]).
 *
 * L'accès aux médias n'est pas une destination : `MediaAccessGate` entoure ce graphe, pour qu'un
 * accès retiré ramène à l'explication quel que soit l'écran (ADR 0014).
 *
 */
@Composable
fun PhotoSwipeNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    deckToOpen: LocalDate? = null,
    onDeckOpened: () -> Unit = {},
) {
    OpenRequestedDeck(navController, deckToOpen, onDeckOpened)
    val transitions = rememberSharedAxisTransitions()
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = { transitions.enter },
        exitTransition = { transitions.exit },
        popEnterTransition = { transitions.popEnter },
        popExitTransition = { transitions.popExit },
    ) {
        homeDestination(navController)
        composable<DeckRoute> {
            DeckDestination(
                onBack = navController::popBackStack,
                onOpenSummary = { navController.navigate(SummaryRoute) },
            )
        }
        composable<SummaryRoute> {
            SummaryDestination(
                onGoHome = { trashedCount ->
                    // L'accueil annonce ce que la confirmation a changé (option choisie par Jérémy).
                    if (trashedCount > 0) {
                        navController.getBackStackEntry(HomeRoute).savedStateHandle[TRASHED_COUNT_KEY] = trashedCount
                    }
                    navController.popBackStack(HomeRoute, inclusive = false)
                },
                onBack = navController::popBackStack,
            )
        }
        composable<SettingsRoute> {
            SettingsDestination(
                onBack = navController::popBackStack,
                onOpenLicenses = { navController.navigate(LicensesRoute) },
            )
        }
        composable<LicensesRoute> {
            LicensesScreen(onBack = navController::popBackStack)
        }
    }
}

/** Clé du nombre de médias envoyés à la corbeille, transmis du résumé à l'accueil. */
private const val TRASHED_COUNT_KEY = "trashed_count"

/** Notification du rappel : la pile [deckToOpen] s'ouvre au-dessus de l'accueil, que « Retour » retrouve. */
@Composable
private fun OpenRequestedDeck(
    navController: NavHostController,
    deckToOpen: LocalDate?,
    onDeckOpened: () -> Unit,
) {
    LaunchedEffect(deckToOpen) {
        if (deckToOpen != null) {
            navController.navigate(DeckRoute.of(deckToOpen)) { launchSingleTop = true }
            onDeckOpened()
        }
    }
}

/** Accueil : il ouvre les autres écrans et annonce ce que la confirmation du résumé a changé. */
private fun NavGraphBuilder.homeDestination(navController: NavHostController) {
    composable<HomeRoute> { entry ->
        val trashedCount by entry.savedStateHandle.getStateFlow(TRASHED_COUNT_KEY, 0).collectAsState()
        HomeDestination(
            navigation =
                HomeNavigation(
                    openDeck = { day -> navController.navigate(DeckRoute.of(day)) },
                    openSettings = { navController.navigate(SettingsRoute) },
                    openSummary = { navController.navigate(SummaryRoute) },
                ),
            confirmed =
                if (trashedCount > 0) {
                    ConfirmationAnnouncement(trashedCount) { entry.savedStateHandle[TRASHED_COUNT_KEY] = 0 }
                } else {
                    null
                },
        )
    }
}
