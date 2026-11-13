package fr.babeuloula.photoswipe.ui.deck

import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.usecase.MarkForDeletion
import fr.babeuloula.photoswipe.ui.common.CONFETTI_TAG
import fr.babeuloula.photoswipe.ui.common.CelebrationHost
import fr.babeuloula.photoswipe.ui.common.LocalAnimationsDisabled
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Célébration de fin de pile (PS-174) : la vraie pile, reliée à son ViewModel, sous la couche de
 * confettis de l'application.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class DeckCelebrationTest : DeckViewModelTestBase() {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private var summaryOpened = false
    private var closed = false

    /** Retours haptiques demandés pendant le test. */
    private val haptics = mutableListOf<HapticFeedbackType>()

    private val recordingHaptics =
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                haptics += hapticFeedbackType
            }
        }

    /** Pile d'un seul souvenir, sous la couche de confettis ; l'horloge passe ensuite en manuel. */
    private fun show(
        viewModel: DeckViewModel,
        celebrationEnabled: Boolean,
        animationsDisabled: Boolean = false,
    ) {
        composeRule.activity.setContent {
            CompositionLocalProvider(
                LocalImageLoader provides ImageLoader(composeRule.activity),
                LocalHapticFeedback provides recordingHaptics,
                LocalAnimationsDisabled provides animationsDisabled,
            ) {
                PhotoSwipeTheme {
                    CelebrationHost(enabled = celebrationEnabled) {
                        DeckRoute(
                            onBack = { closed = true },
                            onOpenSummary = { summaryOpened = true },
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false
    }

    /** Trie la dernière carte, puis laisse passer quelques images. */
    private fun sortLastCard(viewModel: DeckViewModel) {
        composeRule.runOnUiThread { viewModel.onEvent(DeckEvent.Keep) }
        repeat(FRAMES) { composeRule.mainClock.advanceTimeByFrame() }
    }

    @Test
    fun `throws confetti over the summary once the last card of the day is sorted, with a short vibration`() {
        val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020))))
        show(viewModel, celebrationEnabled = true)

        sortLastCard(viewModel)

        assertTrue(summaryOpened)
        composeRule.onNodeWithTag(CONFETTI_TAG).assertExists()
        assertEquals(listOf(HapticFeedbackType.Confirm), haptics)

        composeRule.mainClock.advanceTimeBy(CELEBRATION_OVER_MILLIS)
        composeRule.onNodeWithTag(CONFETTI_TAG).assertDoesNotExist()
    }

    @Test
    fun `opens the summary without confetti when the celebration is off`() {
        val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020))))
        show(viewModel, celebrationEnabled = false)

        sortLastCard(viewModel)

        assertTrue(summaryOpened)
        composeRule.onNodeWithTag(CONFETTI_TAG).assertDoesNotExist()
        assertEquals(emptyList<HapticFeedbackType>(), haptics)
    }

    @Test
    fun `opens the summary without confetti when the animations are reduced`() {
        val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2020))))
        show(viewModel, celebrationEnabled = true, animationsDisabled = true)

        sortLastCard(viewModel)

        assertTrue(summaryOpened)
        composeRule.onNodeWithTag(CONFETTI_TAG).assertDoesNotExist()
    }

    @Test
    fun `goes back to the summary without confetti at the end of the marked deck`() {
        val media = takenOn(1, 2020)
        runTest { MarkForDeletion(repository, journal, clock)(media, day) }
        val viewModel = deckViewModel(FakeMediaSource(listOf(media)), route = emptyMap())
        show(viewModel, celebrationEnabled = true)

        sortLastCard(viewModel)

        assertTrue(closed)
        composeRule.onNodeWithTag(CONFETTI_TAG).assertDoesNotExist()
    }

    private companion object {
        /** Images laissées passer après le tri : l'effet est livré, les confettis sont composés. */
        const val FRAMES = 5

        /** Au-delà de la durée des confettis. */
        const val CELEBRATION_OVER_MILLIS = 4_000L
    }
}
