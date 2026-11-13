package fr.babeuloula.photoswipe.ui.whatsnew

import android.content.Context
import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.ReleaseVersion
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.ui.home.HomeEvent
import fr.babeuloula.photoswipe.ui.home.HomeScreen
import fr.babeuloula.photoswipe.ui.home.HomeUiState
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.Locale

/** Feuille des nouveautés sur l'accueil, et textes de chaque nouveauté (PS-173). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class WhatsNewSheetTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<HomeEvent>()

    private fun show(
        whatsNew: List<WhatsNewRelease>,
        showRatingInvitation: Boolean = false,
    ) {
        val today = LocalDate.of(2026, 7, 15)
        val state =
            HomeUiState.Loaded(
                today = ReviewDay(today, remainingCount = 2, memoryCount = 4),
                previousDays = (1L..6L).map { ReviewDay(today.minusDays(it), remainingCount = 0) },
                showRatingInvitation = showRatingInvitation,
                whatsNew = whatsNew,
            )
        composeRule.activity.setContent {
            PhotoSwipeTheme { HomeScreen(state = state, onEvent = { events += it }) }
        }
    }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    @Test
    fun `shows the news of the missed version on the home screen, then closes`() {
        show(WHATS_NEW)

        composeRule.onNodeWithText(text(R.string.whats_new_title)).assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.whats_new_version, "1.4.0")).assertExists()
        composeRule.onNodeWithText(text(R.string.whats_new_1_4_0_progress_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.whats_new_close)).performClick()

        assertEquals(listOf<HomeEvent>(HomeEvent.DismissWhatsNew), events)
    }

    @Test
    fun `scrolls through every missed version, the most recent first`() {
        val older = WhatsNewRelease(ReleaseVersion(1, 3, 0), WHATS_NEW.single().items)
        show(WHATS_NEW + older + older.copy(version = ReleaseVersion(1, 2, 0)))

        val oldest = composeRule.activity.getString(R.string.whats_new_version, "1.2.0")
        composeRule
            .onNode(
                hasScrollToNodeAction() and hasAnyDescendant(hasText(text(R.string.whats_new_1_4_0_progress_title))),
            ).performScrollToNode(hasText(oldest))

        composeRule.onNodeWithText(oldest).assertExists()
    }

    @Test
    fun `waits for the rating invitation to be closed`() {
        show(WHATS_NEW, showRatingInvitation = true)

        composeRule.onNodeWithText(text(R.string.rating_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.whats_new_title)).assertDoesNotExist()
    }

    @Test
    fun `shows nothing without news`() {
        show(emptyList())

        composeRule.onNodeWithText(text(R.string.whats_new_title)).assertDoesNotExist()
    }

    @Test
    fun `lists each version once, the most recent first`() {
        val versions = WHATS_NEW.map { it.version }

        assertEquals(versions.sortedDescending(), versions)
        assertEquals(versions.distinct(), versions)
        assertTrue(WHATS_NEW.all { it.items.isNotEmpty() })
    }

    @Test
    fun `gives each news its own keys for its version, in English and French`() {
        val english = localized(Locale.ENGLISH)
        val french = localized(Locale.FRENCH)
        WHATS_NEW.forEach { release ->
            val prefix = "whats_new_${release.version.toString().replace('.', '_')}_"
            release.items.flatMap { listOf(it.title, it.text) }.forEach { id ->
                val name = english.resources.getResourceEntryName(id)
                assertTrue("$name does not belong to ${release.version}", name.startsWith(prefix))
                val inEnglish = english.getString(id)
                val inFrench = french.getString(id)
                assertTrue("$name is empty", inEnglish.isNotBlank() && inFrench.isNotBlank())
                assertNotEquals("$name is not translated", inEnglish, inFrench)
            }
        }
    }

    private fun localized(locale: Locale): Context {
        val configuration = Configuration(composeRule.activity.resources.configuration)
        configuration.setLocale(locale)
        return composeRule.activity.createConfigurationContext(configuration)
    }
}
