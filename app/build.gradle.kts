// Module application : point d'entrée Android, UI et assemblage DI (SPEC §5.4).
plugins {
    alias(libs.plugins.photoswipe.android.application)
    alias(libs.plugins.photoswipe.android.hilt)
    alias(libs.plugins.photoswipe.android.compose)
    alias(libs.plugins.photoswipe.android.robolectric)
    // Routes de navigation typées (@Serializable).
    alias(libs.plugins.kotlin.serialization)
}

// Numéro de version : le nombre de commits, pour qu'un nouveau build s'installe par-dessus l'ancien
// (PS-110). 1 si Git est absent ou si l'historique est tronqué (clone superficiel de la CI).
val commitCount: Int =
    providers
        .exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            isIgnoreExitValue = true
        }.standardOutput.asText
        .get()
        .trim()
        .toIntOrNull() ?: 1

android {
    namespace = "fr.babeuloula.photoswipe"

    defaultConfig {
        applicationId = "fr.babeuloula.photoswipe"
        versionCode = commitCount
        versionName = "0.1.0"
    }

    // Clé de debug partagée, versionnée dans le dépôt privé (PS-110) : tous les builds debug, de
    // n'importe quelle machine, ont la même signature et se mettent à jour sans désinstaller l'app.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
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
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.coil.compose.core)
    implementation(libs.coil.video)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui.compose)

    // Données de démo des outils développeur (SPEC §10, niveau 2) : jamais dans la variante release.
    debugImplementation(testFixtures(project(":domain")))
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    // Fabrique Hilt des tâches WorkManager, branchée dans PhotoSwipeApplication (rappel quotidien).
    implementation(libs.work.runtime)
    implementation(libs.androidx.hilt.work)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    // Builders, fakes et FixedClock du domaine (SPEC §10).
    testImplementation(testFixtures(project(":domain")))
    // Tests Hilt sur la JVM : Robolectric (convention photoswipe.android.robolectric) fournit le framework Android.
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
