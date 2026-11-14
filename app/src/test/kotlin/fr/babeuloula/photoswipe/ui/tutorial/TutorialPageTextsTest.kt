package fr.babeuloula.photoswipe.ui.tutorial

import android.os.Build
import fr.babeuloula.photoswipe.R
import fr.babeuloula.photoswipe.platform.PlatformFeatures
import org.junit.Assert.assertEquals
import org.junit.Test

/** Textes des pages du tutoriel selon les fonctions du téléphone (PS-183). */
class TutorialPageTextsTest {
    private val android10 = PlatformFeatures.forSdk(Build.VERSION_CODES.Q)
    private val android14 = PlatformFeatures.forSdk(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

    @Test
    fun `keeps every page as written where the gallery has favorites`() {
        TutorialPage.entries.forEach { page ->
            assertEquals(TutorialTexts(page.title, page.body, page.illustration), page.texts(android14))
        }
    }

    @Test
    fun `explains the swipe up as more actions on android 10`() {
        assertEquals(
            TutorialTexts(
                R.string.tutorial_more_actions_title,
                R.string.tutorial_more_actions_body,
                R.string.tutorial_more_actions_illustration,
            ),
            TutorialPage.SWIPE_UP.texts(android10),
        )
    }
}
