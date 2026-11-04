// Module application : point d'entrée Android, UI et assemblage DI (SPEC §5.4).
plugins {
    alias(libs.plugins.photoswipe.android.application)
    alias(libs.plugins.photoswipe.android.hilt)
    alias(libs.plugins.photoswipe.android.compose)
    alias(libs.plugins.photoswipe.android.robolectric)
    // Routes de navigation typées (@Serializable).
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "fr.babeuloula.photoswipe"

    defaultConfig {
        applicationId = "fr.babeuloula.photoswipe"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}

// Tests propres à la variante release (src/testRelease) : AGP 9 n'active les tests unitaires que pour
// debug. On les active pour release, en ne rejouant que les tests de src/testRelease : le reste de la
// suite tourne déjà en debug.
androidComponents {
    beforeVariants(selector().withBuildType("release")) {
        it.hostTests[com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
    }
}
tasks.matching { it.name == "testReleaseUnitTest" }.withType<Test>().configureEach {
    filter.includeTestsMatching("fr.babeuloula.photoswipe.di.MediaSourceBindingTest")
}

dependencies {
    implementation(project(":domain"))
    // :data n'est utilisé que pour le câblage DI (règle vérifiée par le test d'architecture, PS-7).
    implementation(project(":data"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)

    // Données de démo des outils développeur (SPEC §10, niveau 2) : jamais dans la variante release.
    debugImplementation(testFixtures(project(":domain")))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    // Builders, fakes et FixedClock du domaine (SPEC §10).
    testImplementation(testFixtures(project(":domain")))
    // Tests Hilt sur la JVM : Robolectric (convention photoswipe.android.robolectric) fournit le framework Android.
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
