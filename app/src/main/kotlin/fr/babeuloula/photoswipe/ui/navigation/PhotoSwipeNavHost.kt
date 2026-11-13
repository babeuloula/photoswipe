package fr.babeuloula.photoswipe.ui.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.babeuloula.photoswipe.ui.common.LocalNavAnimatedScope
import fr.babeuloula.photoswipe.ui.common.LocalSharedTransitionScope
import fr.babeuloula.photoswipe.ui.common.rememberReducedMotion
import fr.babeuloula.photoswipe.ui.home.ConfirmationAnnouncement
import fr.babeuloula.photoswipe.ui.home.HomeNavigation
import fr.babeuloula.photoswipe.ui.licenses.LicensesScreen
import fr.babeuloula.photoswipe.ui.settings.SettingsNavigation
import fr.babeuloula.photoswipe.ui.tutorial.TutorialScreen
import java.time.LocalDate
import fr.babeuloula.photoswipe.ui.dayoverview.DayOverviewRoute as DayOverviewDestination
import fr.babeuloula.photoswipe.ui.deck.DeckRoute as DeckDestination
import fr.babeuloula.photoswipe.ui.home.HomeRoute as HomeDestination
import fr.babeuloula.photoswipe.ui.progress.ProgressRoute as ProgressDestination
import fr.babeuloula.photoswipe.ui.settings.SettingsRoute as SettingsDestination
import fr.babeuloula.photoswipe.ui.summary.SummaryRoute as SummaryDestination

/**
 * Graphe de navigation de l'application (SPEC §5.4), en routes typées : accueil, pile d'un jour,
 * vue d'ensemble d'un jour, résumé, progression, réglages, licences, tutoriel revu depuis les réglages. Le rappel quotidien peut demander d'ouvrir directement une pile
 * ([deckToOpen]), qui s'empile au-dessus de l'accueil. Les écrans s'enchaînent en axe partagé
 * avec zoom ([ScreenTransitions]), et les photos passent d'un écran à l'autre en grandissant
 * (éléments partagés, `SharedTransitionLayout`, PS-141).
 *
 * L'accès aux médias n'est pas une destination : `MediaAccessGate` entoure ce graphe, pour qu'un
 * accès retiré ramène à l'explication quel que soit l'écran (ADR 0014).
 *
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoSwipeNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    deckToOpen: LocalDate? = null,
    onDeckOpened: () -> Unit = {},
) {
    OpenRequestedDeck(navController, deckToOpen, onDeckOpened)
    val transitions = rememberScreenTransitions()
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            PhotoSwipeGraph(navController, transitions)
        }
    }
}

/** Les destinations de l'application, enchaînées par [transitions]. */
@Composable
private fun PhotoSwipeGraph(
    navController: NavHostController,
    transitions: ScreenTransitions,
) {
    val reduced = rememberReducedMotion()
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        // Animations coupées (réglages de PhotoSwipe ou d'Android) : les écrans changent d'un coup.
        enterTransition = { if (reduced) EnterTransition.None else transitions.enter },
        exitTransition = { if (reduced) ExitTransition.None else transitions.exit },
        popEnterTransition = { if (reduced) EnterTransition.None else transitions.popEnter },
        popExitTransition = { if (reduced) ExitTransition.None else transitions.popExit },
    ) {
        homeDestination(navController)
        composable<DeckRoute> {
            SharedScreen(this) {
                DeckDestination(
                    onBack = navController::popBackStack,
                    onOpenSummary = { navController.navigate(SummaryRoute) },
                    onOpenOverview = { day -> navController.navigate(DayOverviewRoute.of(day)) },
                )
            }
        }
        composable<DayOverviewRoute> {
            SharedScreen(this) {
                DayOverviewDestination(
                    onBack = navController::popBackStack,
                    // La pile ouverte depuis la vue d'ensemble remplace celle d'où elle venait, s'il y en a une.
                    onOpenDeck = { day, first ->
                        navController.navigate(DeckRoute.of(day, first)) { popUpTo<HomeRoute> { inclusive = false } }
                    },
                )
            }
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
                onOpenMarkedDeck = { first -> navController.navigate(DeckRoute.marked(first)) },
            )
        }
        settingsDestinations(navController)
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

/** Réglages et ce qu'ils ouvrent : progression, tutoriel revu, licences. */
private fun NavGraphBuilder.settingsDestinations(navController: NavHostController) {
    composable<SettingsRoute> {
        SettingsDestination(
            SettingsNavigation(
                back = navController::popBackStack,
                openLicenses = { navController.navigate(LicensesRoute) },
                openTutorial = { navController.navigate(TutorialRoute) },
                openProgress = { navController.navigate(ProgressRoute) },
            ),
        )
    }
    composable<ProgressRoute> {
        ProgressDestination(
            onBack = navController::popBackStack,
            onOpenOverview = { day -> navController.navigate(DayOverviewRoute.of(day)) },
        )
    }
    composable<TutorialRoute> {
        // Revu depuis les réglages : terminé ou passé, on y revient, sans toucher à l'état « vu ».
        TutorialScreen(onDone = navController::popBackStack, modifier = Modifier.safeDrawingPadding())
    }
    composable<LicensesRoute> {
        LicensesScreen(onBack = navController::popBackStack)
    }
}

/** Accueil : il ouvre les autres écrans et annonce ce que la confirmation du résumé a changé. */
private fun NavGraphBuilder.homeDestination(navController: NavHostController) {
    composable<HomeRoute> { entry ->
        SharedScreen(this) {
            val trashedCount by entry.savedStateHandle.getStateFlow(TRASHED_COUNT_KEY, 0).collectAsState()
            HomeDestination(
                navigation =
                    HomeNavigation(
                        openDeck = { day -> navController.navigate(DeckRoute.of(day)) },
                        openOverview = { day -> navController.navigate(DayOverviewRoute.of(day)) },
                        openSettings = { navController.navigate(SettingsRoute) },
                        openSummary = { navController.navigate(SummaryRoute) },
                        openProgress = { navController.navigate(ProgressRoute) },
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
}

/** Fournit à [content] l'animation de sa destination, pour ses éléments partagés (PS-141). */
@Composable
private fun SharedScreen(
    scope: AnimatedVisibilityScope,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalNavAnimatedScope provides scope, content = content)
}
