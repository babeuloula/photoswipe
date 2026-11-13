package fr.babeuloula.photoswipe.ui.progress

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
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
import fr.babeuloula.photoswipe.domain.model.Achievements
import fr.babeuloula.photoswipe.domain.model.ActivityReport
import fr.babeuloula.photoswipe.domain.model.Badge
import fr.babeuloula.photoswipe.domain.model.BadgeStatus
import fr.babeuloula.photoswipe.domain.model.DayMemories
import fr.babeuloula.photoswipe.domain.model.FreeSpaceGoal
import fr.babeuloula.photoswipe.domain.model.GIGABYTE
import fr.babeuloula.photoswipe.domain.model.GoalProgress
import fr.babeuloula.photoswipe.domain.model.MonthlyActivity
import fr.babeuloula.photoswipe.domain.model.Streak
import fr.babeuloula.photoswipe.domain.model.UsageStatistics
import fr.babeuloula.photoswipe.domain.model.YearMemories
import fr.babeuloula.photoswipe.ui.deck.fileSizeLabel
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

/** Page « Progression » composée seule dans `MainActivity`, avec des états fixés. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class ProgressScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<ProgressEvent>()

    private fun show(state: ProgressUiState) {
        composeRule.activity.setContent {
            PhotoSwipeTheme { ProgressScreen(state = state, onEvent = { events += it }) }
        }
    }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    private companion object {
        /** Six mois sans aucun tri, historique vide. */
        val EMPTY_ACTIVITY = ActivityReport((6..11).map { MonthlyActivity(YearMonth.of(2026, it)) })
    }

    @Test
    fun `shows how many media were deleted and the space freed since the first use`() {
        show(
            ProgressUiState.Loaded(
                UsageStatistics(LocalDate.of(2026, 3, 2), deletedCount = 128, freedBytes = 1_200_000_000),
                EMPTY_ACTIVITY,
            ),
        )

        composeRule.onNodeWithText(text(R.string.progress_title)).assertIsDisplayed()
        composeRule.onNodeWithText("March 2, 2026").performScrollTo().assertExists()
        composeRule.onNodeWithText("128 photos and videos").performScrollTo().assertExists()
        composeRule.onNodeWithText(fileSizeLabel(composeRule.activity, 1_200_000_000)).performScrollTo().assertExists()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `says in French that nothing was deleted yet`() {
        show(ProgressUiState.Loaded(UsageStatistics(LocalDate.of(2026, 3, 2)), EMPTY_ACTIVITY))

        composeRule.onNodeWithText("Progression").assertIsDisplayed()
        composeRule.onNodeWithText("2 mars 2026").performScrollTo().assertExists()
        composeRule.onNodeWithText("Aucun média supprimé pour l'instant").performScrollTo().assertExists()
        composeRule.onNodeWithText(text(R.string.settings_statistics_freed)).assertDoesNotExist()
    }

    @Test
    fun `explains where the charts will be while nothing was sorted in the last months`() {
        show(ProgressUiState.Loaded(UsageStatistics(LocalDate.of(2026, 3, 2)), EMPTY_ACTIVITY))

        composeRule.onNodeWithText("The last 6 months").performScrollTo().assertExists()
        composeRule.onNodeWithText(text(R.string.progress_activity_empty)).performScrollTo().assertExists()
        composeRule.onNodeWithText(text(R.string.progress_activity_sorted)).assertDoesNotExist()
    }

    @Test
    fun `charts the media sorted and the space freed per month, read as a whole by TalkBack`() {
        show(ProgressUiState.Loaded(UsageStatistics(LocalDate.of(2026, 3, 2)), PREVIEW_ACTIVITY))

        composeRule.onNodeWithText("70% kept, 30% deleted").performScrollTo().assertExists()
        composeRule
            .onNodeWithContentDescription("June: kept 12, deleted 3.", substring = true)
            .performScrollTo()
            .assertExists()
        composeRule
            .onNodeWithContentDescription("July: ${fileSizeLabel(composeRule.activity, 0)}.", substring = true)
            .performScrollTo()
            .assertExists()
        composeRule
            .onNodeWithText("${fileSizeLabel(composeRule.activity, 33 * 40_000_000L)} freed in 6 months")
            .performScrollTo()
            .assertExists()
        composeRule.onNodeWithText("History since Jun 4, 2026").performScrollTo().assertExists()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `charts the activity in French`() {
        show(ProgressUiState.Loaded(UsageStatistics(LocalDate.of(2026, 3, 2)), PREVIEW_ACTIVITY))

        composeRule.onNodeWithText("Les 6 derniers mois").performScrollTo().assertExists()
        composeRule.onNodeWithText("70 % gardés, 30 % supprimés").performScrollTo().assertExists()
        composeRule
            .onNodeWithContentDescription("août : gardés 30, supprimés 18.", substring = true)
            .performScrollTo()
            .assertExists()
        composeRule.onNodeWithText("Historique depuis le 4 juin 2026").performScrollTo().assertExists()
    }

    @Test
    fun `shows the current and best streaks, and what keeps a streak going`() {
        show(
            ProgressUiState.Loaded(
                UsageStatistics(LocalDate.of(2026, 3, 2)),
                EMPTY_ACTIVITY,
                Streak(current = 4, best = 12),
            ),
        )

        composeRule.onNodeWithText("4 days in a row", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.progress_streak_rule), substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("12 days in a row").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `explains in French how to start a streak`() {
        show(ProgressUiState.Loaded(UsageStatistics(LocalDate.of(2026, 3, 2)), EMPTY_ACTIVITY))

        composeRule.onNodeWithText("Triez tous les souvenirs d'aujourd'hui pour en commencer une.").assertIsDisplayed()
        composeRule.onNodeWithText("Aucune pour l'instant").assertIsDisplayed()
    }

    /** Page avec le calendrier [calendar] ; aujourd'hui, le 15 juillet 2026. */
    private fun showCalendar(calendar: YearMemories?) {
        show(
            ProgressUiState.Loaded(
                UsageStatistics(LocalDate.of(2026, 3, 2)),
                EMPTY_ACTIVITY,
                calendar = calendar,
                today = LocalDate.of(2026, 7, 15),
            ),
        )
    }

    @Test
    fun `opens on the month of today and tells each day apart without relying on colour`() {
        showCalendar(
            YearMemories(
                2026,
                mapOf(
                    LocalDate.of(2026, 7, 15) to DayMemories(memoryCount = 12, remainingCount = 12),
                    LocalDate.of(2026, 7, 3) to DayMemories(memoryCount = 2, remainingCount = 0),
                ),
            ),
        )

        composeRule.onNodeWithText("July 2026").performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(
                "Today, July 15, 2026, 12 memories to sort",
            ).performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("July 3, 2026, all sorted").assertExists()
        composeRule.onNodeWithContentDescription("July 4, 2026, no memories").assertExists().assertHasNoClickAction()
    }

    @Test
    fun `opens a day with memories, even ahead of time`() {
        showCalendar(YearMemories(2026, mapOf(LocalDate.of(2026, 7, 28) to DayMemories(3, 3))))

        composeRule.onNodeWithContentDescription("July 28, 2026, 3 memories to sort").performScrollTo().performClick()

        assertEquals(listOf<ProgressEvent>(ProgressEvent.OpenDay(LocalDate.of(2026, 7, 28))), events)
    }

    @Test
    fun `moves from month to month within the year`() {
        showCalendar(YearMemories(2026, emptyMap()))

        composeRule.onNodeWithContentDescription(text(R.string.progress_calendar_next)).performScrollTo().performClick()
        composeRule.onNodeWithText("August 2026").assertIsDisplayed()
        repeat(2) { composeRule.onNodeWithContentDescription(text(R.string.progress_calendar_previous)).performClick() }

        composeRule.onNodeWithText("June 2026").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `shows the calendar in French, weeks starting on Monday`() {
        showCalendar(YearMemories(2026, mapOf(LocalDate.of(2026, 7, 15) to DayMemories(1, 1))))

        composeRule.onNodeWithText("Juillet 2026").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Aujourd'hui, 15 juillet 2026, 1 média à trier").assertExists()
    }

    @Test
    fun `waits for the gallery before showing the calendar`() {
        showCalendar(null)

        composeRule.onNodeWithText(text(R.string.progress_calendar_section)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.progress_calendar_hint)).assertDoesNotExist()
    }

    /** Page avec les badges et l'objectif [achievements]. */
    private fun showAchievements(achievements: Achievements) {
        show(
            ProgressUiState.Loaded(
                UsageStatistics(LocalDate.of(2026, 3, 2)),
                EMPTY_ACTIVITY,
                achievements = achievements,
            ),
        )
    }

    private val someBadges =
        listOf(
            BadgeStatus(Badge.FIRST_SORT, earned = true, remaining = 0),
            BadgeStatus(Badge.STREAK_7, earned = false, remaining = 3),
            BadgeStatus(Badge.FREED_5_GB, earned = false, remaining = 2 * GIGABYTE),
            BadgeStatus(Badge.WEEK_CAUGHT_UP, earned = false, remaining = 1),
        )

    @Test
    fun `lists the badges with what is left to earn them`() {
        showAchievements(Achievements(someBadges))

        composeRule.onNodeWithText("Badges, 1 of 4").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.progress_badge_earned)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("3 more days in a row").performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithText("${fileSizeLabel(composeRule.activity, 2 * GIGABYTE)} more to free")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.progress_badge_week_left)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `offers to set a goal, then shows how far it is`() {
        showAchievements(Achievements(someBadges))
        composeRule.onNodeWithText(text(R.string.progress_goal_none)).performScrollTo().performClick()
        assertEquals(listOf<ProgressEvent>(ProgressEvent.PickGoal), events)

        events.clear()
        showAchievements(Achievements(someBadges, goal = GoalProgress(FreeSpaceGoal(5 * GIGABYTE, 0), 2 * GIGABYTE)))
        composeRule
            .onNodeWithText(
                "${fileSizeLabel(composeRule.activity, 2 * GIGABYTE)} freed of ${fileSizeLabel(
                    composeRule.activity,
                    5 * GIGABYTE,
                )}",
            ).performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.progress_goal_remove)).performScrollTo().performClick()
        assertEquals(listOf<ProgressEvent>(ProgressEvent.RemoveGoal), events)
    }

    @Test
    @Config(qualifiers = "fr")
    fun `picks a goal among the sizes offered, in French`() {
        show(
            ProgressUiState.Loaded(
                UsageStatistics(LocalDate.of(2026, 3, 2)),
                EMPTY_ACTIVITY,
                achievements = Achievements(someBadges),
                goalDialogOpen = true,
            ),
        )

        composeRule.onNodeWithText("Place à libérer").assertIsDisplayed()
        composeRule.onNodeWithText(fileSizeLabel(composeRule.activity, 10 * GIGABYTE)).performClick()
        composeRule.onNodeWithText("OK").performClick()

        assertEquals(listOf<ProgressEvent>(ProgressEvent.GoalPicked(10 * GIGABYTE)), events)
    }

    @Test
    fun `offers to try again when the statistics cannot be read`() {
        show(ProgressUiState.Error)

        composeRule.onNodeWithText(text(R.string.progress_error)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_retry)).performClick()

        assertEquals(listOf<ProgressEvent>(ProgressEvent.Retry), events)
    }

    @Test
    fun `goes back from the top bar`() {
        show(ProgressUiState.Loading)

        composeRule.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()

        assertEquals(listOf<ProgressEvent>(ProgressEvent.Back), events)
    }
}
