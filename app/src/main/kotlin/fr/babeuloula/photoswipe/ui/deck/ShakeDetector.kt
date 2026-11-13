package fr.babeuloula.photoswipe.ui.deck

import kotlin.math.sqrt

/**
 * Détecteur de secousse (PS-175), en Kotlin pur pour être testé sans Android : il reçoit les
 * mesures de l'accéléromètre et dit laquelle termine une secousse.
 *
 * Une secousse, c'est au moins [minPeaks] mesures au-delà de [thresholdG] fois la gravité en moins
 * de [windowNanos] : un seul choc (téléphone posé un peu fort, nid-de-poule) ne suffit pas. Après
 * une secousse, plus rien pendant [cooldownNanos] : on secoue souvent plusieurs fois de suite, et
 * chaque secousse ne doit annuler qu'une décision.
 *
 * @param thresholdG accélération minimale d'une mesure, en multiples de la gravité terrestre.
 * @param minPeaks mesures au-delà du seuil nécessaires dans la fenêtre.
 * @param windowNanos fenêtre de temps où ces mesures doivent tomber, en nanosecondes.
 * @param cooldownNanos délai avant la secousse suivante, en nanosecondes.
 */
class ShakeDetector(
    private val thresholdG: Float = SHAKE_THRESHOLD_G,
    private val minPeaks: Int = SHAKE_MIN_PEAKS,
    private val windowNanos: Long = SHAKE_WINDOW_NANOS,
    private val cooldownNanos: Long = SHAKE_COOLDOWN_NANOS,
) {
    /** Instants des mesures au-delà du seuil, dans la fenêtre en cours. */
    private val peaks = ArrayDeque<Long>()

    /** Instant de la dernière secousse, `null` avant la première. */
    private var lastShakeAt: Long? = null

    /**
     * Prend une mesure de l'accéléromètre ([x], [y], [z] en m/s², gravité comprise, comme Android les
     * donne) prise à [timestampNanos] ; renvoie vrai si elle termine une secousse.
     */
    fun onAcceleration(
        x: Float,
        y: Float,
        z: Float,
        timestampNanos: Long,
    ): Boolean {
        val last = lastShakeAt
        val resting = last != null && timestampNanos - last < cooldownNanos
        val gForce = sqrt(x * x + y * y + z * z) / STANDARD_GRAVITY
        if (resting || gForce < thresholdG) return false
        peaks.addLast(timestampNanos)
        while (timestampNanos - peaks.first() > windowNanos) peaks.removeFirst()
        val shaken = peaks.size >= minPeaks
        if (shaken) {
            peaks.clear()
            lastShakeAt = timestampNanos
        }
        return shaken
    }

    companion object {
        /** Gravité terrestre standard, en m/s² (`SensorManager.STANDARD_GRAVITY`). */
        const val STANDARD_GRAVITY = 9.80665f

        /**
         * Seuil d'une mesure : 2,7 g. Au repos, l'accéléromètre lit 1 g (la gravité) ; marcher, monter
         * dans un bus qui freine ou poser le téléphone restent sous 2 g la plupart du temps, alors
         * qu'un geste franc du poignet dépasse facilement 2,5 à 3 g. 2,7 g est la valeur courante des
         * détecteurs de secousse sur Android ; elle sera ajustée sur un vrai téléphone.
         */
        const val SHAKE_THRESHOLD_G = 2.7f

        /** Trois mesures au-delà du seuil : un aller-retour du poignet, pas un choc isolé. */
        const val SHAKE_MIN_PEAKS = 3

        /** Fenêtre d'une secousse : un demi-seconde, la durée d'un ou deux allers-retours. */
        const val SHAKE_WINDOW_NANOS = 500_000_000L

        /** Délai avant la secousse suivante : une seconde et demie, une annulation par secousse. */
        const val SHAKE_COOLDOWN_NANOS = 1_500_000_000L
    }
}
