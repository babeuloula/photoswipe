package fr.babeuloula.photoswipe.ui.home

import androidx.activity.compose.setContent
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
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Accueil composé seul dans `MainActivity`, avec des états fixés. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class HomeScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<HomeEvent>()

    private fun show(state: HomeUiState) {
        composeRule.activity.setContent { PhotoSwipeTheme { HomeScreen(state = state, onEvent = { events += it }) } }
    }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    @Test
    fun `shows the today card with the date and the count of media to sort`() {
        show(HomeUiState.Loaded(ReviewDay(LocalDate.of(2026, 7, 15), remainingCount = 12)))

        composeRule.onNodeWithText(text(R.string.home_today)).assertIsDisplayed()
        composeRule.onNodeWithText("Wednesday, July 15, 2026").assertIsDisplayed()
        composeRule.onNodeWithText("12 items to sort").assertIsDisplayed()
    }

    @Test
    fun `opens the deck of today when the card is tapped`() {
        show(HomeUiState.Loaded(ReviewDay(LocalDate.of(2026, 7, 15), remainingCount = 1)))

        composeRule.onNodeWithText("1 item to sort").performClick()

        assertEquals(listOf<HomeEvent>(HomeEvent.OpenToday), events)
    }

    @Test
    fun `offers to try again after an error`() {
        show(HomeUiState.Error)

        composeRule.onNodeWithText(text(R.string.home_error)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_retry)).performClick()

        assertEquals(listOf<HomeEvent>(HomeEvent.Retry), events)
    }

    @Test
    fun `shows no card while loading`() {
        show(HomeUiState.Loading)

        composeRule.onNodeWithText(text(R.string.home_today)).assertDoesNotExist()
    }

    @Test
    fun `opens the settings from the top bar`() {
        show(HomeUiState.Loading)

        composeRule.onNodeWithContentDescription(text(R.string.home_open_settings)).performClick()

        assertEquals(listOf<HomeEvent>(HomeEvent.OpenSettings), events)
    }

    @Test
    @Config(qualifiers = "fr")
    fun `shows the today card in french`() {
        show(HomeUiState.Loaded(ReviewDay(LocalDate.of(2026, 7, 15), remainingCount = 12)))

        composeRule.onNodeWithText("Aujourd'hui").assertIsDisplayed()
        composeRule.onNodeWithText("mercredi 15 juillet 2026").assertIsDisplayed()
        composeRule.onNodeWithText("12 médias à trier").assertIsDisplayed()
    }
}
