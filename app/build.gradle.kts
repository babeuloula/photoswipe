// Module application : point d'entrée Android, UI et assemblage DI (SPEC §5.4).
plugins {
    alias(libs.plugins.photoswipe.android.application)
    alias(libs.plugins.photoswipe.android.hilt)
    alias(libs.plugins.photoswipe.android.compose)
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

// Robolectric hors ligne : son jar Android est résolu par Gradle (donc mis en cache, en local comme en CI)
// au lieu d'être téléchargé par Robolectric à chaque exécution des tests (Maven Central renvoie des 429 en CI).
val robolectricRuntime: Configuration by configurations.creating { isTransitive = false }
val robolectricJarsDir = layout.buildDirectory.dir("robolectric-jars")
val syncRobolectricJars =
    tasks.register<Sync>("syncRobolectricJars") {
        from(robolectricRuntime)
        into(robolectricJarsDir)
    }

tasks.withType<Test>().configureEach {
    inputs.files(syncRobolectricJars).withPropertyName("robolectricJars").withPathSensitivity(PathSensitivity.NAME_ONLY)
    systemProperty("robolectric.offline", "true")
    systemProperty("robolectric.dependency.dir", robolectricJarsDir.get().asFile.absolutePath)
}

dependencies {
    implementation(project(":domain"))
    // :data n'est utilisé que pour le câblage DI (règle vérifiée par le test d'architecture, PS-7).
    implementation(project(":data"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit4)
    // Tests Hilt sur la JVM : Robolectric fournit le framework Android sans émulateur.
    testImplementation(libs.robolectric)
    robolectricRuntime(libs.robolectric.androidAll)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.androidx.compose.ui.test.junit4)
}
