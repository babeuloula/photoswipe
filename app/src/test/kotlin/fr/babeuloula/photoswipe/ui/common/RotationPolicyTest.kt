package fr.babeuloula.photoswipe.ui.common

import android.content.pm.ActivityInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Règle d'orientation de l'activité (PS-126, ADR 0028). */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class RotationPolicyTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Écrans affichés qui autorisent la rotation. */
    private var rotatingScreens by mutableIntStateOf(0)

    private fun show() {
        composeRule.activity.setContent {
            RotationPolicy {
                repeat(rotatingScreens) { AllowRotation() }
            }
        }
        composeRule.waitForIdle()
    }

    private val orientation get() = composeRule.activity.requestedOrientation

    @Test
    fun `keeps the app in portrait without a rotating screen`() {
        show()

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, orientation)
    }

    @Test
    fun `follows the phone while a rotating screen is shown, then goes back to portrait`() {
        show()

        rotatingScreens = 2
        composeRule.waitForIdle()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER, orientation)

        rotatingScreens = 1
        composeRule.waitForIdle()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER, orientation)

        rotatingScreens = 0
        composeRule.waitForIdle()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, orientation)
    }

    @Test
    fun `goes back to portrait when a recreated screen does not reopen its full screen view`() {
        rotatingScreens = 1
        show()
        assertEquals(ActivityInfo.SCREEN_ORIENTATION_FULL_USER, orientation)

        // Nouvelle composition, comme après une recréation, sans l'écran qui autorisait la rotation.
        rotatingScreens = 0
        show()

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, orientation)
    }
}
