package fr.babeuloula.photoswipe.ui.dayoverview

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.DayOverview
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.OverviewMemory
import fr.babeuloula.photoswipe.domain.model.OverviewYear
import fr.babeuloula.photoswipe.domain.model.ReviewDecision
import fr.babeuloula.photoswipe.domain.model.SimilarGroup
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Vue d'ensemble d'un jour composée seule dans `MainActivity`, avec des états fixés (PS-139). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class DayOverviewScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val day = LocalDate.of(2026, 5, 14)
    private val events = mutableListOf<DayOverviewEvent>()

    private val overview =
        DayOverview(
            day,
            listOf(
                OverviewYear(
                    2024,
                    listOf(
                        OverviewMemory(aMediaItem(id = 1), decision = null, favorite = false),
                        OverviewMemory(aMediaItem(id = 2), ReviewDecision.KEPT, favorite = true),
                    ),
                ),
                OverviewYear(
                    2020,
                    listOf(
                        OverviewMemory(
                            aMediaItem(id = 3),
                            ReviewDecision.MARKED_FOR_DELETION,
                            false,
                            SimilarGroup(1, 2),
                        ),
                        OverviewMemory(aMediaItem(id = 4), decision = null, favorite = false, SimilarGroup(2, 2)),
                    ),
                ),
            ),
        )

    private fun show(state: DayOverviewUiState) {
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                PhotoSwipeTheme { DayOverviewScreen(day = day, state = state, onEvent = { events += it }) }
            }
        }
    }

    @Test
    fun `groups the memories of the day by year with how long ago and how many`() {
        show(DayOverviewUiState.Loaded(overview))

        composeRule.onNodeWithText("Memories of May 14").assertIsDisplayed()
        composeRule.onNodeWithText("2024 · 2 years ago · 2 memories").assertIsDisplayed()
        composeRule.onNodeWithText("2020 · 6 years ago · 2 memories").assertIsDisplayed()
    }

    @Test
    fun `describes each memory with its state for TalkBack`() {
        show(DayOverviewUiState.Loaded(overview))

        composeRule.onNodeWithTag("${MEMORY_TILE_TAG}1").assertContentDescriptionEquals("Photo, to sort")
        composeRule.onNodeWithTag("${MEMORY_TILE_TAG}2").assertContentDescriptionEquals("Photo, kept, in favorites")
        composeRule
            .onNodeWithTag("${MEMORY_TILE_TAG}3")
            .assertContentDescriptionEquals("Photo, marked for deletion, 1 of 2 taken one after another")
    }

    @Test
    fun `opens a memory and sorts what is left`() {
        show(DayOverviewUiState.Loaded(overview))

        composeRule.onNodeWithTag("${MEMORY_TILE_TAG}1").performClick()
        composeRule.onNodeWithText("Sort the 2 left").performClick()

        assertEquals(listOf(DayOverviewEvent.Open(MediaId(1)), DayOverviewEvent.StartSorting), events)
    }

    @Test
    fun `selects a memory by a long press, or by the TalkBack action`() {
        show(DayOverviewUiState.Loaded(overview))

        composeRule.onNodeWithTag("${MEMORY_TILE_TAG}1").performTouchInput { longClick() }
        composeRule
            .onNodeWithTag("${MEMORY_TILE_TAG}2")
            .fetchSemanticsNode()
            .config[SemanticsActions.CustomActions]
            .single { it.label == "Select" }
            .action()

        assertEquals(
            listOf(DayOverviewEvent.ToggleSelection(MediaId(1)), DayOverviewEvent.ToggleSelection(MediaId(2))),
            events,
        )
    }

    @Test
    fun `shows the selection, selects a year and sorts the batch`() {
        show(DayOverviewUiState.Loaded(overview, selected = setOf(MediaId(1), MediaId(3))))

        composeRule.onNodeWithText("2 selected").assertIsDisplayed()
        composeRule.onNodeWithTag("${MEMORY_TILE_TAG}1").assertContentDescriptionEquals("Photo, to sort, selected")
        composeRule.onNodeWithText("Sort the 2 left").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Select all of 2024").performClick()
        composeRule.onNodeWithText("Keep").performClick()
        composeRule.onNodeWithText("Delete").performClick()
        composeRule.onNodeWithContentDescription("Cancel the selection").performClick()

        assertEquals(
            listOf(
                DayOverviewEvent.ToggleYear(2024),
                DayOverviewEvent.KeepSelected,
                DayOverviewEvent.MarkSelected,
                DayOverviewEvent.Back,
            ),
            events,
        )
    }

    @Test
    @Config(qualifiers = "fr")
    fun `shows the overview in French`() {
        show(DayOverviewUiState.Loaded(overview))

        composeRule.onNodeWithText("2024 · Il y a 2 ans · 2 souvenirs").assertIsDisplayed()
        composeRule.onNodeWithTag("${MEMORY_TILE_TAG}2").assertContentDescriptionEquals("Photo, gardée, en favori")
    }

    @Test
    fun `says when the day has no memory`() {
        show(DayOverviewUiState.Loaded(DayOverview(day, emptyList())))

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.day_overview_empty)).assertIsDisplayed()
    }

    @Test
    fun `retries after an error`() {
        show(DayOverviewUiState.Error)

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.summary_retry)).performClick()

        assertEquals(listOf<DayOverviewEvent>(DayOverviewEvent.Retry), events)
    }
}
