package fr.babeuloula.photoswipe.ui.summary

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.SessionSummary
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Résumé composé seul dans `MainActivity`, avec des états fixés. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class SummaryScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<SummaryEvent>()

    private fun show(state: SummaryUiState) {
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                PhotoSwipeTheme { SummaryScreen(state = state, onEvent = { events += it }) }
            }
        }
    }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    private val summary =
        SessionSummary(keptCount = 12, markedForDeletionCount = 4, favoriteCount = 1, estimatedFreedBytes = 48_000_000)

    @Test
    fun `puts the estimated freed space first`() {
        show(SummaryUiState.Loaded(summary))

        composeRule.onNodeWithText(text(R.string.summary_freed_space)).assertIsDisplayed()
        composeRule.onNodeWithText("48 MB").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `writes a freed space under a kilobyte without parentheses`() {
        show(SummaryUiState.Loaded(summary.copy(markedForDeletionCount = 1, estimatedFreedBytes = 0)))

        composeRule.onNodeWithText("0 octet").assertIsDisplayed()
    }

    @Test
    fun `shows no freed space at all when nothing is marked nor deleted today`() {
        show(SummaryUiState.Loaded(nothingMarked))

        composeRule.onNodeWithTag(FREED_SPACE_TAG).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.summary_freed_space)).assertDoesNotExist()
    }

    @Test
    fun `shows the space freed today once every deletion is confirmed`() {
        show(SummaryUiState.Loaded(nothingMarked.copy(trashedTodayCount = 10, trashedTodayBytes = 25_000_000)))

        composeRule.onNodeWithText(text(R.string.summary_freed_today)).assertIsDisplayed()
        composeRule.onNodeWithText("25 MB").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "10 items, their space freed in 30 days or sooner from the trash",
            ).assertIsDisplayed()
        composeRule.onNodeWithText("10 items sent to the trash (25 MB)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_freed_space)).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `keeps the estimate first and lists what was already deleted today in French`() {
        show(SummaryUiState.Loaded(summary.copy(trashedTodayCount = 1, trashedTodayBytes = 2_400_000)))

        composeRule.onNodeWithText("Espace à libérer").assertIsDisplayed()
        composeRule
            .onNodeWithText(
                "1 média mis à la corbeille (",
                substring = true,
            ).performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `keeps the freed space on a single line`() {
        show(SummaryUiState.Loaded(summary.copy(estimatedFreedBytes = 999_000_000)))

        assertEquals(1, freedSpaceLayout().lineCount)
        assertFalse(freedSpaceLayout().didOverflowWidth)
    }

    @Test
    @Config(qualifiers = "fr", fontScale = 2.0f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `keeps the freed space on a single line with a font at 200 percent`() {
        show(SummaryUiState.Loaded(summary.copy(estimatedFreedBytes = 512)))

        composeRule.onNodeWithText("512 octets").assertIsDisplayed()
        assertEquals(1, freedSpaceLayout().lineCount)
        assertFalse(freedSpaceLayout().didOverflowWidth)
    }

    /** Mise en page du texte de l'espace libéré. */
    private fun freedSpaceLayout(): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithTag(FREED_SPACE_TAG).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        return layouts.single()
    }

    @Test
    fun `shows what was kept and favorited today, then what waits for confirmation`() {
        show(SummaryUiState.Loaded(summary))

        composeRule.onNodeWithText("12 items kept").assertIsDisplayed()
        composeRule.onNodeWithText("4 items marked for deletion").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1 favorite").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_sorted_today)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_to_confirm)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `shows the summary in French`() {
        show(SummaryUiState.Loaded(summary.copy(keptCount = 1, markedForDeletionCount = 2, favoriteCount = 0)))

        composeRule.onNodeWithText("1 média gardé").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2 médias marqués pour suppression").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("0 favori").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Trié aujourd'hui").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Espace à libérer").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `previews the last marked media and says how many others wait, and what the freed space depends on`() {
        val previews = (1L..5L).map { aMediaItem(id = it) }
        show(SummaryUiState.Loaded(summary.copy(markedForDeletionCount = 12), markedPreviews = previews))

        composeRule.onNodeWithText("If you confirm the 12 marked items, once they leave the trash").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("and 7 more").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `opens the marked deck on the tapped thumbnail, or from the start on the group`() {
        val previews = (1L..5L).map { aMediaItem(id = it) }
        show(SummaryUiState.Loaded(summary.copy(markedForDeletionCount = 12), markedPreviews = previews))

        composeRule.onNodeWithText("12 items marked for deletion").performScrollTo()
        composeRule.onNodeWithTag("${MARKED_THUMBNAIL_TAG}3", useUnmergedTree = true).performClick()
        composeRule.onNodeWithText("12 items marked for deletion", useUnmergedTree = true).performClick()

        assertEquals(listOf(SummaryEvent.OpenMarked(MediaId(3)), SummaryEvent.OpenMarked(null)), events)
    }

    @Test
    fun `offers TalkBack a labelled action to see the marked media`() {
        show(SummaryUiState.Loaded(summary, markedPreviews = listOf(aMediaItem(id = 1))))

        composeRule
            .onNode(
                SemanticsMatcher("click label") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == "See the marked items"
                },
            ).assertExists()
    }

    @Test
    fun `says nothing is deleted yet`() {
        show(SummaryUiState.Loaded(summary))

        composeRule.onNodeWithText(text(R.string.summary_nothing_deleted_yet)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `goes home later or back with the arrow`() {
        show(SummaryUiState.Loaded(summary))

        composeRule.onNodeWithText(text(R.string.summary_later)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()

        assertEquals(listOf(SummaryEvent.Later, SummaryEvent.Back), events)
    }

    @Test
    fun `confirms the deletion from its button`() {
        show(SummaryUiState.Loaded(summary))

        composeRule
            .onNodeWithText(text(R.string.summary_confirm))
            .assertIsEnabled()
            .performClick()

        assertEquals(listOf<SummaryEvent>(SummaryEvent.Confirm), events)
    }

    private val nothingMarked =
        SessionSummary(keptCount = 3, markedForDeletionCount = 0, favoriteCount = 0, estimatedFreedBytes = 0)

    @Test
    fun `offers only a way back home when nothing is marked`() {
        show(SummaryUiState.Loaded(nothingMarked))

        composeRule.onNodeWithText(text(R.string.summary_back_home)).assertIsDisplayed().performClick()

        assertEquals(listOf<SummaryEvent>(SummaryEvent.Later), events)
        composeRule.onNodeWithText(text(R.string.summary_confirm)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.summary_later)).assertDoesNotExist()
    }

    @Test
    fun `hides what waits for confirmation when nothing is marked`() {
        show(SummaryUiState.Loaded(nothingMarked))

        composeRule.onNodeWithText(text(R.string.summary_sorted_today)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_to_confirm)).assertDoesNotExist()
        composeRule.onNodeWithText(text(R.string.summary_nothing_deleted_yet)).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "fr")
    fun `offers the way back home in French`() {
        show(SummaryUiState.Loaded(nothingMarked))

        composeRule.onNodeWithText("Retour à l'accueil").assertIsDisplayed()
    }

    @Test
    fun `keeps the confirmation and later buttons when something is marked`() {
        show(SummaryUiState.Loaded(summary))

        composeRule.onNodeWithText(text(R.string.summary_confirm)).assertIsEnabled()
        composeRule.onNodeWithText(text(R.string.summary_later)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_back_home)).assertDoesNotExist()
    }

    @Test
    fun `cannot confirm twice while the system dialogs are shown`() {
        show(SummaryUiState.Loaded(summary, confirming = true))

        composeRule.onNodeWithText(text(R.string.summary_confirm)).assertIsNotEnabled()
    }

    @Test
    fun `retries after an error`() {
        show(SummaryUiState.Error)

        composeRule.onNodeWithText(text(R.string.summary_error)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_retry)).performClick()

        assertEquals(listOf<SummaryEvent>(SummaryEvent.Retry), events)
    }

    @Test
    fun `asks its own confirmation before trashing with media management`() {
        show(
            SummaryUiState.Loaded(
                SessionSummary(
                    keptCount = 0,
                    markedForDeletionCount = 3,
                    favoriteCount = 0,
                    estimatedFreedBytes = 9_000_000,
                ),
                askingTrashConfirmation = true,
            ),
        )

        composeRule.onNode(isDialog()).assertExists()
        composeRule
            .onNodeWithText(
                composeRule.activity.resources.getQuantityString(R.plurals.summary_trash_confirm_title, 3, 3),
            ).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_trash_confirm_text)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.summary_trash_confirm_action)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(android.R.string.cancel)).performClick()

        assertEquals(listOf(SummaryEvent.TrashConfirmationAccepted, SummaryEvent.TrashConfirmationDismissed), events)
    }

    @Test
    fun `opens the trash of PhotoSwipe`() {
        show(SummaryUiState.Loaded(summary))

        composeRule.onNodeWithText(text(R.string.summary_open_trash)).performScrollTo().performClick()

        assertEquals(listOf<SummaryEvent>(SummaryEvent.OpenTrash), events)
    }
}
