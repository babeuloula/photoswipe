package fr.babeuloula.photoswipe

import android.Manifest
import android.content.pm.ActivityInfo
import android.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.permission.MEDIA_PERMISSIONS
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Vérifie que `MainActivity` démarre comme point d'entrée Hilt et affiche l'interface Compose bord à bord. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class MainActivityTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `main activity stays in portrait`() {
        val activity = composeRule.activity
        val declared = activity.packageManager.getActivityInfo(activity.componentName, 0)

        assertEquals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, declared.screenOrientation)
    }

    @Test
    fun `main activity starts on the media access explanation`() {
        val title = composeRule.activity.getString(R.string.permission_rationale_title)

        composeRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `main activity shows the home screen once media access is granted`() {
        shadowOf(composeRule.activity.application).grantPermissions(*MEDIA_PERMISSIONS)

        resume()

        awaitText(composeRule.activity.getString(R.string.home_today))
    }

    @Test
    fun `main activity goes back to the explanation when media access is revoked`() {
        val application = shadowOf(composeRule.activity.application)
        application.grantPermissions(*MEDIA_PERMISSIONS)
        resume()
        application.denyPermissions(*MEDIA_PERMISSIONS)

        resume()

        composeRule
            .onNodeWithText(
                composeRule.activity.getString(R.string.permission_rationale_title),
            ).assertIsDisplayed()
    }

    @Test
    fun `main activity warns about limited access and keeps the home screen`() {
        shadowOf(composeRule.activity.application).grantPermissions(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)

        resume()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.limited_access_message)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.limited_access_manage)).assertIsDisplayed()
        awaitText(composeRule.activity.getString(R.string.home_today))
    }

    @Test
    fun `main activity shows no limited access banner with full access`() {
        shadowOf(composeRule.activity.application).grantPermissions(*MEDIA_PERMISSIONS)

        resume()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.limited_access_message)).assertDoesNotExist()
    }

    /** Attend [value] : l'accueil se calcule hors du thread principal (MediaStore, Room). */
    private fun awaitText(value: String) {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(value).assertIsDisplayed()
    }

    /** Simule un retour au premier plan, par exemple depuis les réglages système. */
    private fun resume() {
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.waitForIdle()
    }

    @Test
    @Suppress("DEPRECATION") // Seul moyen de lire la couleur des barres système sur l'API 34.
    fun `main activity draws behind transparent system bars`() {
        val window = composeRule.activity.window

        assertEquals(Color.TRANSPARENT, window.statusBarColor)
        assertEquals(Color.TRANSPARENT, window.navigationBarColor)
    }
}
