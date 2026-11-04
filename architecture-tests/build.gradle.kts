// Tests d'architecture (Konsist) : vérifient le sens des dépendances entre modules (SPEC §5.1, §11).
// Module à part : Konsist lit les sources de tout le projet, aucun module métier n'a à le connaître.
plugins {
    alias(libs.plugins.photoswipe.jvm.library)
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    // Konsist lit les sources des autres modules sans passer par Gradle : on les déclare en entrées,
    // sinon Gradle jugerait ce test à jour (et le sauterait) après une modification de :app ou :domain.
    inputs
        .files(
            fileTree(rootDir) {
                include("app/src/**/*.kt", "data/src/**/*.kt", "domain/src/**/*.kt")
            },
        ).withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("scannedSources")
}
