package fr.babeuloula.photoswipe.ui

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.Achievements
import fr.babeuloula.photoswipe.domain.model.Badge
import fr.babeuloula.photoswipe.domain.model.BadgeStatus
import fr.babeuloula.photoswipe.domain.model.CatchUpWindow
import fr.babeuloula.photoswipe.domain.model.DayMemories
import fr.babeuloula.photoswipe.domain.model.DayOverview
import fr.babeuloula.photoswipe.domain.model.FreeSpaceGoal
import fr.babeuloula.photoswipe.domain.model.GIGABYTE
import fr.babeuloula.photoswipe.domain.model.GoalProgress
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.OverviewMemory
import fr.babeuloula.photoswipe.domain.model.OverviewYear
import fr.babeuloula.photoswipe.domain.model.ReminderSettings
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.domain.model.Streak
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import fr.babeuloula.photoswipe.domain.model.Trash
import fr.babeuloula.photoswipe.domain.model.TrashItem
import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import fr.babeuloula.photoswipe.domain.model.YearMemories
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.crash.CrashReportDialog
import fr.babeuloula.photoswipe.ui.dayoverview.DayOverviewScreen
import fr.babeuloula.photoswipe.ui.dayoverview.DayOverviewUiState
import fr.babeuloula.photoswipe.ui.deck.DeckScreen
import fr.babeuloula.photoswipe.ui.deck.DeckUiState
import fr.babeuloula.photoswipe.ui.deck.FullscreenVideoContent
import fr.babeuloula.photoswipe.ui.deck.LocalCardVideoPlayer
import fr.babeuloula.photoswipe.ui.deck.PREVIEW_CARD
import fr.babeuloula.photoswipe.ui.deck.PhotoViewerContent
import fr.babeuloula.photoswipe.ui.deck.RecordingVideoPlayer
import fr.babeuloula.photoswipe.ui.deck.VideoProgressBar
import fr.babeuloula.photoswipe.ui.home.HomeScreen
import fr.babeuloula.photoswipe.ui.home.HomeUiState
import fr.babeuloula.photoswipe.ui.licenses.LicensesScreen
import fr.babeuloula.photoswipe.ui.permission.LimitedAccessBanner
import fr.babeuloula.photoswipe.ui.permission.MediaAccessDeniedScreen
import fr.babeuloula.photoswipe.ui.permission.PermissionRationaleScreen
import fr.babeuloula.photoswipe.ui.postcard.PostcardPreview
import fr.babeuloula.photoswipe.ui.postcard.PostcardPreviewDialog
import fr.babeuloula.photoswipe.ui.progress.GoalDialog
import fr.babeuloula.photoswipe.ui.progress.PREVIEW_ACTIVITY
import fr.babeuloula.photoswipe.ui.progress.ProgressScreen
import fr.babeuloula.photoswipe.ui.progress.ProgressUiState
import fr.babeuloula.photoswipe.ui.settings.SettingsEnvironment
import fr.babeuloula.photoswipe.ui.settings.SettingsScreen
import fr.babeuloula.photoswipe.ui.settings.SettingsUiState
import fr.babeuloula.photoswipe.ui.summary.SummaryScreen
import fr.babeuloula.photoswipe.ui.summary.SummaryUiState
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import fr.babeuloula.photoswipe.ui.trash.TrashScreen
import fr.babeuloula.photoswipe.ui.trash.TrashUiState
import fr.babeuloula.photoswipe.ui.tutorial.PracticePage
import fr.babeuloula.photoswipe.ui.tutorial.TutorialScreen
import fr.babeuloula.photoswipe.ui.whatsnew.WHATS_NEW
import fr.babeuloula.photoswipe.ui.whatsnew.WhatsNewContent
import fr.babeuloula.photoswipe.ui.widget.WidgetSettingsScreen
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Accessibilité de chaque écran (SPEC §8) : libellés des éléments actionnables et cibles tactiles
 * d'au moins 48 dp, en police normale et à 200 %. Les contrastes sont vérifiés sur les couleurs
 * du thème (`ContrastTest`). L'Accessibility Test Framework de Google ne détecte rien sous
 * Robolectric : ce contrôle passe donc par l'arbre sémantique de Compose, que lit TalkBack.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
// 360 dp de large, celle des téléphones courants : le calendrier de la page « Progression » y tient
// sept cases de 48 dp (PS-157), ce que les 320 dp par défaut de Robolectric ne permettent pas.
@Config(application = HiltTestApplication::class, qualifiers = "w360dp-h800dp")
class AccessibilityTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val today = LocalDate.of(2026, 7, 15)

    /**
     * Affiche [screen] puis vérifie chaque élément actionnable de son arbre sémantique, tel que
     * TalkBack le parcourt : il a un libellé (texte ou description) et une cible tactile d'au
     * moins 48 dp. La moindre erreur fait échouer le test.
     */
    private fun check(screen: @Composable () -> Unit) {
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                PhotoSwipeTheme { screen() }
            }
        }
        composeRule.waitForIdle()
        val minTouch = with(Density(composeRule.activity)) { MIN_TOUCH_TARGET.toPx() }
        val problems =
            composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().mapNotNull { node ->
                val label =
                    node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() +
                        node.config
                            .getOrNull(SemanticsProperties.Text)
                            .orEmpty()
                            .map { it.text }
                // Cible tactile : celle de Compose, qui inclut l'agrandissement à 48 dp des boutons
                // d'icône ; hors de l'écran visible (bornes rognées à zéro), la taille de l'élément.
                val touch = node.touchBoundsInRoot
                val width = if (touch.isEmpty) node.size.width.toFloat() else touch.width
                val height = if (touch.isEmpty) node.size.height.toFloat() else touch.height
                when {
                    label.all { it.isBlank() } -> "unlabelled action at ${node.boundsInRoot}"
                    width < minTouch - 1 || height < minTouch - 1 -> "touch target too small: $label ($width x $height)"
                    else -> null
                }
            }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    /** Ce qui s'ouvre ou se pose sur la carte de la pile : photo en grand, barre de progression, plein écran vidéo. */
    private fun deckOverlays() {
        check { PhotoViewerContent(card = PREVIEW_CARD, onClose = {}) }
        check { VideoProgressBar(position = Duration.ofSeconds(12), duration = Duration.ofSeconds(45), onSeek = {}) }
        val videoPlayer = RecordingVideoPlayer(composeRule.activity)
        check {
            FullscreenVideoContent(
                card = PREVIEW_CARD.copy(type = MediaType.VIDEO, duration = Duration.ofSeconds(45)),
                videoPlayer = videoPlayer,
                onExit = {},
            )
        }
        // Photo animée (PS-167) : pastille et bouton de lecture sur la carte du dessus.
        check {
            CompositionLocalProvider(LocalCardVideoPlayer provides videoPlayer) {
                DeckScreen(
                    day = today,
                    state =
                        DeckUiState.Loaded(
                            listOf(PREVIEW_CARD.copy(motionVideo = "${PREVIEW_CARD.uri}#photoswipe-motion=1-2")),
                        ),
                    onEvent = {},
                )
            }
        }
        videoPlayer.release()
        // Aperçu de la carte postale (PS-177), prête puis en préparation.
        val postcard = Bitmap.createBitmap(300, 240, Bitmap.Config.ARGB_8888).asImageBitmap()
        val ready = PostcardPreview.Ready(postcard, "5 years ago · Nov 6, 2021")
        listOf(ready, PostcardPreview.Loading).forEach { preview ->
            check {
                PostcardPreviewDialog(
                    preview = preview,
                    withBrand = true,
                    onWithBrandChange = {},
                    onShare = {},
                    onDismiss = {},
                )
            }
        }
    }

    /** Réglages, rappel activé. */
    private fun settingsScreen() {
        check {
            SettingsScreen(
                state =
                    SettingsUiState(
                        reminder = ReminderSettings.DEFAULT.copy(enabled = true),
                        themeMode = ThemeMode.SYSTEM,
                        catchUpWindow = CatchUpWindow.TWO_WEEKS,
                    ),
                onEvent = {},
                environment = SettingsEnvironment(languageLabel = "English"),
            )
        }
    }

    /** Page « Progression », statistiques remplies, puis en erreur. */
    private fun progressScreen() {
        check {
            ProgressScreen(
                state =
                    ProgressUiState.Loaded(
                        UsageStatistics(today, deletedCount = 12, freedBytes = 40_000_000),
                        PREVIEW_ACTIVITY,
                        Streak(current = 4, best = 9),
                        YearMemories(
                            today.year,
                            mapOf(
                                today to DayMemories(memoryCount = 12, remainingCount = 12),
                                today.minusDays(1) to DayMemories(memoryCount = 3, remainingCount = 2),
                                today.minusDays(2) to DayMemories(memoryCount = 1, remainingCount = 0),
                            ),
                        ),
                        today,
                        Achievements(
                            Badge.entries.map { BadgeStatus(it, earned = it == Badge.FIRST_SORT, remaining = 1) },
                            goal = GoalProgress(FreeSpaceGoal(5 * GIGABYTE, 0), GIGABYTE),
                        ),
                    ),
                onEvent = {},
            )
        }
        check { ProgressScreen(state = ProgressUiState.Error, onEvent = {}) }
        check { GoalDialog(onEvent = {}) }
    }

    /** Accueil sur une fenêtre de 14 jours (PS-169) : liste repliée avec « Voir les jours plus anciens ». */
    private fun catchUpHomeScreen() {
        check {
            HomeScreen(
                state =
                    HomeUiState.Loaded(
                        today = ReviewDay(today, remainingCount = 3),
                        previousDays =
                            (1L..13L).map {
                                ReviewDay(
                                    today.minusDays(it),
                                    remainingCount = 1,
                                    memoryCount = 1,
                                )
                            },
                    ),
                onEvent = {},
            )
        }
    }

    /** Corbeille (PS-168) : avec des médias, vide, et la confirmation de suppression définitive. */
    private fun trashScreens() {
        val item =
            TrashItem(
                aMediaItem(id = 1),
                Instant.parse("2026-11-13T10:00:00Z"),
                Instant.parse("2026-12-13T10:00:00Z"),
            )
        check { TrashScreen(state = TrashUiState.Loaded(Trash(listOf(item))), onEvent = {}) }
        check { TrashScreen(state = TrashUiState.Loaded(Trash()), onEvent = {}) }
        check { TrashScreen(state = TrashUiState.Loaded(Trash(listOf(item)), confirmingDeletion = item), onEvent = {}) }
    }

    private fun everyScreen() {
        check { TutorialScreen(onDone = {}) }
        check { PracticePage() }
        check { PermissionRationaleScreen(onContinue = {}) }
        check { MediaAccessDeniedScreen(canAskAgain = false, onOpenSettings = {}, onTryAgain = {}) }
        check { LimitedAccessBanner(onManageAccess = {}) }
        check { WhatsNewContent(releases = WHATS_NEW, onClose = {}) }
        check {
            HomeScreen(
                state =
                    HomeUiState.Loaded(
                        today = ReviewDay(today, remainingCount = 3),
                        previousDays =
                            (1L..6L).map {
                                ReviewDay(
                                    today.minusDays(it),
                                    remainingCount = (it % 2).toInt(),
                                )
                            },
                        pendingDeletionCount = 2,
                        streak = 3,
                    ),
                onEvent = {},
            )
        }
        catchUpHomeScreen()
        check {
            DeckScreen(
                day = today,
                // Carte d'un jour retrié (ADR 0026) : pastille de la décision précédente et cœur.
                state =
                    DeckUiState.Loaded(
                        listOf(PREVIEW_CARD.copy(previousDecision = ReviewDecision.KEPT, favorite = true)),
                        canUndo = true,
                    ),
                onEvent = {},
            )
        }
        check { DeckScreen(day = today, state = DeckUiState.Empty(canUndo = true), onEvent = {}) }
        deckOverlays()
        dayOverviewScreens()
        check {
            SummaryScreen(
                state =
                    SummaryUiState.Loaded(
                        SessionSummary(
                            keptCount = 5,
                            markedForDeletionCount = 3,
                            favoriteCount = 1,
                            estimatedFreedBytes = 9_000_000,
                        ),
                    ),
                onEvent = {},
            )
        }
        trashScreens()
        settingsScreen()
        progressScreen()
        smallScreens()
    }

    /** Licences, rapport de plantage et réglages du widget « Ce jour-là » (PS-170). */
    private fun smallScreens() {
        check { LicensesScreen(onBack = {}, licenseText = "Apache License, Version 2.0") }
        check { CrashReportDialog(onEvent = {}) }
        check { WidgetSettingsScreen(showPhoto = true, onShowPhotoChange = {}, onDone = {}) }
    }

    /** Vue d'ensemble d'un jour, puis la même pendant une sélection (PS-159). */
    private fun dayOverviewScreens() {
        check {
            DayOverviewScreen(
                day = today,
                // Case d'une photo animée (PS-167).
                state = DayOverviewUiState.Loaded(sampleOverview(), motionPhotos = setOf(MediaId(1))),
                onEvent = {},
            )
        }
        check {
            DayOverviewScreen(
                day = today,
                state = DayOverviewUiState.Loaded(sampleOverview(), selected = setOf(MediaId(1))),
                onEvent = {},
            )
        }
    }

    /** Vue d'ensemble d'exemple : une année, un souvenir de chaque état. */
    private fun sampleOverview(): DayOverview =
        DayOverview(
            today,
            listOf(
                OverviewYear(
                    today.year - 2,
                    listOf(
                        OverviewMemory(aMediaItem(id = 1), decision = null, favorite = false),
                        OverviewMemory(aMediaItem(id = 2), ReviewDecision.KEPT, favorite = true),
                        OverviewMemory(aMediaItem(id = 3), ReviewDecision.MARKED_FOR_DELETION, favorite = false),
                    ),
                ),
            ),
        )

    @Test
    fun `every screen passes the accessibility checks`() {
        everyScreen()
    }

    @Test
    @Config(fontScale = 2.0f)
    fun `every screen passes the accessibility checks with a 200 percent font`() {
        everyScreen()
    }

    @Test
    fun `the checks catch a tiny unlabelled button`() {
        // Témoin : prouve que les vérifications tournent vraiment sur ces écrans.
        assertThrows(Throwable::class.java) {
            check { Box(Modifier.size(20.dp).clickable {}) }
        }
    }

    private companion object {
        /** Cible tactile minimale (SPEC §8, Material 3). */
        val MIN_TOUCH_TARGET = 48.dp
    }
}
