plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // compileOnly : les plugins AGP et Kotlin sont fournis à l'exécution par le build principal.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)

    testImplementation(libs.android.gradlePlugin)
    testImplementation(libs.kotlin.gradlePlugin)
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
        register("jvmLibrary") {
            id = libs.plugins.photoswipe.jvm.library.get().pluginId
            implementationClass = "fr.babeuloula.photoswipe.buildlogic.JvmLibraryConventionPlugin"
        }
    }
}
