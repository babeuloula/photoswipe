package fr.babeuloula.photoswipe.buildlogic

/**
 * Valeurs de configuration partagées par tous les modules (SPEC §7).
 * Une seule source de vérité : les modules ne répètent jamais ces nombres.
 */
object ProjectConfig {
    /**
     * Android 10 : `MediaStore` et `RELATIVE_PATH`, sur lesquels repose la lecture de la galerie.
     * Les fonctions plus récentes (corbeille et favoris d'Android 11, langue par application
     * d'Android 13…) ne sont proposées que là où elles existent (epic 14, PS-181).
     */
    const val MIN_SDK = 29

    /** Dernière API stable (Android 17), maximum supporté par AGP 9.5. */
    const val COMPILE_SDK = 37

    /** Cible alignée sur la dernière API stable, exigence du Play Store. */
    const val TARGET_SDK = 37

    /** Version de JVM minimale requise par AGP 9.5. */
    const val JVM_VERSION = 17

    /** Runner des tests instrumentés (`src/androidTest`), fourni par `androidx.test:runner`. */
    const val TEST_INSTRUMENTATION_RUNNER = "androidx.test.runner.AndroidJUnitRunner"
}
