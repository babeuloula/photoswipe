// Module domaine : Kotlin JVM pur, sans aucune dépendance Android (SPEC §5.1).
plugins {
    alias(libs.plugins.photoswipe.jvm.library)
    // Fixtures en mémoire (src/testFixtures) partagées avec les tests de :data et :app (SPEC §10).
    // Propre à ce module : aucun autre ne publie de fixtures, d'où l'absence de convention.
    `java-test-fixtures`
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    // Appel des fonctions suspend des ports et des fakes depuis les tests (runTest).
    testImplementation(libs.kotlinx.coroutines.test)
}
