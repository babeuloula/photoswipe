// Module application : point d'entrée Android, UI et assemblage DI (SPEC §5.4).
plugins {
    alias(libs.plugins.photoswipe.android.application)
    alias(libs.plugins.photoswipe.android.hilt)
}

android {
    namespace = "fr.babeuloula.photoswipe"

    defaultConfig {
        applicationId = "fr.babeuloula.photoswipe"
        versionCode = 1
        versionName = "0.1.0"
    }

    testOptions {
        // Robolectric a besoin des ressources et du manifeste fusionnés.
        unitTests.isIncludeAndroidResources = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}

dependencies {
    implementation(project(":domain"))
    // :data n'est utilisé que pour le câblage DI (règle vérifiée par le test d'architecture, PS-7).
    implementation(project(":data"))

    implementation(libs.androidx.activity)

    testImplementation(libs.junit4)
    // Tests Hilt sur la JVM : Robolectric fournit le framework Android sans émulateur.
    testImplementation(libs.robolectric)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
}
