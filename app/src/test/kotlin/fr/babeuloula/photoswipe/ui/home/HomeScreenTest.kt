package fr.babeuloula.photoswipe.ui.home

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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

    /** Le 15 juillet 2026 avec [todayCount] médias ; la veille en a 3, les autres jours aucun. */
    private fun loaded(todayCount: Int): HomeUiState.Loaded =
        HomeUiState.Loaded(
            today = ReviewDay(LocalDate.of(2026, 7, 15), remainingCount = todayCount),
            previousDays =
                (1L..6L).map {
                    ReviewDay(
                        LocalDate.of(2026, 7, 15).minusDays(it),
                        remainingCount =
                            if (it ==
                                1L
                            ) {
                                3
                            } else {
                                0
                            },
                    )
                },
        )

    @Test
    fun `shows the pending deletions banner and opens the summary`() {
        show(loaded(todayCount = 1).copy(pendingDeletionCount = 4))

        composeRule.onNodeWithText("4 items waiting to be deleted").assertIsDisplayed().performClick()

        assertEquals(listOf<HomeEvent>(HomeEvent.OpenPendingDeletions), events)
    }

    @Test
    fun `hides the pending deletions banner when nothing is waiting`() {
        show(loaded(todayCount = 1))

        composeRule.onNodeWithText(text(R.string.home_pending_deletions_review)).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `shows the pending deletions banner in french`() {
        show(loaded(todayCount = 1).copy(pendingDeletionCount = 1))

        composeRule.onNodeWithText("1 média en attente de suppression").assertIsDisplayed()
        composeRule.onNodeWithText("Vérifier").assertIsDisplayed()
    }

    @Test
    fun `says that everything is sorted today and keeps the card inactive`() {
        show(
            loaded(
                todayCount = 0,
            ).copy(today = ReviewDay(LocalDate.of(2026, 7, 15), remainingCount = 0, memoryCount = 4)),
        )

        composeRule.onNodeWithText("All done for today!").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_today_come_back)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_today)).performClick()

        assertEquals(emptyList<HomeEvent>(), events)
    }

    @Test
    fun `says that today has no memories while other days do`() {
        show(loaded(todayCount = 0))

        composeRule.onNodeWithText("No memories from this day").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_previous_days)).assertIsDisplayed()
    }

    @Test
    fun `shows a dedicated message when no memory is found in the whole week`() {
        val empty = (0L..6L).map { ReviewDay(LocalDate.of(2026, 7, 15).minusDays(it), remainingCount = 0) }
        show(HomeUiState.Loaded(today = empty.first(), previousDays = empty.drop(1)))

        composeRule.onNodeWithText(text(R.string.home_empty_title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_empty_body)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_today)).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `shows the empty states in french`() {
        show(
            loaded(
                todayCount = 0,
            ).copy(today = ReviewDay(LocalDate.of(2026, 7, 15), remainingCount = 0, memoryCount = 2)),
        )

        composeRule.onNodeWithText("Tout est trié pour aujourd'hui\u00A0!").assertIsDisplayed()
    }

    @Test
    fun `lists the six previous days with their localized date and count`() {
        show(loaded(todayCount = 1))

        composeRule.onNodeWithText(text(R.string.home_previous_days)).assertIsDisplayed()
        composeRule.onNodeWithText("Tuesday, July 14, 2026").assertIsDisplayed()
        composeRule.onNodeWithText("3 items to sort").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Thursday, July 9, 2026"))
        composeRule.onNodeWithText("Thursday, July 9, 2026").assertIsDisplayed()
    }

    @Test
    fun `opens the deck of a previous day`() {
        show(loaded(todayCount = 1))

        composeRule.onNodeWithText("Tuesday, July 14, 2026").performClick()

        assertEquals(listOf<HomeEvent>(HomeEvent.OpenDay(LocalDate.of(2026, 7, 14))), events)
    }

    @Test
    fun `greys out a previous day without media and ignores taps`() {
        show(loaded(todayCount = 1))

        composeRule.onNodeWithText("Monday, July 13, 2026").performClick()

        assertEquals(emptyList<HomeEvent>(), events)
    }

    @Test
    fun `shows the today card with the date and the count of media to sort`() {
        show(loaded(todayCount = 12))

        composeRule.onNodeWithText(text(R.string.home_today)).assertIsDisplayed()
        composeRule.onNodeWithText("Wednesday, July 15, 2026").assertIsDisplayed()
        composeRule.onNodeWithText("12 items to sort").assertIsDisplayed()
    }

    @Test
    fun `opens the deck of today when the card is tapped`() {
        show(loaded(todayCount = 1))

        composeRule.onNodeWithText("1 item to sort").performClick()

        assertEquals(listOf<HomeEvent>(HomeEvent.OpenDay(LocalDate.of(2026, 7, 15))), events)
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
        show(loaded(todayCount = 12))

        composeRule.onNodeWithText("Aujourd'hui").assertIsDisplayed()
        composeRule.onNodeWithText("mercredi 15 juillet 2026").assertIsDisplayed()
        composeRule.onNodeWithText("12 médias à trier").assertIsDisplayed()
    }
}
