package fr.babeuloula.photoswipe.ui.navigation

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
import fr.babeuloula.photoswipe.permission.MEDIA_PERMISSIONS
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Parcours entre les coquilles d'écrans, avec l'accès aux médias accordé avant le démarrage. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class PhotoSwipeNavHostTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val grantMediaAccess =
        object : ExternalResource() {
            override fun before() {
                shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(*MEDIA_PERMISSIONS)
            }
        }

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int): String = composeRule.activity.getString(id)

    private fun clickOn(id: Int) {
        composeRule.onNodeWithText(text(id)).performClick()
    }

    private fun goBack() {
        composeRule.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()
    }

    @Test
    fun `starts on the home screen`() {
        composeRule.onNodeWithText(text(R.string.home_sort_today)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.navigate_back)).assertDoesNotExist()
    }

    @Test
    fun `opens the deck of today from home and comes back`() {
        clickOn(R.string.home_sort_today)

        val today = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.deck_title, today)).assertIsDisplayed()

        goBack()
        composeRule.onNodeWithText(text(R.string.home_sort_today)).assertIsDisplayed()
    }

    @Test
    fun `goes from the deck to the summary then back home when done`() {
        clickOn(R.string.home_sort_today)
        clickOn(R.string.deck_finish)
        composeRule.onNodeWithText(text(R.string.summary_title)).assertIsDisplayed()

        clickOn(R.string.summary_done)

        composeRule.onNodeWithText(text(R.string.home_sort_today)).assertIsDisplayed()
    }

    @Test
    fun `opens the settings from home and comes back`() {
        clickOn(R.string.home_open_settings)
        composeRule.onNodeWithText(text(R.string.settings_title)).assertIsDisplayed()

        goBack()

        composeRule.onNodeWithText(text(R.string.home_sort_today)).assertIsDisplayed()
    }
}
