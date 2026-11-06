package fr.babeuloula.photoswipe.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.SpringSpec
import androidx.compose.material3.MaterialTheme
import fr.babeuloula.photoswipe.ui.navigation.SharedAxisTransitions
import fr.babeuloula.photoswipe.ui.navigation.rememberSharedAxisTransitions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

/** Mouvement du thème : ressorts Expressive de Material 3, fournis à tous les écrans (SPEC §8). */
@RunWith(RobolectricTestRunner::class)
class MotionTest {
    private val motion = PhotoSwipeMotion()

    private fun spring(spec: Any): SpringSpec<*> = spec as SpringSpec<*>

    @Test
    fun `uses the expressive spatial springs, with a slight bounce`() {
        assertEquals(0.8f, spring(motion.defaultSpatial<Float>()).dampingRatio)
        assertEquals(380f, spring(motion.defaultSpatial<Float>()).stiffness)
        assertEquals(0.6f, spring(motion.fastSpatial<Float>()).dampingRatio)
        assertEquals(800f, spring(motion.fastSpatial<Float>()).stiffness)
        assertEquals(200f, spring(motion.slowSpatial<Float>()).stiffness)
    }

    @Test
    fun `exits the card fast and without bounce, and raises the next one with the standard spring`() {
        assertEquals(1f, spring(motion.fastExit<Float>()).dampingRatio)
        assertEquals(1400f, spring(motion.fastExit<Float>()).stiffness)
        assertEquals(0.9f, spring(motion.fastSpatialStandard<Float>()).dampingRatio)
        assertEquals(1400f, spring(motion.fastSpatialStandard<Float>()).stiffness)
    }

    @Test
    fun `uses effect springs without bounce, faster than the spatial ones`() {
        listOf(motion.fastEffects<Float>(), motion.defaultEffects(), motion.slowEffects()).forEach {
            assertEquals(1f, spring(it).dampingRatio)
        }
        assertTrue(spring(motion.defaultEffects<Float>()).stiffness > spring(motion.defaultSpatial<Float>()).stiffness)
    }

    @Test
    fun `provides the motion and the screen transitions through the theme`() {
        var provided: PhotoSwipeMotion? = null
        var transitions: SharedAxisTransitions? = null
        Robolectric.buildActivity(ComponentActivity::class.java).setup().get().setContent {
            PhotoSwipeTheme {
                provided = MaterialTheme.motion
                transitions = rememberSharedAxisTransitions()
            }
        }
        ShadowLooper.idleMainLooper()

        assertNotNull(provided)
        assertNotNull(transitions!!.enter)
        assertNotNull(transitions!!.popExit)
    }
}
