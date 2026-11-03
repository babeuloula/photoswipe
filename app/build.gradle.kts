// Module application : point d'entrée Android, UI et assemblage DI (SPEC §5.4).
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "fr.babeuloula.photoswipe"
    compileSdk = 37

    defaultConfig {
        applicationId = "fr.babeuloula.photoswipe"
        minSdk = 33
        targetSdk = 37
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
    // :data n'est utilisé que pour le câblage DI (règle vérifiée par le test d'architecture, PS-7).
    implementation(project(":data"))

    testImplementation(libs.junit4)
}
