package fr.babeuloula.photoswipe

import android.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
    fun `main activity starts on the media access explanation`() {
        val title = composeRule.activity.getString(R.string.permission_rationale_title)

        composeRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun `main activity shows the home screen after continue`() {
        composeRule
            .onNodeWithText(
                composeRule.activity.getString(R.string.permission_rationale_continue),
            ).performClick()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.home_placeholder)).assertIsDisplayed()
    }

    @Test
    @Suppress("DEPRECATION") // Seul moyen de lire la couleur des barres système sur l'API 34.
    fun `main activity draws behind transparent system bars`() {
        val window = composeRule.activity.window

        assertEquals(Color.TRANSPARENT, window.statusBarColor)
        assertEquals(Color.TRANSPARENT, window.navigationBarColor)
    }
}
