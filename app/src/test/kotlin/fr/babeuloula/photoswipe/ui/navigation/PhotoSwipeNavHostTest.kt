package fr.babeuloula.photoswipe.ui.navigation

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.di.MediaSourceModule
import fr.babeuloula.photoswipe.di.TutorialModule
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.fixtures.FakeSettingsRepository
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.Tutorial
import fr.babeuloula.photoswipe.domain.port.MediaSource
import fr.babeuloula.photoswipe.domain.usecase.MarkTutorialSeen
import fr.babeuloula.photoswipe.domain.usecase.ShouldShowTutorial
import fr.babeuloula.photoswipe.permission.MEDIA_PERMISSIONS
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
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
 * Parcours entre les écrans, avec l'accès aux médias accordé avant le démarrage et une galerie qui
 * contient un souvenir du jour (la carte « Aujourd'hui » est donc active).
 */
@HiltAndroidTest
@UninstallModules(MediaSourceModule::class, TutorialModule::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class PhotoSwipeNavHostTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val grantMediaAccess =
        object : ExternalResource() {
            override fun before() {
                shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(*MEDIA_PERMISSIONS)
            }
        }

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Tutoriel déjà vu : le test commence sur l'accueil. */
    @BindValue
    @JvmField
    val shouldShowTutorial = ShouldShowTutorial(FakeSettingsRepository(tutorialVersionSeen = Tutorial.CURRENT_VERSION))

    @BindValue
    @JvmField
    val markTutorialSeen = MarkTutorialSeen(FakeSettingsRepository())

    /** Un souvenir pris aujourd'hui il y a un an, à midi. */
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

    private fun text(id: Int): String = composeRule.activity.getString(id)

    /** Attend [value] : l'accueil se calcule hors du thread principal (MediaStore, Room). */
    private fun awaitText(value: String) {
        composeRule.waitUntil(
            TIMEOUT_MILLIS,
        ) { composeRule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(value).assertIsDisplayed()
    }

    private fun clickOn(id: Int) {
        awaitText(text(id))
        composeRule.onNodeWithText(text(id)).performClick()
    }

    private fun goBack() {
        composeRule.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()
    }

    @Test
    fun `opens the deck asked by the reminder above the home screen`() {
        var opened = 0
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides composeRule.activity.imageLoader) {
                PhotoSwipeTheme {
                    PhotoSwipeNavHost(deckToOpen = LocalDate.now(), onDeckOpened = { opened++ })
                }
            }
        }

        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d", Locale.US))
        awaitText(composeRule.activity.getString(R.string.deck_title, today))
        assertEquals(1, opened)

        goBack()
        awaitText(text(R.string.home_today))
    }

    @Test
    fun `starts on the home screen`() {
        awaitText(text(R.string.home_today))
        composeRule.onNodeWithContentDescription(text(R.string.navigate_back)).assertDoesNotExist()
    }

    @Test
    fun `opens the deck of today from its card and comes back`() {
        clickOn(R.string.home_today)

        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d", Locale.US))
        awaitText(composeRule.activity.getString(R.string.deck_title, today))

        goBack()
        awaitText(text(R.string.home_today))
    }

    @Test
    fun `goes from the deck to the summary then back home when done`() {
        clickOn(R.string.home_today)
        clickOn(R.string.deck_finish)
        awaitText(text(R.string.summary_title))

        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.summary_back_home)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text(R.string.summary_back_home)).performClick()

        awaitText(text(R.string.home_today))
    }

    @Test
    fun `opens the settings from home and comes back`() {
        awaitText(text(R.string.home_today))
        composeRule.onNodeWithContentDescription(text(R.string.home_open_settings)).performClick()
        awaitText(text(R.string.settings_title))

        goBack()

        awaitText(text(R.string.home_today))
    }

    @Test
    fun `stays on the settings when a language change recreates the app`() {
        awaitText(text(R.string.home_today))
        composeRule.onNodeWithContentDescription(text(R.string.home_open_settings)).performClick()
        awaitText(text(R.string.settings_title))

        composeRule.activityRule.scenario.recreate()

        awaitText(text(R.string.settings_title))
        goBack()
        awaitText(text(R.string.home_today))
    }

    @Test
    fun `opens the licenses from the settings and comes back`() {
        awaitText(text(R.string.home_today))
        composeRule.onNodeWithContentDescription(text(R.string.home_open_settings)).performClick()
        awaitText(text(R.string.settings_title))
        composeRule.onNodeWithText(text(R.string.settings_about_licenses)).performScrollTo().performClick()
        awaitText(text(R.string.licenses_title))

        goBack()

        awaitText(text(R.string.settings_title))
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L
    }
}
