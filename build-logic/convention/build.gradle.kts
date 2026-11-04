plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // compileOnly : les plugins AGP, Kotlin, Compose, KSP, Hilt, Room, ktlint et detekt sont fournis à l'exécution par le build principal.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)

    testImplementation(libs.android.gradlePlugin)
    testImplementation(libs.kotlin.gradlePlugin)
    testImplementation(libs.ksp.gradlePlugin)
    testImplementation(libs.hilt.gradlePlugin)
    testImplementation(libs.room.gradlePlugin)
    testImplementation(libs.compose.gradlePlugin)
    testImplementation(libs.ktlint.gradlePlugin)
    testImplementation(libs.detekt.gradlePlugin)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.photoswipe.android.application.get().pluginId
            implementationClass = "fr.babeuloula.photoswipe.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.photoswipe.android.library.get().pluginId
            implementationClass = "fr.babeuloula.photoswipe.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidHilt") {
            id = libs.plugins.photoswipe.android.hilt.get().pluginId
            implementationClass = "fr.babeuloula.photoswipe.buildlogic.AndroidHiltConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.photoswipe.android.compose.get().pluginId
            implementationClass = "fr.babeuloula.photoswipe.buildlogic.AndroidComposeConventionPlugin"
        }
        register("androidRoom") {
            id = libs.plugins.photoswipe.android.room.get().pluginId
            implementationClass = "fr.babeuloula.photoswipe.buildlogic.AndroidRoomConventionPlugin"
        }
        register("androidRobolectric") {
            id = libs.plugins.photoswipe.android.robolectric.get().pluginId
            implementationClass = "fr.babeuloula.photoswipe.buildlogic.AndroidRobolectricConventionPlugin"
        }
        register("quality") {
            id = libs.plugins.photoswipe.quality.get().pluginId
            implementationClass = "fr.babeuloula.photoswipe.buildlogic.QualityConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.photoswipe.jvm.library.get().pluginId
            implementationClass = "fr.babeuloula.photoswipe.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
