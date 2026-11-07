package fr.babeuloula.photoswipe.ui.snooze

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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalTime

/** Sélecteur de la reprogrammation du rappel, composé seul dans `MainActivity`. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class SnoozeReminderDialogTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val confirmed = mutableListOf<LocalTime>()
    private var dismissed = false

    private fun show(timePassed: Boolean = false) {
        composeRule.activity.setContent {
            PhotoSwipeTheme {
                SnoozeReminderDialog(
                    initialTime = LocalTime.of(19, 30),
                    is24Hour = true,
                    timePassed = timePassed,
                    onConfirm = { confirmed += it },
                    onDismiss = { dismissed = true },
                )
            }
        }
    }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    @Test
    fun `confirms the suggested time`() {
        show()

        composeRule.onNodeWithText(text(R.string.reminder_snooze_title)).assertExists()
        composeRule.onNodeWithText(text(android.R.string.ok)).performClick()

        assertEquals(listOf(LocalTime.of(19, 30)), confirmed)
    }

    @Test
    fun `cancels without choosing a time`() {
        show()

        composeRule.onNodeWithText(text(android.R.string.cancel)).performClick()

        assertTrue(dismissed)
        assertTrue(confirmed.isEmpty())
    }

    @Test
    fun `explains that a past time is refused`() {
        show(timePassed = false)
        composeRule.onNodeWithText(text(R.string.reminder_snooze_time_passed)).assertDoesNotExist()

        show(timePassed = true)

        composeRule.onNodeWithText(text(R.string.reminder_snooze_time_passed)).assertExists()
    }
}
