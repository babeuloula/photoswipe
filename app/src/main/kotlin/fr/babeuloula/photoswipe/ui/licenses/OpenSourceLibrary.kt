package fr.babeuloula.photoswipe.ui.licenses

/**
 * Bibliothèque embarquée dans l'application, citée dans l'écran des licences.
 *
 * @property name nom affiché.
 * @property author auteur ou éditeur.
 */
data class OpenSourceLibrary(
    val name: String,
    val author: String,
)

/**
 * Bibliothèques embarquées dans l'APK (hors outils de build et de test), toutes sous licence
 * Apache 2.0, comme PhotoSwipe. Toute nouvelle dépendance d'exécution du catalogue
 * (`gradle/libs.versions.toml`) s'ajoute ici, avec sa licence si elle diffère.
 */
val OPEN_SOURCE_LIBRARIES: List<OpenSourceLibrary> =
    listOf(
        OpenSourceLibrary("AndroidX Activity", "The Android Open Source Project"),
        OpenSourceLibrary("AndroidX Lifecycle", "The Android Open Source Project"),
        OpenSourceLibrary("AndroidX Navigation", "The Android Open Source Project"),
        OpenSourceLibrary("AndroidX Hilt", "The Android Open Source Project"),
        OpenSourceLibrary("AndroidX Room", "The Android Open Source Project"),
        OpenSourceLibrary("AndroidX DataStore", "The Android Open Source Project"),
        OpenSourceLibrary("AndroidX WorkManager", "The Android Open Source Project"),
        OpenSourceLibrary("AndroidX Media3", "The Android Open Source Project"),
        OpenSourceLibrary("Jetpack Compose (UI, Material 3, Material Icons)", "The Android Open Source Project"),
        OpenSourceLibrary("Dagger Hilt", "Google LLC"),
        OpenSourceLibrary("Coil", "Coil Contributors"),
        OpenSourceLibrary("Kotlin Standard Library", "JetBrains s.r.o. and Kotlin Programming Language contributors"),
        OpenSourceLibrary("kotlinx.coroutines", "JetBrains s.r.o."),
        OpenSourceLibrary("kotlinx.serialization", "JetBrains s.r.o."),
    )
