package fr.babeuloula.photoswipe.ui.deck

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import coil3.ImageLoader
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import fr.babeuloula.photoswipe.MainActivity
import fr.babeuloula.photoswipe.domain.fixtures.FakeMediaSource
import fr.babeuloula.photoswipe.domain.model.MediaId
import fr.babeuloula.photoswipe.ui.common.LocalImageLoader
import fr.babeuloula.photoswipe.ui.theme.PhotoSwipeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.SensorBuilder
import org.robolectric.shadows.SensorEventBuilder

/** « Secouer pour annuler » (PS-175) : la vraie pile, reliée à son ViewModel, et un accéléromètre simulé. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class DeckShakeTest : DeckViewModelTestBase() {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val sensorManager: SensorManager
        get() = composeRule.activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val accelerometer: Sensor = SensorBuilder.newBuilder().setType(Sensor.TYPE_ACCELEROMETER).build()

    /** Instant de la prochaine mesure simulée, en nanosecondes. */
    private var clockNanos = 0L

    /** Retours haptiques demandés pendant le test. */
    private val haptics = mutableListOf<HapticFeedbackType>()

    private val recordingHaptics =
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                haptics += hapticFeedbackType
            }
        }

    @Before
    fun addAccelerometer() {
        shadowOf(sensorManager).addSensor(accelerometer)
    }

    private fun show(
        viewModel: DeckViewModel,
        shakeToUndo: Boolean,
    ) {
        composeRule.activity.setContent {
            CompositionLocalProvider(
                LocalImageLoader provides ImageLoader(composeRule.activity),
                LocalHapticFeedback provides recordingHaptics,
                LocalShakeToUndo provides shakeToUndo,
            ) {
                PhotoSwipeTheme {
                    DeckRoute(onBack = {}, onOpenSummary = {}, viewModel = viewModel)
                }
            }
        }
        composeRule.waitForIdle()
    }

    /** Secousse franche : trois mesures à 3 g, à 20 ms d'écart ; puis on laisse passer le délai. */
    private fun shake() {
        composeRule.runOnUiThread {
            repeat(3) {
                val event =
                    SensorEventBuilder
                        .newBuilder(accelerometer, floatArrayOf(0f, 3f * SensorManager.STANDARD_GRAVITY, 0f))
                        .setTimestamp(clockNanos)
                        .build()
                shadowOf(sensorManager).sendSensorEventToListeners(event)
                clockNanos += 20_000_000L
            }
        }
        clockNanos += 2_000_000_000L
        composeRule.waitForIdle()
    }

    private fun DeckViewModel.topCard(): MediaId? = (uiState.value as? DeckUiState.Loaded)?.current?.id

    @Test
    fun `undoes the last decision with a shake, with a vibration, one decision per shake`() {
        val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2025), takenOn(2, 2024), takenOn(3, 2023))))
        show(viewModel, shakeToUndo = true)
        composeRule.runOnUiThread {
            viewModel.onEvent(DeckEvent.Keep)
            viewModel.onEvent(DeckEvent.MarkForDeletion)
        }
        composeRule.waitForIdle()
        assertEquals(MediaId(3), viewModel.topCard())

        shake()
        assertEquals(MediaId(2), viewModel.topCard())
        assertEquals(listOf(HapticFeedbackType.LongPress), haptics)

        shake()
        assertEquals(MediaId(1), viewModel.topCard())
        assertEquals(2, haptics.size)
    }

    @Test
    fun `does nothing on a shake when there is nothing to undo`() {
        val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2025), takenOn(2, 2024))))
        show(viewModel, shakeToUndo = true)

        shake()

        assertEquals(MediaId(1), viewModel.topCard())
        assertTrue(haptics.isEmpty())
    }

    @Test
    fun `does not listen to the accelerometer when shake to undo is off`() {
        val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2025), takenOn(2, 2024))))
        show(viewModel, shakeToUndo = false)
        composeRule.runOnUiThread { viewModel.onEvent(DeckEvent.Keep) }
        composeRule.waitForIdle()

        assertTrue(shadowOf(sensorManager).listeners.isEmpty())
        shake()
        assertEquals(MediaId(2), viewModel.topCard())
        assertTrue(haptics.isEmpty())
    }

    @Test
    fun `stops listening to the accelerometer once the app leaves the foreground`() {
        val viewModel = deckViewModel(FakeMediaSource(listOf(takenOn(1, 2025), takenOn(2, 2024))))
        show(viewModel, shakeToUndo = true)
        assertFalse(shadowOf(sensorManager).listeners.isEmpty())

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)

        assertTrue(shadowOf(sensorManager).listeners.isEmpty())
    }
}
