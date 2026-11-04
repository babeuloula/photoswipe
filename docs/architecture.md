# Architecture

> Document vivant : il décrit l'état **actuel** du dépôt et ce qui est prévu. Les éléments pas encore implémentés sont marqués *(prévu)*.

## 1. Vue d'ensemble

PhotoSwipe suit une architecture en couches de type **Clean Architecture**. Le cœur métier (`:domain`) ne connaît aucune technologie. Les modules externes (`:data` pour l'accès aux données, `:app` pour l'interface) dépendent de lui, jamais l'inverse. C'est le principe d'**inversion de dépendances** : le domaine définit des interfaces (les « ports ») et `:data` les implémente.

```
                ┌──────────────────────────┐
                │          :app            │  UI Compose, navigation, ViewModels,
                │  (application Android)   │  assemblage Hilt, notifications
                └──────┬────────────┬──────┘
                       │            │ (uniquement pour le câblage DI)
                       ▼            ▼
┌──────────────────────────┐   ┌──────────────────────────┐
│         :domain          │◄──│          :data           │  MediaStore, Room,
│   Kotlin JVM pur :       │   │  (bibliothèque Android)  │  DataStore, WorkManager
│   modèles, use cases,    │   └──────────────────────────┘
│   ports (interfaces)     │
└──────────────────────────┘
```

Règles :
- `:domain` ne dépend d'**aucun** autre module ni d'aucune bibliothèque Android.
- `:data` dépend de `:domain` pour implémenter ses ports.
- `:app` dépend de `:domain` (use cases, modèles). Il ne dépend de `:data` que pour que Hilt puisse brancher les implémentations : le code de l'UI n'importe jamais de classe de `:data` en dehors du package `di`.
- Le test d'architecture (Konsist, PS-7) vérifiera ces règles *(prévu)*. En attendant, un test de `:domain` vérifie que le framework Android n'est pas sur son classpath.

## 2. Modules

| Module | Plugin Gradle | Package | Rôle |
|---|---|---|---|
| `:app` | `com.android.application` | `fr.babeuloula.photoswipe` | Point d'entrée (`MainActivity`), UI Compose *(prévu)*, ViewModels *(prévu)*, assemblage Hilt *(prévu)* |
| `:data` | `com.android.library` | `fr.babeuloula.photoswipe.data` | Implémentations Android des ports : `MediaStoreMediaSource`, `RoomReviewRepository`, etc. *(prévu)* |
| `:domain` | `org.jetbrains.kotlin.jvm` | `fr.babeuloula.photoswipe.domain` | Modèles, services purs (`SameDayMatcher`, etc.), use cases, ports *(prévu)* |

## 3. Flux de données *(prévu, SPEC §5.4)*

L'UI suit **MVVM** avec un **flux unidirectionnel de données (UDF)** :

```
Écran Compose ──UiEvent──► ViewModel ──appel──► Use case (:domain) ──port──► Implémentation (:data)
      ▲                        │
      └────── StateFlow<UiState> + UiEffect (navigation, dialogue système)
```

- Chaque écran a un `UiState` immuable, des `UiEvent` entrants et des `UiEffect` sortants.
- Un ViewModel ne connaît que des use cases.
- Le temps (`Clock`) et la concurrence (`CoroutineDispatcher`) sont injectés, jamais appelés en statique, pour rester testables.

## 4. Configuration Android

| Paramètre | Valeur | Raison |
|---|---|---|
| `applicationId` | `fr.babeuloula.photoswipe` | SPEC §7 |
| `minSdk` | 33 (Android 13) | SPEC §2 : permissions médias granulaires, choix de langue par application |
| `compileSdk` / `targetSdk` | 37 (Android 17) | Dernière version stable, et le Play Store impose une cible récente |
| JVM cible | 17 | Minimum requis par AGP 9.5 |
| Permissions | aucune pour l'instant | `INTERNET` est interdite (SPEC §0.6). Les permissions médias et notifications viendront avec leurs epics |

## 5. Versions

Relevées le 2026-11-03 sur les sources officielles (Google Maven, Maven Central, services.gradle.org, releases GitHub). Elles sont centralisées dans `gradle/libs.versions.toml`.

| Outil | Version | Note |
|---|---|---|
| Android Gradle Plugin (AGP) | 9.5.0 | Kotlin intégré à AGP 9 : les modules Android n'appliquent pas `kotlin-android` |
| Gradle | 9.8.2 | AGP 9.5 demande au moins Gradle 9.6.0 |
| Kotlin | 2.4.21 | Déclaré à la racine pour aligner le Kotlin intégré d'AGP |
| JDK | 17 | |
| JUnit Jupiter (BOM) | 6.1.3 | Tests de `:domain` |
| JUnit 4 | 4.13.2 | Tests unitaires des modules Android |
| GitHub Actions | `checkout@v7`, `setup-java@v6`, `gradle/actions/setup-gradle@v6`, `upload-artifact@v7` | CI |

## 6. Bibliothèques et justification

SPEC §0.4 : aucune bibliothèque sans justification écrite ici.

| Bibliothèque | Module | Justification |
|---|---|---|
| JUnit Jupiter 6 | `:domain` (tests) | Standard actuel des tests JVM. La SPEC demande JUnit 5 ; JUnit 6 en est la version suivante, avec la même API |
| JUnit 4 | `:app`, `:data` (tests) | Fonctionne nativement sur Android sans plugin supplémentaire |

Prévues dans la SPEC, ajoutées avec leur ticket : Jetpack Compose + Material 3, Hilt (KSP), Navigation Compose, Room, DataStore, Coil 3, Media3 ExoPlayer, WorkManager, ktlint, detekt, Konsist, MockK, Turbine, kotlinx-coroutines-test.

## 7. CI

`.github/workflows/ci.yml` se lance à chaque push sur `main` et sur chaque PR. Il installe le JDK 17 (Temurin) et Gradle, valide le wrapper, puis exécute `test`, `lint` et `assembleDebug`. En cas d'échec, les rapports sont publiés en artefact. Chaque nouveau check du projet y est ajouté dans la PR qui l'introduit.
