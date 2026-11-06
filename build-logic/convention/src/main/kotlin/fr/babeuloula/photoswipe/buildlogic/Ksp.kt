package fr.babeuloula.photoswipe.buildlogic

import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Applique KSP au module, fait passer ses tâches une à une dans tout le build et les sort du
 * cache de build (ADR 0021).
 *
 * Deux tâches KSP lancées en même temps dans le même démon (par exemple `kspDebugKotlin` et
 * `kspReleaseKotlin` de `:app`) se gênent : l'une finit parfois sans aucun fichier généré. Sans
 * les classes `Hilt_*`, javac n'a plus rien à compiler (`NO-SOURCE`) et la transformation Hilt
 * échoue (`transformReleaseClassesWithAsm`). Un service partagé limité à un usage à la fois les
 * fait donc passer l'une après l'autre. Pour la même raison, un résultat KSP mis en cache n'est
 * pas sûr, et KSP incrémental est désactivé dans `gradle.properties` (`ksp.incremental=false`).
 */
internal fun Project.applyKsp() {
    pluginManager.apply("com.google.devtools.ksp")
    val oneAtATime =
        gradle.sharedServices.registerIfAbsent(KSP_SERVICE, KspOneAtATime::class.java) {
            maxParallelUsages.set(1)
        }
    tasks.matching { isKspTask(it.name) }.configureEach {
        usesService(oneAtATime)
        outputs.doNotCacheIf(KSP_NOT_CACHED_REASON) { true }
    }
}

/** Vrai pour une tâche de traitement KSP d'une variante (`kspDebugKotlin`, `kspReleaseUnitTestKotlin`…). */
internal fun isKspTask(name: String): Boolean = name.startsWith("ksp") && name.endsWith("Kotlin")

/** Service sans état : il ne sert qu'à limiter les tâches KSP à une à la fois dans tout le build. */
abstract class KspOneAtATime : BuildService<BuildServiceParameters.None>

/** Nom du service partagé qui fait passer les tâches KSP une à une. */
internal const val KSP_SERVICE: String = "kspOneAtATime"

private const val KSP_NOT_CACHED_REASON =
    "KSP restored from the build cache can lose its generated Java sources (Hilt_* classes)"
