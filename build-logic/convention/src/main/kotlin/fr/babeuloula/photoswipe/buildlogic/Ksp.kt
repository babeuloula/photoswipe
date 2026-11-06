package fr.babeuloula.photoswipe.buildlogic

import org.gradle.api.Project

/**
 * Applique KSP au module et sort ses tâches du cache de build.
 *
 * Restaurée depuis le cache de la CI, la tâche KSP de la variante release revenait parfois sans
 * les sources Java qu'elle génère (les classes `Hilt_*` de Hilt) : javac n'avait plus rien à
 * compiler (`NO-SOURCE`) et la transformation Hilt échouait (`transformReleaseClassesWithAsm`).
 * En local, KSP incrémental perdait de même des fichiers après un changement de branche : il est
 * désactivé dans `gradle.properties` (`ksp.incremental=false`). KSP refait donc tout son
 * traitement à chaque compilation qui le demande ; un résultat en cache n'apporte rien de sûr.
 */
internal fun Project.applyKsp() {
    pluginManager.apply("com.google.devtools.ksp")
    tasks.matching { isKspTask(it.name) }.configureEach {
        outputs.doNotCacheIf(KSP_NOT_CACHED_REASON) { true }
    }
}

/** Vrai pour une tâche de traitement KSP d'une variante (`kspDebugKotlin`, `kspReleaseUnitTestKotlin`…). */
internal fun isKspTask(name: String): Boolean = name.startsWith("ksp") && name.endsWith("Kotlin")

private const val KSP_NOT_CACHED_REASON =
    "KSP restored from the build cache can lose its generated Java sources (Hilt_* classes)"
