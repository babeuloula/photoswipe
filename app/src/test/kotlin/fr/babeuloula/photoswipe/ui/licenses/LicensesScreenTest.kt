package fr.babeuloula.photoswipe.ui.licenses

import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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

/** Écran des licences, avec le vrai texte de `res/raw`. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class LicensesScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private var backs = 0

    private fun show() {
        composeRule.activity.setContent { PhotoSwipeTheme { LicensesScreen(onBack = { backs++ }) } }
    }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    @Test
    fun `states the app license and lists every library`() {
        show()

        composeRule.onNodeWithText(text(R.string.licenses_app_notice)).assertExists()
        OPEN_SOURCE_LIBRARIES.forEach { composeRule.onNodeWithText(it.name).performScrollTo().assertExists() }
    }

    @Test
    fun `shows the full Apache 2 0 text`() {
        show()

        composeRule.onNodeWithText("END OF TERMS AND CONDITIONS", substring = true).performScrollTo().assertExists()
    }

    @Test
    fun `goes back with the back arrow`() {
        show()

        composeRule.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()

        assertEquals(1, backs)
    }
}
