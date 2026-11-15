package fr.babeuloula.photoswipe.ui.trash

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.fixtures.aMediaItem
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.domain.model.Trash
import fr.babeuloula.photoswipe.domain.model.TrashItem
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

/** Page « Corbeille » composée seule dans `MainActivity`, avec des états fixés. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class TrashScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<TrashEvent>()

    /** Midi en UTC : le même jour dans tous les fuseaux de l'Europe, où tournent les tests. */
    private val purgeAt = Instant.parse("2026-12-14T12:00:00Z")
    private val item = TrashItem(aMediaItem(id = 7, sizeBytes = 2_400_000), purgeAt.minusSeconds(THIRTY_DAYS), purgeAt)

    private fun show(state: TrashUiState) {
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                PhotoSwipeTheme { TrashScreen(state = state, onEvent = { events += it }) }
            }
        }
    }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    @Test
    fun `says it only shows PhotoSwipe deletions, with the space still taken and the purge date`() {
        show(TrashUiState.Loaded(Trash(listOf(item))))

        composeRule.onNodeWithText(text(R.string.trash_notice)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.trash_occupied)).assertIsDisplayed()
        composeRule.onAllNodesWithText("2.4 MB").onFirst().assertIsDisplayed()
        val day = purgeAt.atZone(ZoneId.systemDefault()).toLocalDate()
        composeRule.onNodeWithText("Deleted for good on Dec ${day.dayOfMonth}, 2026").assertIsDisplayed()
    }

    @Test
    fun `restores or deletes a media for good`() {
        show(TrashUiState.Loaded(Trash(listOf(item))))

        composeRule.onNodeWithText(text(R.string.trash_restore)).performClick()
        composeRule.onNodeWithText(text(R.string.trash_delete_forever)).performClick()

        assertEquals(listOf(TrashEvent.Restore(MediaId(7)), TrashEvent.DeleteForever(MediaId(7))), events)
    }

    @Test
    fun `disables the actions during a system request`() {
        show(TrashUiState.Loaded(Trash(listOf(item)), busy = true))

        composeRule.onNodeWithText(text(R.string.trash_restore)).assertIsNotEnabled()
    }

    @Test
    fun `says when nothing deleted with PhotoSwipe is left in the trash`() {
        show(TrashUiState.Loaded(Trash()))

        composeRule.onNodeWithTag(TRASH_EMPTY_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.trash_notice)).assertIsDisplayed()
    }

    @Test
    fun `confirms a permanent deletion when Android will not ask`() {
        show(TrashUiState.Loaded(Trash(listOf(item)), confirmingDeletion = item))

        composeRule.onNodeWithText(text(R.string.trash_delete_confirm_title)).assertIsDisplayed()
        composeRule
            .onNode(hasText(text(R.string.trash_delete_forever)) and hasAnyAncestor(isDialog()))
            .performClick()

        assertEquals(TrashEvent.DeletionConfirmed, events.last())
    }

    private val other = TrashItem(aMediaItem(id = 8), purgeAt.minusSeconds(THIRTY_DAYS), purgeAt)
    private val video =
        TrashItem(aMediaItem(id = 9, type = MediaType.VIDEO), purgeAt.minusSeconds(THIRTY_DAYS), purgeAt)

    /** Nœud de la vue en grand, dans sa fenêtre. */
    private fun inViewer(text: String) = hasText(text) and hasAnyAncestor(hasTestTag(TRASH_VIEWER_TAG))

    @Test
    fun `opens a media in large when it is tapped`() {
        show(TrashUiState.Loaded(Trash(listOf(item))))

        composeRule
            .onNodeWithTag("$TRASH_OPEN_TAG${item.media.id.value}")
            .assert(
                SemanticsMatcher("click label") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == text(R.string.photo_viewer_open)
                },
            ).performClick()

        assertEquals(TrashEvent.View(MediaId(7)), events.last())
    }

    @Test
    fun `shows the media in large with its place, and goes to the next one`() {
        show(TrashUiState.Loaded(Trash(listOf(item, other)), viewing = MediaId(7)))

        composeRule.onNodeWithTag(TRASH_VIEWER_TAG).assertIsDisplayed()
        composeRule.onNode(inViewer("1 of 2")).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.trash_viewer_previous)).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription(text(R.string.trash_viewer_next)).performClick()

        assertEquals(TrashEvent.View(MediaId(8)), events.last())
    }

    @Test
    fun `restores, deletes for good or closes from the large view`() {
        show(TrashUiState.Loaded(Trash(listOf(item, other)), viewing = MediaId(8)))

        composeRule.onNode(inViewer(text(R.string.trash_restore))).performClick()
        composeRule.onNode(inViewer(text(R.string.trash_delete_forever))).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.photo_viewer_close)).performClick()

        assertEquals(
            listOf(TrashEvent.Restore(MediaId(8)), TrashEvent.DeleteForever(MediaId(8)), TrashEvent.View(null)),
            events,
        )
        composeRule.onNodeWithContentDescription(text(R.string.trash_viewer_next)).assertIsNotEnabled()
    }

    @Test
    fun `disables the large view actions during a system request`() {
        show(TrashUiState.Loaded(Trash(listOf(item)), busy = true, viewing = MediaId(7)))

        composeRule.onNode(inViewer(text(R.string.trash_restore))).assertIsNotEnabled()
        composeRule.onNode(inViewer(text(R.string.trash_delete_forever))).assertIsNotEnabled()
    }

    @Test
    fun `offers to play a video of the trash`() {
        show(TrashUiState.Loaded(Trash(listOf(video)), viewing = MediaId(9)))

        composeRule.onNodeWithTag(TRASH_VIEWER_PLAY_TAG).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(R.string.deck_video_play)).assertHasClickAction()
    }

    private companion object {
        const val THIRTY_DAYS = 30L * 24 * 60 * 60
    }
}
