package fr.babeuloula.photoswipe

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import fr.babeuloula.photoswipe.di.MediaSourceModule
import fr.babeuloula.photoswipe.di.TutorialModule
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.Tutorial
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.usecase.MarkTutorialSeen
import fr.babeuloula.photoswipe.domain.usecase.ShouldShowTutorial
import fr.babeuloula.photoswipe.link.DeepLinks
import fr.babeuloula.photoswipe.link.LaunchRequest
import fr.babeuloula.photoswipe.permission.MEDIA_PERMISSIONS
import fr.babeuloula.photoswipe.ui.dayoverview.DAY_OVERVIEW_GRID_TAG
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * `MainActivity` lancée par un lien profond `photoswipe://…` (PS-171, ADR 0038) : l'écran demandé
 * s'ouvre au-dessus de l'accueil, après le tutoriel et l'accès aux médias ; un lien invalide ou un
 * jour sans souvenir ouvre l'accueil.
 */
@HiltAndroidTest
@UninstallModules(MediaSourceModule::class, TutorialModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class MainActivityDeepLinkTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    /** Réglages du tutoriel : déjà vu, sauf pour le test qui le montre. */
    private val settings = FakeSettingsRepository(tutorialVersionSeen = Tutorial.CURRENT_VERSION)

    @BindValue
    @JvmField
    val shouldShowTutorial = ShouldShowTutorial(settings)

    @BindValue
    @JvmField
    val markTutorialSeen = MarkTutorialSeen(settings)

    /** Un souvenir pris aujourd'hui il y a un an, à midi : rien les autres jours. */
    @BindValue
    @JvmField
    val mediaSource: MediaSource =
        FakeMediaSource(
            listOf(
                aMediaItem(
                    id = 1,
                    takenAt =
                        LocalDate
                            .now()
                            .minusYears(1)
                            .atTime(12, 0)
                            .atZone(ZoneId.systemDefault())
                            .toInstant(),
                ),
            ),
        )

    private val application = RuntimeEnvironment.getApplication()

    private var scenario: ActivityScenario<MainActivity>? = null

    @After
    fun closeActivity() {
        scenario?.close()
    }

    @Test
    fun `reads the screen asked by a link or by the reminder`() {
        val today = LocalDate.now()

        assertEquals(LaunchRequest.TodayDeck, MainActivity.launchRequestOf(view(DeepLinks.TODAY)))
        assertEquals(LaunchRequest.Home, MainActivity.launchRequestOf(view("photoswipe://nope")))
        assertEquals(
            LaunchRequest.Deck(today),
            MainActivity.launchRequestOf(MainActivity.openDeckIntent(application, today)),
        )
        assertNull(MainActivity.launchRequestOf(Intent(Intent.ACTION_MAIN)))
        assertNull(MainActivity.launchRequestOf(view("https://example.org/today")))
    }

    @Test
    fun `opens the deck of today from its link`() {
        grantMediaAccess()

        launch(DeepLinks.TODAY)

        awaitText(deckTitle())
    }

    @Test
    fun `opens the summary from its link`() {
        grantMediaAccess()

        launch(DeepLinks.SUMMARY)

        awaitText(text(R.string.summary_title))
    }

    @Test
    fun `opens the overview of a day with memories from its link`() {
        grantMediaAccess()

        launch(DeepLinks.day(LocalDate.now()))

        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithTag(DAY_OVERVIEW_GRID_TAG).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun `opens the home screen for a day without memories`() {
        grantMediaAccess()

        launch(DeepLinks.day(LocalDate.now().plusDays(3)))

        awaitText(text(R.string.home_today))
    }

    @Test
    fun `opens the home screen for an invalid link`() {
        grantMediaAccess()

        launch("photoswipe://day/not-a-date")

        awaitText(text(R.string.home_today))
    }

    @Test
    fun `asks for media access before opening the screen of the link`() {
        launch(DeepLinks.TODAY)
        awaitText(text(R.string.permission_rationale_title))

        grantMediaAccess()
        checkNotNull(scenario).moveToState(Lifecycle.State.CREATED).moveToState(Lifecycle.State.RESUMED)

        awaitText(deckTitle())
    }

    @Test
    fun `shows the tutorial before opening the screen of the link`() {
        runBlocking { settings.saveTutorialVersionSeen(Tutorial.NEVER_SEEN) }
        grantMediaAccess()

        launch(DeepLinks.TODAY)
        awaitText(text(R.string.tutorial_skip))
        composeRule.onNodeWithText(deckTitle()).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.tutorial_skip)).performClick()

        awaitText(deckTitle())
    }

    private fun view(link: String): Intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))

    private fun launch(link: String) {
        scenario = ActivityScenario.launch(view(link).setClass(application, MainActivity::class.java))
    }

    private fun grantMediaAccess() {
        shadowOf(application).grantPermissions(*MEDIA_PERMISSIONS)
    }

    private fun text(id: Int): String = application.getString(id)

    /** Titre de la pile du jour : « Memories of November 13 ». */
    private fun deckTitle(): String =
        application.getString(
            R.string.deck_title,
            LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d", Locale.US)),
        )

    /** Attend [value] : la galerie et la base se lisent hors du thread principal. */
    private fun awaitText(value: String) {
        composeRule.waitUntil(
            TIMEOUT_MILLIS,
        ) { composeRule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
