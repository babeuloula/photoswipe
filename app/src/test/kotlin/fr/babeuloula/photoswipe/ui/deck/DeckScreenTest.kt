package fr.babeuloula.photoswipe.ui.deck

import android.content.Context
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.domain.model.MediaType
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDate

/** Pile composée seule dans `MainActivity`, avec des états fixés (langue anglaise de Robolectric). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class DeckScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val events = mutableListOf<DeckEvent>()

    /** Affiche [initial] ; l'état renvoyé permet de le faire évoluer sans recomposer tout l'écran. */
    private fun show(
        initial: DeckUiState,
        videoPlayer: CardVideoPlayer? = null,
    ): MutableState<DeckUiState> {
        val state = mutableStateOf(initial)
        composeRule.activity.setContent {
            CompositionLocalProvider(
                LocalImageLoader provides ImageLoader(composeRule.activity),
                LocalHapticFeedback provides recordingHaptics,
                LocalCardVideoPlayer provides videoPlayer,
            ) {
                PhotoSwipeTheme {
                    DeckScreen(
                        day = LocalDate.of(2026, 5, 14),
                        state = state.value,
                        onEvent = { events += it },
                    )
                }
            }
        }
        return state
    }

    /** Retours haptiques demandés pendant le test. */
    private val haptics = mutableListOf<HapticFeedbackType>()

    private val recordingHaptics =
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                haptics += hapticFeedbackType
            }
        }

    private fun text(id: Int): String = composeRule.activity.getString(id)

    /**
     * Lecteur qui note les demandes ; la surface vidéo reçoit un vrai ExoPlayer, jamais préparé.
     * [playingUri] et [position] se règlent à la main : la lecture n'avance pas seule.
     */
    private class RecordingVideoPlayer(
        context: Context,
    ) : CardVideoPlayer {
        val calls = mutableListOf<String>()
        override val player: Player = ExoPlayer.Builder(context).build()
        override var playingUri: String? by mutableStateOf(null)
        var position: Duration = Duration.ZERO

        override fun play(uri: String) {
            calls += "play $uri"
        }

        override fun position(uri: String): Duration = position

        override fun pauseByUser(uri: String) {
            calls += "pause $uri"
        }

        override fun stop(uri: String) {
            calls += "stop $uri"
        }

        override fun pause() {
            calls += "pause"
        }

        override fun resume() {
            calls += "resume"
        }

        override fun release() {
            player.release()
        }
    }

    private val video =
        PREVIEW_CARD.copy(
            type = MediaType.VIDEO,
            uri = "content://media/external/video/media/1",
            duration = Duration.ofSeconds(32),
        )

    /** Affiche [initial] avec [videoPlayer], puis met l'horloge en mode manuel. */
    private fun showWithPlayer(
        initial: DeckUiState,
        videoPlayer: CardVideoPlayer,
    ): MutableState<DeckUiState> {
        composeRule.mainClock.autoAdvance = false
        val state = show(initial, videoPlayer)
        repeat(SETTLE_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }
        return state
    }

    /** Change un état lu par l'écran alors que l'horloge est manuelle, et le signale à Compose. */
    private fun update(block: () -> Unit) {
        composeRule.runOnUiThread {
            block()
            Snapshot.sendApplyNotifications()
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun advance(duration: Duration) {
        composeRule.mainClock.advanceTimeBy(duration.toMillis())
    }

    @Test
    fun `waits a moment before playing the video of the top card`() {
        val videoPlayer = RecordingVideoPlayer(composeRule.activity)
        showWithPlayer(DeckUiState.Loaded(listOf(video)), videoPlayer)

        advance(AUTOPLAY_DELAY.minusMillis(TOLERANCE_MILLIS))
        assertTrue(videoPlayer.calls.isEmpty())

        advance(Duration.ofMillis(2 * TOLERANCE_MILLIS))
        assertEquals(listOf("play ${video.uri}"), videoPlayer.calls)
        videoPlayer.release()
    }

    @Test
    fun `does not play the next card prepared underneath`() {
        val videoPlayer = RecordingVideoPlayer(composeRule.activity)
        val nextVideo = video.copy(id = MediaId(2), uri = "content://media/external/video/media/2")
        showWithPlayer(DeckUiState.Loaded(listOf(PREVIEW_CARD, nextVideo)), videoPlayer)

        advance(AUTOPLAY_DELAY.multipliedBy(2))

        assertTrue(videoPlayer.calls.isEmpty())
        videoPlayer.release()
    }

    @Test
    fun `does not play a video whose card left before the delay`() {
        val videoPlayer = RecordingVideoPlayer(composeRule.activity)
        val state = showWithPlayer(DeckUiState.Loaded(listOf(video)), videoPlayer)

        update { state.value = DeckUiState.Empty() }
        advance(AUTOPLAY_DELAY.multipliedBy(2))

        assertEquals(listOf("stop ${video.uri}"), videoPlayer.calls)
        videoPlayer.release()
    }

    @Test
    fun `stops the video when the deck is emptied`() {
        val videoPlayer = RecordingVideoPlayer(composeRule.activity)
        val state = showWithPlayer(DeckUiState.Loaded(listOf(video)), videoPlayer)
        advance(AUTOPLAY_DELAY.plusMillis(TOLERANCE_MILLIS))

        update { state.value = DeckUiState.Empty() }

        assertEquals(listOf("play ${video.uri}", "stop ${video.uri}"), videoPlayer.calls)
        videoPlayer.release()
    }

    @Test
    fun `stops the video when its card leaves and plays the next one after the delay`() {
        val videoPlayer = RecordingVideoPlayer(composeRule.activity)
        val nextVideo = video.copy(id = MediaId(2), uri = "content://media/external/video/media/2")
        val state = showWithPlayer(DeckUiState.Loaded(listOf(video, nextVideo)), videoPlayer)
        advance(AUTOPLAY_DELAY.plusMillis(TOLERANCE_MILLIS))

        update { state.value = DeckUiState.Loaded(listOf(nextVideo)) }
        advance(AUTOPLAY_DELAY.plusMillis(TOLERANCE_MILLIS))

        assertEquals(listOf("play ${video.uri}", "stop ${video.uri}", "play ${nextVideo.uri}"), videoPlayer.calls)
        videoPlayer.release()
    }

    @Test
    fun `plays at once from the badge without waiting`() {
        val videoPlayer = RecordingVideoPlayer(composeRule.activity)
        showWithPlayer(DeckUiState.Loaded(listOf(video)), videoPlayer)

        composeRule.onNodeWithContentDescription("Play video, 00:32").performClick()

        assertEquals(listOf("play ${video.uri}"), videoPlayer.calls)
        videoPlayer.release()
    }

    @Test
    fun `turns the badge into a pause button with the remaining time while playing`() {
        val videoPlayer = RecordingVideoPlayer(composeRule.activity)
        showWithPlayer(DeckUiState.Loaded(listOf(video)), videoPlayer)

        update {
            videoPlayer.position = Duration.ofSeconds(10)
            videoPlayer.playingUri = video.uri
        }
        advance(Duration.ofMillis(TOLERANCE_MILLIS))

        composeRule.onNodeWithContentDescription("Pause video, 00:22").performClick()
        assertEquals(listOf("pause ${video.uri}"), videoPlayer.calls)
        videoPlayer.release()
    }

    @Test
    fun `shows the full duration again once the video has ended`() {
        val videoPlayer = RecordingVideoPlayer(composeRule.activity)
        showWithPlayer(DeckUiState.Loaded(listOf(video)), videoPlayer)
        update {
            videoPlayer.position = Duration.ofSeconds(31)
            videoPlayer.playingUri = video.uri
        }
        advance(Duration.ofMillis(TOLERANCE_MILLIS))

        update {
            videoPlayer.position = Duration.ZERO
            videoPlayer.playingUri = null
        }
        advance(Duration.ofMillis(TOLERANCE_MILLIS))

        composeRule.onNodeWithContentDescription("Play video, 00:32").assertExists()
        videoPlayer.release()
    }

    @Test
    fun `counts the remaining time down while playing`() {
        val videoPlayer = RecordingVideoPlayer(composeRule.activity)
        showWithPlayer(DeckUiState.Loaded(listOf(video)), videoPlayer)
        update { videoPlayer.playingUri = video.uri }
        advance(Duration.ofMillis(TOLERANCE_MILLIS))

        videoPlayer.position = Duration.ofSeconds(5)
        advance(Duration.ofSeconds(1))

        composeRule.onNodeWithContentDescription("Pause video, 00:27").assertExists()
        videoPlayer.release()
    }

    @Test
    fun `shows the memory label and the file size of the top card`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD, PREVIEW_CARD.copy(yearsAgo = 5))))

        composeRule.onNodeWithText("3 years ago · May 14, 2023").assertIsDisplayed()
        composeRule.onNodeWithText("3.5 MB").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Photo taken on May 14, 2023").assertIsDisplayed()
    }

    @Test
    fun `describes a video as a video`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(type = MediaType.VIDEO))))

        composeRule.onNodeWithContentDescription("Video taken on May 14, 2023").assertIsDisplayed()
    }

    @Test
    fun `shows the duration of a video on its card`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(type = MediaType.VIDEO, duration = Duration.ofSeconds(32)))))

        composeRule.onNodeWithContentDescription("Duration: 00:32").assertIsDisplayed()
    }

    @Test
    fun `shows no video badge on a photo`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithContentDescription("Duration", substring = true).assertDoesNotExist()
    }

    @Test
    fun `says when the image cannot be opened`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(uri = "content://media/external/images/media/404"))))

        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.deck_media_unavailable)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("3 years ago · May 14, 2023").assertIsDisplayed()
    }

    @Test
    fun `shows the day in the title`() {
        show(DeckUiState.Loading)

        composeRule
            .onNodeWithText(
                composeRule.activity.getString(R.string.deck_title, "May 14, 2026"),
            ).assertIsDisplayed()
    }

    @Test
    fun `finishes and goes back from the top bar`() {
        show(DeckUiState.Loading)

        composeRule.onNodeWithText(text(R.string.deck_finish)).performClick()
        composeRule.onNodeWithContentDescription(text(R.string.navigate_back)).performClick()

        assertEquals(listOf(DeckEvent.Finish, DeckEvent.Back), events)
    }

    @Test
    fun `says when nothing is left to sort`() {
        show(DeckUiState.Empty())

        composeRule.onNodeWithText(text(R.string.deck_empty)).assertIsDisplayed()
    }

    @Test
    fun `retries after an error`() {
        show(DeckUiState.Error)

        composeRule.onNodeWithText(text(R.string.deck_error)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.deck_retry)).performClick()

        assertEquals(listOf<DeckEvent>(DeckEvent.Retry), events)
    }

    private fun card() = composeRule.onNodeWithContentDescription("Photo taken on May 14, 2023")

    @Test
    fun `keeps the card when swiped right past the threshold`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        card().performTouchInput { swipeRight() }
        composeRule.waitForIdle()

        assertEquals(listOf<DeckEvent>(DeckEvent.Keep), events)
    }

    @Test
    fun `brings the card back to the center after a short swipe`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))
        val start = card().fetchSemanticsNode().boundsInRoot

        card().performTouchInput { swipeRight(startX = centerX, endX = centerX + width * SWIPE_THRESHOLD / 2) }
        composeRule.waitForIdle()

        assertTrue(events.isEmpty())
        assertEquals(start, card().fetchSemanticsNode().boundsInRoot)
    }

    @Test
    fun `marks the card for deletion when swiped left past the threshold`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        card().performTouchInput { swipeLeft() }
        composeRule.waitForIdle()

        assertEquals(listOf<DeckEvent>(DeckEvent.MarkForDeletion), events)
    }

    @Test
    fun `brings the card back to the center after a short swipe to the left`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))
        val start = card().fetchSemanticsNode().boundsInRoot

        card().performTouchInput { swipeLeft(startX = centerX, endX = centerX - width * SWIPE_THRESHOLD / 2) }
        composeRule.waitForIdle()

        assertTrue(events.isEmpty())
        assertEquals(start, card().fetchSemanticsNode().boundsInRoot)
    }

    @Test
    fun `shows a green indicator while dragging right and vibrates once past the threshold`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        card().performTouchInput {
            down(center)
            moveBy(Offset(width * 0.2f, 0f))
        }
        composeRule.onNodeWithTag(KEEP_INDICATOR_TAG).assertExists()
        composeRule.onNodeWithTag(DELETE_INDICATOR_TAG).assertDoesNotExist()
        assertTrue(haptics.isEmpty())

        card().performTouchInput {
            moveBy(Offset(width * 0.3f, 0f))
            moveBy(Offset(width * 0.1f, 0f))
        }
        assertEquals(listOf(HapticFeedbackType.GestureThresholdActivate), haptics)

        card().performTouchInput { up() }
    }

    @Test
    fun `shows a red indicator while dragging left and vibrates past the threshold`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        card().performTouchInput {
            down(center)
            moveBy(Offset(-width * 0.6f, 0f))
        }

        composeRule.onNodeWithTag(DELETE_INDICATOR_TAG).assertExists()
        composeRule.onNodeWithTag(KEEP_INDICATOR_TAG).assertDoesNotExist()
        assertEquals(listOf(HapticFeedbackType.GestureThresholdActivate), haptics)
        card().performTouchInput { up() }
    }

    @Test
    fun `shows no indicator while the card springs back to the center`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))
        card().performTouchInput {
            down(center)
            moveBy(Offset(width * 0.25f, 0f))
        }
        composeRule.onNodeWithTag(KEEP_INDICATOR_TAG).assertExists()

        composeRule.mainClock.autoAdvance = false
        card().performTouchInput { up() }
        repeat(BOUNCE_FRAMES) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.onNodeWithTag(KEEP_INDICATOR_TAG).assertDoesNotExist()
            composeRule.onNodeWithTag(DELETE_INDICATOR_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `keeps the card with the keep button, like a swipe right`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithContentDescription(text(R.string.deck_keep)).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<DeckEvent>(DeckEvent.Keep), events)
    }

    @Test
    fun `marks the card with the delete button, like a swipe left`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithContentDescription(text(R.string.deck_mark_for_deletion)).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<DeckEvent>(DeckEvent.MarkForDeletion), events)
    }

    @Test
    fun `animates the card out like a swipe before keeping it from the button`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))
        val start = card().fetchSemanticsNode().boundsInRoot
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithContentDescription(text(R.string.deck_keep)).performClick()
        repeat(EXIT_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }

        composeRule.onNodeWithTag(KEEP_INDICATOR_TAG).assertExists()
        assertTrue(card().fetchSemanticsNode().boundsInRoot.left > start.left)
        assertTrue(events.isEmpty())

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals(listOf<DeckEvent>(DeckEvent.Keep), events)
    }

    @Test
    fun `animates the card out to the left before marking it from the button`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))
        val start = card().fetchSemanticsNode().boundsInRoot
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithContentDescription(text(R.string.deck_mark_for_deletion)).performClick()
        repeat(EXIT_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }

        composeRule.onNodeWithTag(DELETE_INDICATOR_TAG).assertExists()
        assertTrue(card().fetchSemanticsNode().boundsInRoot.left < start.left)

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals(listOf<DeckEvent>(DeckEvent.MarkForDeletion), events)
    }

    @Test
    fun `ignores a second tap while the card is leaving`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithContentDescription(text(R.string.deck_keep)).performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithContentDescription(text(R.string.deck_mark_for_deletion)).performClick()

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals(listOf<DeckEvent>(DeckEvent.Keep), events)
    }

    @Test
    fun `undoes from the undo button when a decision can be undone`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD), canUndo = true))

        composeRule.onNodeWithContentDescription(text(R.string.deck_undo)).assertIsEnabled().performClick()

        assertEquals(listOf<DeckEvent>(DeckEvent.Undo), events)
    }

    @Test
    fun `disables the undo button when there is nothing to undo`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD), canUndo = false))

        composeRule.onNodeWithContentDescription(text(R.string.deck_undo)).assertIsNotEnabled()
    }

    @Test
    fun `offers undo on an emptied deck`() {
        show(DeckUiState.Empty(canUndo = true))

        composeRule.onNodeWithContentDescription(text(R.string.deck_undo)).performClick()

        assertEquals(listOf<DeckEvent>(DeckEvent.Undo), events)
    }

    @Test
    fun `brings an undone card back from the side it left`() {
        val state = show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))
        val center = card().fetchSemanticsNode().positionInRoot
        composeRule.mainClock.autoAdvance = false

        state.value = DeckUiState.Loaded(listOf(PREVIEW_CARD.copy(id = MediaId(2))), returningFrom = CardSide.RIGHT)
        // Les bornes sont coupées au bord de l'écran : on compare la position, qui suit la translation.
        // On avance image par image jusqu'à ce que la nouvelle carte soit posée, hors du centre.
        var frames = 0
        while (card().fetchSemanticsNode().positionInRoot == center && frames < MAX_FRAMES) {
            composeRule.mainClock.advanceTimeByFrame()
            frames++
        }
        assertTrue(card().fetchSemanticsNode().positionInRoot.x > center.x)

        composeRule.mainClock.autoAdvance = true
        composeRule.waitForIdle()
        assertEquals(center, card().fetchSemanticsNode().positionInRoot)
    }

    @Test
    fun `opens the menu when the card is swiped up`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        card().performTouchInput { swipeUp() }
        composeRule.waitForIdle()

        assertEquals(listOf<DeckEvent>(DeckEvent.OpenMenu), events)
    }

    @Test
    fun `shows a blue indicator while dragging up and vibrates past the menu threshold`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        card().performTouchInput {
            down(center)
            moveBy(Offset(0f, -height * 0.4f))
        }

        composeRule.onNodeWithTag(FAVORITE_INDICATOR_TAG).assertExists()
        composeRule.onNodeWithTag(KEEP_INDICATOR_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(DELETE_INDICATOR_TAG).assertDoesNotExist()
        assertEquals(listOf(HapticFeedbackType.GestureThresholdActivate), haptics)

        card().performTouchInput { up() }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(FAVORITE_INDICATOR_TAG).assertDoesNotExist()
    }

    @Test
    fun `does not open the menu on a short swipe up`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        card().performTouchInput { swipeUp(startY = centerY, endY = centerY - height * MENU_THRESHOLD / 2) }
        composeRule.waitForIdle()

        assertTrue(events.isEmpty())
    }

    @Test
    fun `opens the menu from the menu button`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithContentDescription(text(R.string.deck_menu)).performClick()

        assertEquals(listOf<DeckEvent>(DeckEvent.OpenMenu), events)
    }

    @Test
    fun `sends the card up and favorites it from the menu`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD), menuOpen = true))

        composeRule.onNodeWithText(text(R.string.deck_favorite)).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf(DeckEvent.CloseMenu, DeckEvent.Favorite), events)
    }

    @Test
    fun `shares the card from the menu`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD), menuOpen = true))

        composeRule.onNodeWithText(text(R.string.deck_share)).performClick()
        composeRule.waitForIdle()

        assertEquals(listOf<DeckEvent>(DeckEvent.Share), events)
    }

    @Test
    fun `shows a message from the snackbar host`() {
        val host = SnackbarHostState()
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides ImageLoader(composeRule.activity)) {
                PhotoSwipeTheme {
                    DeckScreen(
                        day = LocalDate.of(2026, 5, 14),
                        state = DeckUiState.Loaded(listOf(PREVIEW_CARD)),
                        onEvent = {},
                        snackbarHostState = host,
                    )
                }
            }
        }

        composeRule.activity.lifecycleScope.launch { host.showSnackbar(text(R.string.deck_share_failed)) }

        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.onAllNodesWithText(text(R.string.deck_share_failed)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun `prepares the next card under the top one, hidden from TalkBack`() {
        show(
            DeckUiState.Loaded(
                listOf(PREVIEW_CARD, PREVIEW_CARD.copy(id = MediaId(2), yearsAgo = 5)),
                remainingCount = 40,
            ),
        )

        composeRule.onNodeWithTag(NEXT_CARD_TAG).assertExists()
        composeRule.onNodeWithText("3 years ago · May 14, 2023").assertIsDisplayed()
        composeRule.onNodeWithText("5 years ago · May 14, 2023").assertDoesNotExist()
    }

    @Test
    fun `prepares no next card under the last one`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithTag(NEXT_CARD_TAG).assertDoesNotExist()
    }

    @Test
    fun `shows no menu while it is closed`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithText(text(R.string.deck_favorite)).assertDoesNotExist()
    }

    @Test
    fun `gives the decision buttons accessible touch targets`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD), canUndo = true))

        listOf(
            R.string.deck_keep,
            R.string.deck_mark_for_deletion,
            R.string.deck_undo,
            R.string.deck_menu,
        ).forEach { label ->
            composeRule
                .onNodeWithContentDescription(text(label))
                .assertHasClickAction()
                .assertIsEnabled()
                .assertWidthIsAtLeast(MIN_TOUCH_TARGET)
                .assertHeightIsAtLeast(MIN_TOUCH_TARGET)
        }
    }

    @Test
    @Config(qualifiers = "fr")
    fun `describes the decision buttons in French`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithContentDescription("Garder").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Marquer pour suppression").assertIsDisplayed()
    }

    @Test
    fun `hides the decision buttons when there is no card`() {
        show(DeckUiState.Empty())

        composeRule.onNodeWithContentDescription(text(R.string.deck_keep)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(text(R.string.deck_mark_for_deletion)).assertDoesNotExist()
    }

    @Test
    fun `shows no indicator when the card is at rest`() {
        show(DeckUiState.Loaded(listOf(PREVIEW_CARD)))

        composeRule.onNodeWithTag(KEEP_INDICATOR_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(DELETE_INDICATOR_TAG).assertDoesNotExist()
    }

    private companion object {
        const val TIMEOUT_MILLIS = 5_000L

        /** Images pour composer l'écran avant de mesurer le délai de lecture. */
        const val SETTLE_FRAMES = 2

        /** Marge autour du délai de lecture automatique. */
        const val TOLERANCE_MILLIS = 100L

        /** Cible tactile minimale de Material 3 (SPEC §8). */
        val MIN_TOUCH_TARGET = 48.dp

        /** Images observées pendant le retour au centre, rebond compris (environ une demi-seconde). */
        const val BOUNCE_FRAMES = 40

        /** Quelques images après un tap : la carte a commencé à sortir sans avoir fini. */
        const val EXIT_FRAMES = 3

        /** Nombre maximal d'images attendues pour voir apparaître une carte annulée. */
        const val MAX_FRAMES = 20
    }
}
