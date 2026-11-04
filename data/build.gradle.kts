// Module données : implémentations Android des ports du domaine (SPEC §5.3).
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "fr.babeuloula.photoswipe.data"
    compileSdk = 37

    defaultConfig {
        minSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))

    testImplementation(libs.junit4)
}
