package fr.babeuloula.photoswipe.ui.tutorial

import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Illustrations du tutoriel, animées ou statiques, faites de formes et de repères, jamais de photos. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class TutorialIllustrationsTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun show(
        page: TutorialPage,
        animate: Boolean,
    ) {
        composeRule.mainClock.autoAdvance = false
        composeRule.activity.setContent {
            PhotoSwipeTheme { TutorialIllustration(page = page, animate = animate, modifier = Modifier) }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun yearsAgo(years: Int): String =
        composeRule.activity.resources.getQuantityString(R.plurals.deck_years_ago, years, years)

    private fun shown(text: String): Boolean =
        composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    @Test
    fun `the same day page shows the memories of one year after another`() {
        show(TutorialPage.SAME_DAY, animate = true)
        assertTrue(shown(yearsAgo(SAME_DAY_YEARS[0])))

        composeRule.mainClock.advanceTimeBy(SAME_DAY_STEP_MILLIS + SETTLE_MILLIS)

        assertTrue(shown(yearsAgo(SAME_DAY_YEARS[1])))
    }

    @Test
    fun `the static same day page keeps a memory from three years ago`() {
        show(TutorialPage.SAME_DAY, animate = false)

        composeRule.mainClock.advanceTimeBy(SAME_DAY_STEP_MILLIS * 3)

        composeRule.onNodeWithText(yearsAgo(3), substring = true).assertIsDisplayed()
    }

    @Test
    fun `the swipe up page opens the favorite and share menu`() {
        show(TutorialPage.SWIPE_UP, animate = true)
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.deck_share)).assertDoesNotExist()

        composeRule.mainClock.advanceTimeBy(SWIPE_UP_CYCLE_MILLIS)

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.deck_favorite)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.deck_share)).assertIsDisplayed()
    }

    @Test
    fun `the static swipe up page shows the menu open`() {
        show(TutorialPage.SWIPE_UP, animate = false)

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.deck_favorite)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.deck_share)).assertIsDisplayed()
    }

    private fun reminder(count: Int): String =
        composeRule.activity.resources.getQuantityString(R.plurals.reminder_title, count, count)

    @Test
    fun `the catch up page sorts the last 7 days one after another and counts what is left`() {
        show(TutorialPage.CATCH_UP, animate = true)
        composeRule.onNodeWithText(reminder(SAMPLE_COUNTS.sum())).assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(CATCH_UP_STEP_MILLIS + CATCH_UP_STEP_MILLIS / 2)

        composeRule.onNodeWithText(reminder(SAMPLE_COUNTS.sum() - SAMPLE_COUNTS.last())).assertIsDisplayed()
    }

    @Test
    fun `the static catch up page shows today and yesterday sorted`() {
        show(TutorialPage.CATCH_UP, animate = false)

        val left = SAMPLE_COUNTS.dropLast(2).sum()
        composeRule.onNodeWithText(reminder(left)).assertIsDisplayed()
    }

    private companion object {
        /** Assez pour que le tirage monte et que la feuille s'ouvre. */
        const val SWIPE_UP_CYCLE_MILLIS = 2_200L

        /** Temps laissé à la transition d'un tirage au suivant. */
        const val SETTLE_MILLIS = 1_000L
    }
}
