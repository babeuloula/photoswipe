# 0027 — Le tutoriel en porte d'entrée, avant l'accès aux médias

- **Statut** : accepté
- **Date** : 2026-11-06
- **Ticket** : PS-74

## Contexte
La SPEC §4 veut un tutoriel affiché une seule fois, **avant** l'écran de permissions, qu'on peut passer, et qu'on peut revoir depuis les réglages. Le parcours d'accès aux médias n'est pas une destination de navigation : `MediaAccessGate` entoure le `NavHost` (ADR 0014).

## Décision
- **`TutorialGate` entoure `MediaAccessGate`** dans `PhotoSwipeApp` : tant que `ShouldShowTutorial` le demande, il affiche `TutorialScreen` ; « Passer » ou « C'est parti » appellent `MarkTutorialSeen` puis laissent place à l'accès aux médias.
- **Trois états** (`TutorialGateState`) : `CHECKING` (réglages en cours de lecture, écran vide pour ne pas montrer l'explication d'accès un instant), `SHOWING`, `DONE`. Le `TutorialGateViewModel` vit avec l'activité : une recréation garde l'étape, le carrousel garde sa page (`rememberPagerState`).
- **Une lecture des réglages qui échoue ne bloque pas l'application** : le tutoriel n'est pas montré.
- **`TutorialScreen` ne sait rien de l'état « vu »** : il reçoit `onDone`. Revoir le tutoriel depuis les réglages (PS-81) réutilise le même écran depuis une route, sans toucher à cet état.
- **Use cases fournis par `di/TutorialModule`**, seuls dans leur module : les tests qui démarrent `MainActivity` sans tutoriel le désinstallent et lient des use cases construits sur un `FakeSettingsRepository` déjà vu (`@UninstallModules`, `@BindValue`).
- **Indicateur de page dessiné par l'application** : Material 3 stable n'en fournit pas ; le carrousel est le `HorizontalPager` de Compose Foundation, sans bibliothèque de plus.

## Alternatives envisagées
- **Route `TutorialRoute` en destination de départ** : il faudrait choisir la destination de départ selon les réglages, et l'accès aux médias, qui entoure le graphe, passerait avant le tutoriel.
- **Lecture bloquante au démarrage (comme le thème)** : évite l'état `CHECKING`, mais ajoute une seconde lecture bloquante dans `onCreate` pour un écran vu une fois.

## Conséquences
- Le tutoriel passe aussi avant l'accueil quand sa version augmente (`Tutorial.CURRENT_VERSION`), même avec l'accès déjà accordé.
- Un nouveau test qui démarre `MainActivity` et attend l'explication d'accès ou l'accueil lie un tutoriel déjà vu.
