package fr.babeuloula.photoswipe.ui.home

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performFirstLinkClick
import androidx.compose.ui.test.performScrollToNode
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.ReviewDay
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
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
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                PhotoSwipeTheme { HomeScreen(state = state, onEvent = { events += it }) }
            }
        }
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

        composeRule.onNodeWithText("All sorted for today").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_today)).performClick()

        assertEquals(emptyList<HomeEvent>(), events)
    }

    @Test
    fun `says that today has no memories while other days do`() {
        show(loaded(todayCount = 0))

        composeRule.onNodeWithText("No memories today").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_previous_days)).assertIsDisplayed()
    }

    @Test
    fun `shows a dedicated message when no memory is found in the whole week`() {
        val empty = (0L..6L).map { ReviewDay(LocalDate.of(2026, 7, 15).minusDays(it), remainingCount = 0) }
        show(HomeUiState.Loaded(today = empty.first(), previousDays = empty.drop(1)))

        composeRule.onNodeWithText(text(R.string.home_empty_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText("No photo or video taken from July 9 to July 15 in previous years. Come back tomorrow.")
            .assertIsDisplayed()
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

        composeRule.onNodeWithText("Tout est trié pour aujourd'hui").assertIsDisplayed()
    }

    @Test
    fun `lists the six previous days with their localized date and count`() {
        show(loaded(todayCount = 1))

        composeRule.onNodeWithText(text(R.string.home_previous_days)).assertIsDisplayed()
        composeRule.onNodeWithText("Tuesday, July 14").assertIsDisplayed()
        composeRule.onNodeWithText("3 items to sort").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Thursday, July 9"))
        composeRule.onNodeWithText("Thursday, July 9").assertIsDisplayed()
    }

    @Test
    fun `shows the years of today's memories, how many are not previewed, and starts sorting`() {
        val media = (1L..3L).map { aMediaItem(id = it) }
        show(
            loaded(todayCount = 23).copy(
                today = ReviewDay(LocalDate.of(2026, 7, 15), remainingCount = 23, previews = media, years = 2016..2025),
            ),
        )

        composeRule.onNodeWithText("From 2016 to 2025").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("and 20 more").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_start_sorting)).performClick()

        assertEquals(listOf<HomeEvent>(HomeEvent.OpenDay(LocalDate.of(2026, 7, 15))), events)
    }

    @Test
    fun `opens the summary once everything is sorted today`() {
        show(
            loaded(
                todayCount = 0,
            ).copy(today = ReviewDay(LocalDate.of(2026, 7, 15), remainingCount = 0, memoryCount = 4)),
        )

        composeRule.onNodeWithText(text(R.string.home_view_summary)).performClick()

        assertEquals(listOf<HomeEvent>(HomeEvent.OpenPendingDeletions), events)
    }

    @Test
    fun `sorts today again once everything is sorted`() {
        show(
            loaded(
                todayCount = 0,
            ).copy(today = ReviewDay(LocalDate.of(2026, 7, 15), remainingCount = 0, memoryCount = 4)),
        )

        composeRule.onNodeWithText(text(R.string.home_resort)).performClick()

        assertEquals(listOf<HomeEvent>(HomeEvent.ResortDay(LocalDate.of(2026, 7, 15))), events)
    }

    @Test
    fun `sorts again a previous day that is all sorted`() {
        val state = loaded(todayCount = 1)
        show(
            state.copy(
                previousDays =
                    state.previousDays.map {
                        if (it.date == LocalDate.of(2026, 7, 13)) it.copy(memoryCount = 2) else it
                    },
            ),
        )

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Monday, July 13"))
        composeRule.onNodeWithText("Monday, July 13").performClick()

        assertEquals(listOf<HomeEvent>(HomeEvent.ResortDay(LocalDate.of(2026, 7, 13))), events)
    }

    @Test
    @Config(qualifiers = "fr")
    fun `offers to sort today again in French`() {
        show(
            loaded(
                todayCount = 0,
            ).copy(today = ReviewDay(LocalDate.of(2026, 7, 15), remainingCount = 0, memoryCount = 4)),
        )

        composeRule.onNodeWithText("Retrier").assertIsDisplayed()
    }

    @Test
    fun `opens the deck of a previous day`() {
        show(loaded(todayCount = 1))

        composeRule.onNodeWithText("Tuesday, July 14").performClick()

        assertEquals(listOf<HomeEvent>(HomeEvent.OpenDay(LocalDate.of(2026, 7, 14))), events)
    }

    @Test
    fun `greys out a previous day without media and ignores taps`() {
        show(loaded(todayCount = 1))

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Monday, July 13"))
        composeRule.onNodeWithText("Monday, July 13").performClick()

        assertEquals(emptyList<HomeEvent>(), events)
    }

    @Test
    fun `shows the today card with the date and the count of media to sort`() {
        show(loaded(todayCount = 12))

        composeRule.onNodeWithText(text(R.string.home_today)).assertIsDisplayed()
        composeRule.onNodeWithText("Wednesday, July 15").assertIsDisplayed()
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
        composeRule.onNodeWithText("Mercredi 15 juillet").assertIsDisplayed()
        composeRule.onNodeWithText("12 médias à trier").assertIsDisplayed()
    }

    @Test
    fun `offers media management with a settings link and a close button`() {
        show(loaded(todayCount = 1).copy(showManageMediaTip = true))
        val message =
            composeRule.activity.getString(
                R.string.home_manage_media_tip,
                text(R.string.home_manage_media_tip_link),
            )

        composeRule.onNodeWithText(message).assertIsDisplayed().performFirstLinkClick()
        composeRule.onNodeWithContentDescription(text(R.string.home_manage_media_tip_dismiss)).performClick()

        assertEquals(listOf(HomeEvent.OpenManageMediaSettings, HomeEvent.DismissManageMediaTip), events)
    }

    @Test
    @Config(qualifiers = "fr")
    fun `offers media management in French`() {
        show(loaded(todayCount = 1).copy(showManageMediaTip = true))

        composeRule
            .onNodeWithText("Marre des confirmations ? Activez la gestion des médias dans les paramètres.")
            .assertIsDisplayed()
    }

    @Test
    fun `hides the media management tip by default`() {
        show(loaded(todayCount = 1))

        composeRule.onNodeWithContentDescription(text(R.string.home_manage_media_tip_dismiss)).assertDoesNotExist()
    }
}
