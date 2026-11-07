package fr.babeuloula.photoswipe.ui.home

import androidx.activity.compose.setContent
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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

/** Invitation unique à noter l'application, sur l'accueil (PS-121). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class RatingInvitationDialogTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<HomeEvent>()

    private fun show(invite: Boolean) {
        val today = LocalDate.of(2026, 7, 15)
        val state =
            HomeUiState.Loaded(
                today = ReviewDay(today, remainingCount = 0, memoryCount = 4),
                previousDays = (1L..6L).map { ReviewDay(today.minusDays(it), remainingCount = 0) },
                showRatingInvitation = invite,
            )
        composeRule.activity.setContent {
            PhotoSwipeTheme { HomeScreen(state = state, onEvent = { events += it }) }
        }
    }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    @Test
    fun `offers to rate the app, to support the developer or to close`() {
        show(invite = true)

        composeRule.onNodeWithText(text(R.string.rating_title)).assertExists()
        composeRule.onNodeWithText(text(R.string.rating_rate)).performClick()
        composeRule.onNodeWithText(text(R.string.rating_support)).performClick()
        composeRule.onNodeWithText(text(R.string.rating_close)).performClick()

        assertEquals(
            listOf(HomeEvent.RateApp, HomeEvent.SupportDeveloper, HomeEvent.DismissRatingInvitation),
            events,
        )
    }

    @Test
    fun `shows no invitation otherwise`() {
        show(invite = false)

        composeRule.onNodeWithText(text(R.string.rating_title)).assertDoesNotExist()
    }
}
