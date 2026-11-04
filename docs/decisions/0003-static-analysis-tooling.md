# 0003 — Outils de qualité : ktlint-gradle, detekt 1.23 et une convention `photoswipe.quality`

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-5

## Contexte
La SPEC (§0, §12) impose ktlint et detekt, bloquants en CI, et Android Lint avec `MissingTranslation` en erreur. Trois choix ne sont pas tranchés par la SPEC :
- comment brancher ktlint dans Gradle ;
- quelle version de detekt prendre : la dernière version stable (1.23.8, février 2025) est compilée avec Kotlin 2.0.21, alors que le projet est en Kotlin 2.4. La branche 2.0, qui suit Kotlin, n'existe qu'en alpha (`2.0.0-alpha.6`) ;
- quel style ktlint appliquer.

## Décision
- **ktlint** via le plugin Gradle `org.jlleitschuh.gradle.ktlint` 14.2.0, avec le moteur ktlint 1.8.0 (version fixée dans le catalogue). Style **`ktlint_official`** dans `.editorconfig` : c'est celui que suivait déjà le code (virgules finales, imports `java.*` en dernier). Les fonctions `@Composable` sont exemptées de la règle de nommage.
- **detekt 1.23.8**, la dernière version stable. Sa configuration Gradle `detekt` est forcée sur la version de Kotlin qu'il embarque (`getSupportedKotlinVersion()`), pour éviter que Gradle lui impose le Kotlin du projet. On passera à detekt 2 quand il sera stable.
- Une **convention `photoswipe.quality`** (build-logic) applique les deux outils et enregistre une tâche `quality` par module. Elle est appliquée d'office par les conventions `android.application`, `android.library` et `jvm.library`, ce qui évite qu'un module l'oublie. La configuration Android Lint (`abortOnError`, `MissingTranslation` en erreur) vit dans la configuration Android commune.
- Commande unique : `./gradlew quality` (ktlint, detekt et Android Lint sur tous les modules). `./gradlew ktlintFormat` corrige automatiquement la mise en forme.

## Alternatives envisagées
- **Spotless** pour ktlint : plus générique (XML, Markdown, etc.) mais plus lourd à configurer, et nous n'avons besoin que de Kotlin.
- **detekt 2.0 alpha** : compatible avec Kotlin 2.4, mais la règle du projet est de n'utiliser que des versions stables.
- **Style `android_studio`** : il aurait retiré les virgules finales et réordonné les imports de tout le code existant, sans gain.
- **Tâche racine `quality` qui dépend de chaque module** : elle demande de connaître les modules depuis le build racine (couplage, isolation de projet). Une tâche du même nom dans chaque module, que Gradle lance partout, est plus simple.

## Conséquences
- Toute violation de format ou d'analyse fait échouer `./gradlew quality` et la CI.
- Les règles detekt désactivées ou ajustées sont listées et justifiées dans `config/detekt/detekt.yml`.
- Avec Gradle 9.8, detekt 1.23 et ktlint-gradle déclenchent deux avertissements de dépréciation internes (`ReportingExtension.file`, `Configuration.setVisible`). Ils viennent des plugins, pas de notre code ; à surveiller avant de passer à Gradle 10.
- detekt 1.23 analyse le code avec le parseur de Kotlin 2.0 : une syntaxe propre à Kotlin 2.1+ pourrait ne pas être comprise. Si cela arrive, c'est un signal pour passer à detekt 2.
