package fr.babeuloula.photoswipe.ui.permission

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** L'écran d'explication, affiché au démarrage de `MainActivity`, en anglais et en français. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class PermissionRationaleScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int): String = composeRule.activity.getString(id)

    @Test
    fun `explains what the app does, why it needs media access and that nothing leaves the phone`() {
        listOf(
            R.string.permission_rationale_what,
            R.string.permission_rationale_why_title,
            R.string.permission_rationale_why,
            R.string.permission_rationale_private_title,
            R.string.permission_rationale_private,
            R.string.permission_rationale_next,
        ).forEach { composeRule.onNodeWithText(text(it)).performScrollTo().assertIsDisplayed() }
    }

    @Test
    fun `marks the title as a heading for screen readers`() {
        composeRule.onNode(isHeading()).assertTextEquals(text(R.string.permission_rationale_title))
    }

    @Test
    fun `offers a continue button`() {
        composeRule.onNodeWithText(text(R.string.permission_rationale_continue)).assertIsDisplayed().performClick()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `shows the explanation in french`() {
        composeRule.onNodeWithText("Revivez vos souvenirs").assertIsDisplayed()
        composeRule.onNodeWithText("Continuer").assertIsDisplayed()
    }
}
