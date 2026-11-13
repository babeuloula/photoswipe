package fr.babeuloula.photoswipe.ui.deck

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Vrai quand « Secouer pour annuler » est activé dans les réglages (PS-175), fourni par
 * `MainActivity` ; faux ailleurs, comme le réglage par défaut.
 */
val LocalShakeToUndo = compositionLocalOf { false }

/** Vrai si le téléphone a un accéléromètre : sans lui, « Secouer pour annuler » n'est pas proposé. */
fun hasAccelerometer(context: Context): Boolean =
    context.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

/**
 * Écoute l'accéléromètre tant que [enabled] est vrai, que l'écran qui l'appelle est composé et que
 * l'application est au premier plan (entre `ON_RESUME` et `ON_PAUSE`), et appelle [onShake] à
 * chaque secousse reconnue par [ShakeDetector] (PS-175). Aucune permission n'est nécessaire.
 */
@Composable
internal fun ShakeEffect(
    enabled: Boolean,
    onShake: () -> Unit,
) {
    if (!enabled) return
    val context = LocalContext.current
    val currentOnShake by rememberUpdatedState(onShake)
    LifecycleResumeEffect(context) {
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val detector = ShakeDetector()
        val listener =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val (x, y, z) = event.values
                    if (detector.onAcceleration(x, y, z, event.timestamp)) currentOnShake()
                }

                override fun onAccuracyChanged(
                    sensor: Sensor?,
                    accuracy: Int,
                ) = Unit
            }
        // Cadence des jeux (une mesure toutes les 20 ms environ) : plusieurs mesures par aller-retour du
        // poignet, pour reconnaître la secousse ; le capteur n'est écouté que pile à l'écran.
        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        onPauseOrDispose { sensorManager?.unregisterListener(listener) }
    }
}
