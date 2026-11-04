package fr.babeuloula.photoswipe.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * Configuration commune aux modules Android (application et bibliothèque).
 * Le Kotlin intégré d'AGP 9 est utilisé : aucun plugin `kotlin-android` n'est appliqué.
 * La version Java (`compileOptions`) n'est pas fixée ici : AGP la déduit du toolchain JVM.
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.compileSdk = ProjectConfig.COMPILE_SDK
    commonExtension.defaultConfig.minSdk = ProjectConfig.MIN_SDK
    configureKotlinToolchain()
}

/** Aligne Kotlin et Java sur la JVM du projet via le toolchain Gradle. */
internal fun Project.configureKotlinToolchain() {
    extensions.getByType(KotlinProjectExtension::class.java).jvmToolchain(ProjectConfig.JVM_VERSION)
}
