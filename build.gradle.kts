// Les plugins sont déclarés ici sans être appliqués afin de partager un seul classpath
// entre les modules (et d'aligner le Kotlin intégré d'AGP 9 sur la version du catalogue).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}
