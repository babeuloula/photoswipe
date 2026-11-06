package fr.babeuloula.photoswipe.di

import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.debug.DeveloperToolsActivity
import fr.babeuloula.photoswipe.debug.DeveloperToolsScreen
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Outils développeur de la variante debug : entrée des réglages, remise à zéro confirmée. Le
 * vidage de la base lui-même est testé dans `:data` (`DatabaseCleanerTest`).
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class DeveloperToolsTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val application = RuntimeEnvironment.getApplication()

    private fun text(id: Int): String = application.getString(id)

    @Test
    fun `opens the developer tools from the settings entry`() {
        val intent = DeveloperToolsModule.provideDeveloperToolsEntry().intent(application)

        assertEquals(DeveloperToolsActivity::class.java.name, intent?.component?.className)
    }

    @Test
    fun `asks for a confirmation before resetting the decisions`() {
        var resets = 0
        composeRule.activity.setContent {
            PhotoSwipeTheme {
                DeveloperToolsScreen(initialDemoData = false, onDemoDataChange = {}, onResetDecisions = { resets++ })
            }
        }

        composeRule.onNodeWithText(text(R.string.developer_tools_reset_action)).performClick()
        assertEquals(0, resets)
        composeRule.onNodeWithText(text(R.string.developer_tools_reset_cancel)).performClick()
        composeRule.onNodeWithText(text(R.string.developer_tools_reset_action)).performClick()
        composeRule.onNodeWithText(text(R.string.developer_tools_reset_confirm_title)).assertExists()
        composeRule.onAllNodesWithText(text(R.string.developer_tools_reset_action))[1].performClick()

        composeRule.waitUntil { resets == 1 }
        composeRule.onNodeWithText(text(R.string.developer_tools_reset_done)).assertExists()
    }
}
