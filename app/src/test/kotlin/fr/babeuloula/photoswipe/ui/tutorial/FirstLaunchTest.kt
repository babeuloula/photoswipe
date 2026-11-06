package fr.babeuloula.photoswipe.ui.tutorial

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.domain.model.Tutorial
import fr.babeuloula.photoswipe.domain.port.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject

/**
 * Premier lancement de `MainActivity`, sur les vrais réglages DataStore (vides pour chaque test) :
 * le tutoriel passe avant l'écran d'accès aux médias et, passé, il est retenu comme vu. Un tutoriel
 * déjà vu mène droit à l'explication (`MainActivityTest`).
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class FirstLaunchTest {
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

    private fun text(id: Int): String = composeRule.activity.getString(id)

    private fun awaitText(value: String) {
        composeRule.waitUntil(5_000) { composeRule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithText(value).assertIsDisplayed()
    }

    @Test
    fun `shows the tutorial before the media access explanation on the first launch`() {
        awaitText(text(TutorialPage.entries.first().title))

        composeRule.onNodeWithText(text(R.string.permission_rationale_title)).assertDoesNotExist()
    }

    @Test
    fun `skipping the tutorial leads to the media access explanation and remembers it`() {
        awaitText(text(R.string.tutorial_skip))

        composeRule.onNodeWithText(text(R.string.tutorial_skip)).performClick()

        awaitText(text(R.string.permission_rationale_title))
        composeRule.waitUntil(5_000) { runBlocking { settings.tutorialVersionSeen() } == Tutorial.CURRENT_VERSION }
    }
}
