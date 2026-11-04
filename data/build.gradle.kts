// Module données : implémentations Android des ports du domaine (SPEC §5.3).
plugins {
    alias(libs.plugins.photoswipe.android.library)
}

android {
    namespace = "fr.babeuloula.photoswipe.data"
}

dependencies {
    implementation(project(":domain"))

    testImplementation(libs.junit4)
    // Builders, fakes et FixedClock du domaine (SPEC §10).
    testImplementation(testFixtures(project(":domain")))
}
