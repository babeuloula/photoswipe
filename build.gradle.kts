// Les plugins sont déclarés ici sans être appliqués afin de partager un seul classpath
// entre les modules (et d'aligner le Kotlin intégré d'AGP 9 sur la version du catalogue).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

// `./gradlew test` lance aussi les tests des plugins de convention (build inclus build-logic).
tasks.register("test") {
    group = "verification"
    description = "Runs the build-logic convention plugin tests."
    dependsOn(gradle.includedBuild("build-logic").task(":convention:test"))
}
