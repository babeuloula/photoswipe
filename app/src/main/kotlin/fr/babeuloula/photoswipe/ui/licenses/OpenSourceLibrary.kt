package fr.babeuloula.photoswipe.ui.licenses

/** Licence d'une bibliothèque embarquée, dont le texte complet est affiché par l'écran des licences. */
enum class OpenSourceLicense {
    /** Apache License, Version 2.0, celle de PhotoSwipe. */
    APACHE_2_0,

    /** SIL Open Font License, Version 1.1, celle des polices embarquées. */
    OFL_1_1,
}

/**
 * Bibliothèque ou police embarquée dans l'application, citée dans l'écran des licences.
 *
 * @property name nom affiché.
 * @property author auteur ou éditeur, avec le nom de police réservé s'il y en a un.
 * @property license licence sous laquelle elle est distribuée.
 */
data class OpenSourceLibrary(
    val name: String,
    val author: String,
    val license: OpenSourceLicense = OpenSourceLicense.APACHE_2_0,
)

/**
 * Bibliothèques et polices embarquées dans l'APK (hors outils de build et de test) : sous licence
 * Apache 2.0, comme PhotoSwipe, sauf les polices, sous SIL Open Font License 1.1. Toute nouvelle
 * dépendance d'exécution du catalogue (`gradle/libs.versions.toml`) ou police de `res/font`
 * s'ajoute ici, avec sa licence.
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
        OpenSourceLibrary(
            "Atkinson Hyperlegible Next",
            "The Atkinson Hyperlegible Next Project Authors",
            OpenSourceLicense.OFL_1_1,
        ),
        OpenSourceLibrary("IBM Plex Mono", "IBM Corp. (Reserved Font Name \"Plex\")", OpenSourceLicense.OFL_1_1),
    )
