// Script d'init de la CI (PS-132) : la compilation Kotlin ne vient jamais du build cache, pour que
// ses avertissements (dont les dépréciations, relevées par scripts/deprecation_report.py) soient
// affichés à chaque run. Une tâche restaurée depuis le cache ne réaffiche pas ses avertissements.
// Les autres tâches (tests, lint, KSP…) gardent le cache.
gradle.beforeProject {
    tasks.configureEach {
        if (name.startsWith("compile") && name.endsWith("Kotlin")) {
            outputs.doNotCacheIf("Kotlin warnings are reported by the CI (PS-132)") { true }
        }
    }
}
