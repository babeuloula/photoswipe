package fr.babeuloula.photoswipe.ui.permission

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
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

/** Écran de refus, composé seul dans `MainActivity` (son contenu habituel est remplacé). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class MediaAccessDeniedScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val clicks = mutableListOf<String>()

    private fun show(canAskAgain: Boolean) {
        composeRule.activity.setContent {
            PhotoSwipeTheme {
                MediaAccessDeniedScreen(
                    canAskAgain = canAskAgain,
                    onOpenSettings = { clicks += "settings" },
                    onTryAgain = { clicks += "again" },
                )
            }
        }
    }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    @Test
    fun `explains the refusal and opens the settings`() {
        show(canAskAgain = false)

        composeRule.onNodeWithText(text(R.string.media_access_denied_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.media_access_denied_body)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.media_access_open_settings)).performClick()

        assertEquals(listOf("settings"), clicks)
    }

    @Test
    fun `hides try again once android no longer asks`() {
        show(canAskAgain = false)

        composeRule.onNodeWithText(text(R.string.media_access_try_again)).assertDoesNotExist()
    }

    @Test
    fun `lets the user try again while android still asks`() {
        show(canAskAgain = true)

        composeRule.onNodeWithText(text(R.string.media_access_try_again)).performClick()

        assertEquals(listOf("again"), clicks)
    }

    @Test
    @Config(qualifiers = "fr")
    fun `shows the refusal in french`() {
        show(canAskAgain = true)

        composeRule.onNodeWithText("Ouvrir les réglages").assertIsDisplayed()
        composeRule.onNodeWithText("Réessayer").assertIsDisplayed()
    }
}
