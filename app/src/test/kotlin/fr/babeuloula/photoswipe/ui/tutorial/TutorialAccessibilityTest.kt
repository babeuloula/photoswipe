package fr.babeuloula.photoswipe.ui.tutorial

import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.common.rememberReducedMotion
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tutoriel adapté aux réglages d'accessibilité (SPEC §8) : animations supprimées, descriptions des
 * animations pour TalkBack, navigation par boutons, grande police.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class TutorialAccessibilityTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun text(
        id: Int,
        vararg args: Any,
    ): String = composeRule.activity.getString(id, *args)

    private fun removeAnimations() {
        Settings.Global.putFloat(composeRule.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
    }

    private fun showTutorial() {
        composeRule.activity.setContent {
            CompositionLocalProvider(LocalImageLoader provides composeRule.activity.imageLoader) {
                PhotoSwipeTheme { TutorialScreen(onDone = {}) }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun `follows the system setting that removes animations`() {
        var reduced: Boolean? = null
        composeRule.activity.setContent { reduced = rememberReducedMotion() }
        composeRule.waitForIdle()
        assertFalse(reduced!!)

        removeAnimations()
        composeRule.activity.setContent { reduced = rememberReducedMotion() }
        composeRule.waitForIdle()

        assertTrue(reduced!!)
    }

    @Test
    fun `with animations removed, next shows the following page at once`() {
        removeAnimations()
        showTutorial()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText(text(R.string.tutorial_next)).performClick()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()

        val pages = TutorialPage.entries.size
        composeRule.onNodeWithContentDescription(text(R.string.tutorial_page_indicator, 2, pages)).assertIsDisplayed()
    }

    @Test
    fun `describes every animation to screen readers, page after page with the buttons alone`() {
        removeAnimations()
        showTutorial()

        TutorialPage.entries.dropLast(1).forEach { page ->
            composeRule.onNodeWithText(text(page.title)).assertIsDisplayed()
            page.illustration?.let { composeRule.onNodeWithContentDescription(text(it)).assertIsDisplayed() }
            composeRule.onNodeWithText(text(R.string.tutorial_next)).performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithText(text(TutorialPage.PRACTICE.title)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.tutorial_start)).assertIsDisplayed()
    }

    @Test
    @Config(fontScale = 2.0f)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `the illustration texts fit, uncut, with a 200 percent font`() {
        TutorialPage.entries.filter { it.illustration != null }.forEach { page ->
            composeRule.activity.setContent {
                PhotoSwipeTheme {
                    Box(Modifier.width(PHONE_CONTENT_WIDTH)) {
                        TutorialIllustration(
                            page,
                            animate = false,
                            modifier = Modifier.illustrationFrame().testTag(FRAME),
                        )
                    }
                }
            }
            composeRule.waitForIdle()
            val frame = composeRule.onNodeWithTag(FRAME).fetchSemanticsNode().boundsInRoot
            composeRule
                .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
                .fetchSemanticsNodes()
                .forEach { node ->
                    val layouts = mutableListOf<TextLayoutResult>()
                    node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                    val label = "${page.name}: ${node.config[SemanticsProperties.Text]}"
                    val layout = layouts.single()
                    // Texte coupé : lignes perdues en hauteur ou points de suspension.
                    val ellipsized = (0 until layout.lineCount).any { layout.isLineEllipsized(it) }
                    assertFalse("cut text $label", layout.didOverflowHeight || ellipsized)
                    val bounds = node.boundsInRoot
                    assertTrue(
                        "text outside the frame $label",
                        bounds.top >= frame.top && bounds.bottom <= frame.bottom + 1,
                    )
                }
        }
    }

    private companion object {
        /** Largeur d'une page de tutoriel sur un téléphone de 360 dp, marges déduites. */
        val PHONE_CONTENT_WIDTH = 312.dp

        const val FRAME = "illustration"
    }
}
