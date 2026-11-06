package fr.babeuloula.photoswipe.ui.tutorial

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Le carrousel du tutoriel : pages, indicateur, « Passer », « Suivant » et « C'est parti ». */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class TutorialScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private var doneCount = 0

    private fun text(
        id: Int,
        vararg args: Any,
    ): String = composeRule.activity.getString(id, *args)

    private fun show() {
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides composeRule.activity.imageLoader) {
                PhotoSwipeTheme { TutorialScreen(onDone = { doneCount++ }) }
            }
        }
        composeRule.waitForIdle()
    }

    private val pageCount = TutorialPage.entries.size

    @Test
    fun `starts on the first page with skip and next`() {
        show()

        composeRule.onNodeWithText(text(TutorialPage.entries.first().title)).assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(
                text(R.string.tutorial_page_indicator, 1, pageCount),
            ).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.tutorial_skip)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.tutorial_next)).assertIsDisplayed()
    }

    @Test
    fun `next shows the following page`() {
        show()

        composeRule.onNodeWithText(text(R.string.tutorial_next)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(TutorialPage.entries[1].title)).assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(
                text(R.string.tutorial_page_indicator, 2, pageCount),
            ).assertIsDisplayed()
    }

    @Test
    fun `swiping also changes the page`() {
        show()

        composeRule.onRoot().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(text(TutorialPage.entries[1].title)).assertIsDisplayed()
    }

    @Test
    fun `skip ends the tutorial from any page`() {
        show()

        composeRule.onNodeWithText(text(R.string.tutorial_skip)).performClick()

        assertEquals(1, doneCount)
    }

    @Test
    fun `the last page offers to start instead of next and skip`() {
        show()

        repeat(pageCount - 1) {
            composeRule.onNodeWithText(text(R.string.tutorial_next)).performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithText(text(TutorialPage.entries.last().title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.tutorial_next)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.tutorial_skip)).assertDoesNotExist()
        assertEquals(0, doneCount)
        composeRule.onNodeWithText(text(R.string.tutorial_start)).performClick()
        assertEquals(1, doneCount)
    }

    @Test
    @Config(qualifiers = "fr")
    fun `shows the tutorial in french`() {
        show()

        composeRule.onNodeWithText("Les souvenirs du même jour").assertIsDisplayed()
        composeRule.onNodeWithText("Passer").assertIsDisplayed()
        composeRule.onNodeWithText("Suivant").assertIsDisplayed()
    }
}
