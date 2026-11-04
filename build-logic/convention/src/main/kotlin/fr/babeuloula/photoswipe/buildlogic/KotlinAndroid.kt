package fr.babeuloula.photoswipe.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * Configuration commune aux modules Android (application et bibliothèque).
 * Le Kotlin intégré d'AGP 9 est utilisé : aucun plugin `kotlin-android` n'est appliqué.
 * Elle applique aussi Android Lint bloquant et la convention qualité (ktlint, detekt), et lance
 * les tests instrumentés (`src/androidTest`) avec `AndroidJUnitRunner`.
 * La version Java (`compileOptions`) n'est pas fixée ici : AGP la déduit du toolchain JVM.
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.compileSdk = ProjectConfig.COMPILE_SDK
    commonExtension.defaultConfig.minSdk = ProjectConfig.MIN_SDK
    commonExtension.defaultConfig.testInstrumentationRunner = ProjectConfig.TEST_INSTRUMENTATION_RUNNER
    configureKotlinToolchain()
    configureAndroidLint(commonExtension)
    pluginManager.apply(QualityConventionPlugin::class.java)
}

/**
 * Android Lint bloquant (SPEC §0.2) : une traduction manquante (`MissingTranslation`)
 * est une erreur, et toute erreur fait échouer le build.
 */
private fun configureAndroidLint(commonExtension: CommonExtension) {
    commonExtension.lint.abortOnError = true
    commonExtension.lint.error += "MissingTranslation"
}

/** Aligne Kotlin et Java sur la JVM du projet via le toolchain Gradle. */
internal fun Project.configureKotlinToolchain() {
    extensions.getByType(KotlinProjectExtension::class.java).jvmToolchain(ProjectConfig.JVM_VERSION)
}
