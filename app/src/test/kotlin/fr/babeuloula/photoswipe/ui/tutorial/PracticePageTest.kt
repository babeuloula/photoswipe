package fr.babeuloula.photoswipe.ui.tutorial

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.port.ReviewRepository
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import javax.inject.Inject

/**
 * Mode entraînement du tutoriel, sur la vraie base de l'application : les cartes d'exemple se
 * trient avec les boutons de la pile, sans qu'aucune décision soit enregistrée.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class PracticePageTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var reviewRepository: ReviewRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides composeRule.activity.imageLoader) {
                PhotoSwipeTheme { PracticePage() }
            }
        }
        composeRule.waitForIdle()
    }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    private fun press(description: Int) {
        composeRule.onNodeWithContentDescription(text(description)).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun `the sample cards are app illustrations, never gallery media`() {
        sampleCards(composeRule.activity).forEach { assertTrue(it.uri.startsWith("android.resource://")) }
    }

    @Test
    fun `sorts the sample cards with the deck buttons and says what each action does`() {
        composeRule.onNodeWithText(text(R.string.tutorial_practice_body)).assertIsDisplayed()

        press(R.string.deck_keep)
        composeRule.onNodeWithText(text(R.string.tutorial_practice_kept)).assertIsDisplayed()

        press(R.string.deck_mark_for_deletion)
        composeRule.onNodeWithText(text(R.string.tutorial_practice_marked)).assertIsDisplayed()

        press(R.string.deck_undo)
        composeRule.onNodeWithText(text(R.string.tutorial_practice_undone)).assertIsDisplayed()
    }

    @Test
    fun `sorting every sample card records no decision and offers to start again`() {
        repeat(sampleCards(composeRule.activity).size) { press(R.string.deck_mark_for_deletion) }

        composeRule.onNodeWithText(text(R.string.tutorial_practice_done)).assertIsDisplayed()
        val years = (0..10).map { LocalDate.now().year - it }.toSet()
        assertTrue(runBlocking { reviewRepository.entriesMarkedForDeletion() }.isEmpty())
        assertTrue(runBlocking { reviewRepository.observeEntries(years).first() }.isEmpty())

        composeRule.onNodeWithText(text(R.string.tutorial_practice_restart)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(text(R.string.tutorial_practice_body)).assertIsDisplayed()
    }
}
