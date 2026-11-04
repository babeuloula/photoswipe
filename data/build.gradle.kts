// Module données : implémentations Android des ports du domaine (SPEC §5.3).
plugins {
    alias(libs.plugins.photoswipe.android.library)
    alias(libs.plugins.photoswipe.android.room)
    alias(libs.plugins.photoswipe.android.robolectric)
}

android {
    namespace = "fr.babeuloula.photoswipe.data"
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    // Builders, fakes et FixedClock du domaine (SPEC §10).
    testImplementation(testFixtures(project(":domain")))
}
