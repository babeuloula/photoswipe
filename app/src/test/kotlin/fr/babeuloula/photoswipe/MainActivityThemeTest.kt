package fr.babeuloula.photoswipe

import android.graphics.Color
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.core.view.WindowCompat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.domain.model.ThemeMode
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * Thème choisi dans les réglages appliqué par `MainActivity`, tout de suite et jusqu'aux barres
 * système : leurs icônes sont sombres sur un thème clair, claires sur un thème sombre, quel que
 * soit le thème du téléphone (clair sous Robolectric, sauf qualificateur `night`).
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class MainActivityThemeTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var settings: SettingsRepository

    @Before
    fun inject() {
        hiltRule.inject()
    }

    private fun lightSystemBarIcons(): Boolean {
        composeRule.waitForIdle()
        val activity = composeRule.activity
        return !WindowCompat.getInsetsController(activity.window, activity.window.decorView).isAppearanceLightStatusBars
    }

    private fun choose(mode: ThemeMode) {
        runBlocking { settings.saveThemeMode(mode) }
        composeRule.waitUntil { lightSystemBarIcons() == (mode == ThemeMode.DARK) }
    }

    @Test
    fun `follows the light phone theme by default`() {
        assertFalse(lightSystemBarIcons())
    }

    @Test
    fun `switches to the dark theme as soon as it is chosen, system bars included`() {
        choose(ThemeMode.DARK)

        assertTrue(lightSystemBarIcons())
    }

    @Test
    fun `blackens the navigation bar as soon as pure black is chosen, in the dark theme only`() {
        runBlocking { settings.savePureBlack(true) }
        composeRule.waitForIdle()
        assertNotEquals(Color.BLACK, composeRule.activity.window.navigationBarColor)

        choose(ThemeMode.DARK)
        composeRule.waitUntil { composeRule.activity.window.navigationBarColor == Color.BLACK }

        runBlocking { settings.savePureBlack(false) }
        composeRule.waitUntil { composeRule.activity.window.navigationBarColor != Color.BLACK }
        assertTrue(lightSystemBarIcons())
    }

    @Test
    @Config(qualifiers = "night")
    fun `keeps the light theme chosen on a dark phone`() {
        assertTrue(lightSystemBarIcons())

        choose(ThemeMode.LIGHT)

        assertFalse(lightSystemBarIcons())
    }
}
