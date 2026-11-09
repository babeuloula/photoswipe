package fr.babeuloula.photoswipe.ui.crash

import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Dialogue du rapport de plantage, proposé au lancement suivant (ADR 0033). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class CrashReportDialogTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<CrashReportEvent>()

    private fun text(id: Int): String = composeRule.activity.getString(id)

    @Test
    fun `offers to view and send the report, or to ignore it`() {
        composeRule.activity.setContent {
            PhotoSwipeTheme { CrashReportDialog(onEvent = { events += it }) }
        }

        composeRule.onNodeWithText(text(R.string.crash_report_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.crash_report_send)).performClick()
        composeRule.onNodeWithText(text(R.string.crash_report_ignore)).performClick()

        assertEquals(listOf(CrashReportEvent.Send, CrashReportEvent.Dismiss), events)
    }
}
