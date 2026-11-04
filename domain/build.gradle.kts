// Module domaine : Kotlin JVM pur, sans aucune dépendance Android (SPEC §5.1).
plugins {
    alias(libs.plugins.photoswipe.jvm.library)
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
