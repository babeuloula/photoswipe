# CLAUDE.md — PhotoSwipe

> **À maintenir à jour.** Toute PR qui change une commande, une convention, la structure des modules ou le processus de travail met à jour ce fichier dans la même PR.

Application Android (Kotlin + Jetpack Compose) qui fait revoir chaque jour les photos et vidéos prises le même jour lors des années précédentes. Tri par swipe : gauche = marquer pour suppression, droite = garder, haut = menu favori / partage.

## Sources de vérité
- `SPEC.md` : la spécification complète. **Elle fait foi**, en particulier le §0 (règles non négociables) et le §16 (points ouverts, valeurs par défaut appliquées sauf avis contraire).
- `docs/architecture.md` : les modules, le sens des dépendances, les versions et la justification de chaque bibliothèque.
- `docs/beginners-guide.md` : le guide pour les néophytes (Kotlin, Gradle, Android, rôle de chaque dossier).
- `README.md` : présentation courte et liens vers la documentation.
- `docs/decisions/` : les ADR. Toute décision structurante absente de la spec y est écrite au lieu d'être improvisée en silence.

## Règles permanentes (résumé du SPEC §0)
- Le code est en anglais. Les commentaires et la KDoc sont en français.
- Aucune chaîne en dur dans l'UI : `values/strings.xml` en anglais, `values-fr/` en français. `MissingTranslation` est une erreur.
- `:domain` est du Kotlin JVM pur : ni Android, ni Room, ni Compose. Les dépendances vont toujours **vers** `:domain`.
- Principes SOLID et KISS, injection par constructeur (Hilt + KSP), aucun singleton statique.
- Aucune permission `INTERNET`. L'application est 100 % locale.
- Chaque ticket est livré avec ses tests. Rien n'est terminé sans tests verts, en local comme en CI.
- Aucune nouvelle bibliothèque sans une justification d'une ligne dans `docs/architecture.md`.
- Versions : vérifier les dernières versions stables sur les sources officielles (Google Maven, Maven Central, services.gradle.org), pas de mémoire.

## Commandes
Prérequis : JDK 17 et le SDK Android (`local.properties` contient `sdk.dir`, ce fichier n'est pas commité).

```bash
# En local sur la machine de Jérémy : le JBR d'Android Studio est en version 25, on force le JDK 17
export JAVA_HOME=/opt/homebrew/opt/openjdk@17

./gradlew test            # tests unitaires de tous les modules + tests des conventions build-logic
./gradlew lint            # Android Lint
./gradlew assembleDebug   # APK debug dans app/build/outputs/apk/debug/
./gradlew test lint assembleDebug   # ce que lance la CI
```

## Structure
- `:app` : UI, navigation, assemblage DI, point d'entrée Android (`fr.babeuloula.photoswipe`).
- `:data` : implémentations Android des ports du domaine : MediaStore, Room, DataStore, WorkManager (`fr.babeuloula.photoswipe.data`).
- `:domain` : modèles, use cases, ports, en Kotlin JVM pur (`fr.babeuloula.photoswipe.domain`).
- `build-logic/` (build inclus) : plugins de convention `photoswipe.android.application`, `photoswipe.android.library` et `photoswipe.jvm.library`. Les valeurs partagées (SDK, JVM) sont dans `ProjectConfig`. Un module applique sa convention et ne déclare que ce qui lui est propre (namespace, dépendances). Toute nouvelle configuration commune (Compose, Hilt, qualité) devient un plugin de convention (ADR 0001).
- Versions : `gradle/libs.versions.toml`, sans aucune version en dur ailleurs. CI : `.github/workflows/ci.yml`.

## Tests
- `:domain` : JUnit Jupiter (JUnit 6).
- Modules Android (`:app`, `:data`) : JUnit 4, en attendant une décision ADR sur JUnit 5/6 côté Android.
- Les noms des tests sont en anglais et décrivent le comportement, par exemple `` `moves leap day media to February 28 on non-leap years` ``.

## Processus de travail
1. On travaille un ticket (PS-N) à la fois, dans l'ordre de l'epic. Chaque ticket a sa branche `ps-N-description` et sa PR vers `main`.
2. Avant de commiter, `./gradlew test lint assembleDebug` doit passer.
3. Les commits suivent Conventional Commits et citent le ticket, par exemple `feat(core): add multi-module skeleton (PS-1)`.
4. Chaque PR est assignée à `babeuloula` et porte :
   - un label `epic: N-nom` ;
   - un ou plusieurs labels `area: build|ci|quality|app|data|domain|i18n|testing` ;
   - un label de type : `enhancement`, `bug`, `documentation` ou `accessibility`.

   Lancer `gh label list` avant de créer un nouveau label.
5. **La CI est maintenue au fil du projet.** Tout nouveau check (ktlint, detekt, Konsist, tests instrumentés, nouveau module, etc.) est ajouté à `.github/workflows/ci.yml` dans la même PR. Une PR n'est prête que si son run CI est vert.
6. **La documentation est maintenue au fil du projet.** On met à jour `CLAUDE.md`, `docs/architecture.md` et `docs/beginners-guide.md` dès qu'un module, un dossier, un outil ou une commande change.
7. **Démo sur émulateur** : si le ticket a un effet visible, on lance l'émulateur (`~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_37.0`), on installe l'app (`./gradlew installDebug`) et on donne à Jérémy la liste de ce qu'il faut tester et comment. Si le ticket n'a rien de visible, on le dit explicitement.
8. **Suivi Notion** : les tickets vivent dans le kanban Notion « Tickets de développement » (page PhotoSwipe). Le `Ticket ID` Notion (PS-N) fait foi pour la numérotation. Le statut avance au fil du travail :
   - « En cours » quand on commence le ticket ;
   - « En revue » à l'ouverture de la PR, avec les critères d'acceptation cochés et le lien de la PR ;
   - « Terminé » une fois la PR mergée.
9. Après avoir ouvert la PR, on s'arrête et on attend la revue avant le ticket suivant.
10. Ne rien faire qui dépasse le périmètre du ticket et de l'epic en cours.

## État d'avancement
- Epic 1 « Squelette & outillage » : PS-1 mergé (PR #1), PS-2 en revue (catalogue et conventions). Suivent PS-3 (Hilt), PS-4 (thème M3), PS-5 (ktlint, detekt, lint), PS-6 (compléments CI), PS-7 (Konsist), PS-8 (i18n) et PS-9 (`docs/testing.md` ; README et docs déjà livrés avec PS-1).
