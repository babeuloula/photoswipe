# PhotoSwipe — Spécification

> Jeu de mots *photo wipe* / *photo swipe*. Application Android de tri de photos et vidéos par swipe, façon Tinder, centrée sur les souvenirs « du même jour » à travers les années.

Ce document est destiné à Claude Code. Il peut être copié à la racine du dépôt (`SPEC.md`) et résumé dans `CLAUDE.md`.

---

## 0. Règles non négociables pour l'agent

1. **Code en anglais** (noms de classes, fonctions, variables, packages, noms de tests). **Commentaires et KDoc en français.**
2. **Aucune chaîne en dur dans l'UI** : tout passe par `strings.xml` (EN par défaut + FR). Le lint `MissingTranslation` est configuré en **erreur**.
3. **Dépendances inversées** : le domaine ne connaît ni Android, ni Room, ni Compose. Les couches externes dépendent du domaine, jamais l'inverse.
4. **KISS d'abord** : pas d'abstraction sans besoin réel et testable. Pas de bibliothèque ajoutée sans justification écrite dans `docs/architecture.md`.
5. **Chaque fonctionnalité arrive avec ses tests** (voir §11). Une PR/étape sans test n'est pas terminée.
6. **Aucune permission `INTERNET`**. L'app est 100 % locale, sans analytics ni réseau.
7. **Aucune suppression sans confirmation système explicite** de l'utilisateur.
8. Avant de coder une étape, relire la section concernée. En cas d'ambiguïté, noter l'hypothèse dans `docs/decisions/` (mini ADR) plutôt que d'improviser en silence.

---

## 1. Vision et périmètre

### Objectif
Faire revoir à l'utilisateur, chaque jour, les photos et vidéos prises **le même jour (mois/jour) lors des années précédentes**, et lui permettre de les trier d'un geste.

### Périmètre v1
- Photos **et** vidéos de la bibliothèque locale (MediaStore).
- Tri par swipe : gauche = marquer pour suppression, droite = garder, haut = menu (favori / partage).
- Revue du jour + **rattrapage des 7 derniers jours**.
- Suppression **groupée** en fin de session, après confirmation système.
- Notification quotidienne, **uniquement s'il reste des médias à trier**.
- FR + EN, thème Material 3 dynamique.
- **Tutoriel de première utilisation** animé, passable et rejouable (réalisé en dernier, voir §4).

### Hors périmètre v1 (voir roadmap §15)
Détection de doublons, statistiques, widget, synchronisation cloud, édition de médias, choix de la taille de la fenêtre de rattrapage.

---

## 2. Décisions produit validées

| Sujet | Décision |
|---|---|
| Plateforme | Android natif, Kotlin + Jetpack Compose |
| Version minimale | Android 13 (API 33) |
| Cible | Usage perso d'abord, Play Store ensuite (à garder compatible) |
| Contenu | Photos + vidéos |
| Suppression | Marquage pendant le tri, puis confirmation groupée en fin de session |
| Photo gardée | Réapparaît l'année suivante (voir §3.3) |
| Périmètre temporel | Aujourd'hui + rattrapage des 7 derniers jours |
| Geste haut | Petit menu : favori + partage |
| Notification | Quotidienne, seulement s'il y a des médias à trier |

### Décisions par défaut prises pour compléter (à valider, voir §16)
- La « suppression » utilise la **corbeille système** (`MediaStore.createTrashRequest`), restaurable environ 30 jours. La suppression définitive n'est pas proposée en v1.
- « Favori » dans le menu = favori système **+** marque la photo comme gardée, puis passe à la carte suivante. « Partager » ouvre la feuille de partage, la carte reste affichée.
- Le favori système est **demandé aussitôt** à la galerie (`createFavoriteRequest` pour ce seul média, PS-85) : sans rien demander avec l'accès « Gestion des médias », sinon par le dialogue d'Android. Accepté : le favori est appliqué ; refusé : le média reste gardé, favori en attente (`favorite_pending`) pour un nouvel essai.

---

## 3. Règles métier

### 3.1 Sélection des médias d'un jour
Pour un jour cible `D` (aujourd'hui ou l'un des 6 jours précédents), la revue contient les médias dont la date de prise de vue a le **même mois et le même jour que `D`**, dans des **années strictement antérieures à l'année de `D`**.

- Exemple : le 2 janvier 2027, le rattrapage inclut le 28 décembre 2026 ; ses médias « du même jour » sont ceux du 28 décembre 2025, 2024, etc., pas ceux de 2026.
- **Date de prise de vue** : `DATE_TAKEN`. Si absente ou nulle, repli sur `DATE_ADDED`. Documenter ce choix dans le code.
- **Fuseau horaire** : celui de l'appareil au moment du calcul. Limite connue (photos prises dans un autre fuseau), à documenter.
- **29 février** : les médias du 29 février apparaissent le **28 février** des années non bissextiles.
- Exclure les médias déjà dans la corbeille système.

### 3.2 Ordre du paquet
Par défaut : année décroissante (les souvenirs les plus récents d'abord), puis heure croissante. L'ordre est encapsulé dans une stratégie (`DeckOrdering`) pour pouvoir changer sans toucher au reste.

### 3.3 Cycle de revue annuel
Une décision est liée à un **couple (média, année de revue)**, où l'année de revue est l'année du jour cible `D`.

- `KEPT` en 2026 : le média n'est plus proposé en 2026 pour ce jour, mais **redevient à trier en 2027**.
- `MARKED_FOR_DELETION` : persiste tant que la suppression n'est pas confirmée ou annulée.
- `TRASHED` : confirmé et envoyé à la corbeille. Le média disparaît de MediaStore, la ligne est purgée par le nettoyage des orphelins.
- Absence de ligne = **à trier**.

### 3.4 Session
Une session = une revue de la pile d'un jour (ou « tout trier » sur la fenêtre de 7 jours). Elle se termine quand la pile est vide ou que l'utilisateur quitte.

- Les décisions sont **écrites en base immédiatement** (survie à la mort du process).
- Un **undo en pile** annule les décisions de la pile d'un jour, de la plus récente à la plus ancienne, tant que rien n'est confirmé. Cet historique est **enregistré en base** : il survit à la sortie de la pile et à la fermeture de l'app (journal des décisions, ADR 0019).
- Les marquages non confirmés restent visibles sur l'accueil (« N médias en attente de suppression »).

### 3.5 Confirmation en fin de session
1. Écran de résumé : **espace libéré estimé**, puis « Trié aujourd'hui » (gardés, favoris : tout ce qui a été trié aujourd'hui, toutes piles confondues, ADR 0024) et « En attente de votre confirmation » (marqués).
2. Bouton « Confirmer la suppression » : demande système `createTrashRequest` (par lots pour éviter les limites de taille des Intents, par exemple 500 URI).
3. Le résumé ne demande plus les favoris : « Confirmer la suppression » ne concerne que la corbeille.
4. Résultat refusé ou annulé : **les marquages restent en base**, rien n'est perdu ; message clair à l'utilisateur.

> ⚠️ Android affiche un dialogue de confirmation pour chaque requête de ce type sur des médias dont l'app n'est pas propriétaire. C'est pourquoi tout est groupé en fin de session et non fait à chaque swipe.

5. **Accès « Gestion des médias »** (`MANAGE_MEDIA`, ADR 0023) : donné par l'utilisateur dans les réglages d'Android, il fait appliquer les demandes de corbeille et de favori **sans dialogue d'Android** (vérifié sur émulateur, `ManageMediaInstrumentedTest`). Après le premier dialogue d'Android validé, l'accueil propose : « Marre des confirmations ? Activez la gestion des médias dans les paramètres », le mot « paramètres » ouvrant le réglage ; le conseil se ferme pour de bon, et disparaît une fois l'accès donné. Avec l'accès, PhotoSwipe affiche **sa propre confirmation** (dialogue Material 3) avant l'envoi à la corbeille, puisque celle d'Android disparaît.

### 3.6 Notification
- Tâche périodique quotidienne (WorkManager). L'heure est approximative (Doze), c'est acceptable.
- Envoyée **seulement si** le nombre de médias à trier sur la fenêtre est > 0.
- Réglages : activée/désactivée, heure souhaitée (défaut 18:00).
- Demande de `POST_NOTIFICATIONS` au moment du réglage, pas au premier lancement.

---

## 4. Écrans et parcours

1. **Permissions / onboarding** : explication courte, demande `READ_MEDIA_IMAGES` + `READ_MEDIA_VIDEO`. Gérer l'accès partiel d'Android 14+ (`READ_MEDIA_VISUAL_USER_SELECTED`) avec une bannière « accès limité, vos souvenirs peuvent être incomplets » et un raccourci pour étendre la sélection. Gérer le refus avec un lien vers les réglages.
2. **Accueil** :
   - carte « Aujourd'hui » (nombre à trier), avec une vignette par année et les années des souvenirs (« De 2016 à 2025 ») ;
   - liste des 6 jours précédents avec leur nombre en attente et une vignette ;
   - bannière « N médias en attente de suppression » ;
   - accès aux réglages.
3. **Pile de swipe** :
   - carte plein écran (image ou vidéo) avec le repère de bord de film « Il y a 3 ans · 14 mai 2023 » et le poids du fichier, et la progression du jour (« 4 sur 23 ») ;
   - glisser à gauche : indicateur rouge + icône corbeille ; à droite : indicateur vert + coche ; en haut : menu favori / partage (feuille avec un aperçu du média) ;
   - **boutons équivalents sous la carte** (supprimer / annuler / plus d'actions / garder), libellés, pour l'accessibilité et TalkBack ; celui du côté glissé se remplit pendant le geste ;
   - pile vidée : « Voir le résumé » et « Retour à l'accueil » ;
   - retour haptique, animations à ressort, seuil de déclenchement et retour au centre si geste incomplet ;
   - vidéos : lecture automatique muette, écran gardé allumé pendant la lecture ; un appui sur le bouton ▶ lance la vidéo avec le son, que le bouton du son peut couper ; le choix du son vaut pour les vidéos suivantes.
4. **Résumé / confirmation** : voir §3.5 ; vignettes des derniers médias marqués.
5. **Réglages** : notification (on/off + heure), langue (réglage de langue par application d'Android 13+), thème (celui du téléphone, clair ou sombre, en boutons radio dans la liste), à propos. En debug : outils développeur (§10).
6. **Icône** : icône adaptative « pile de tirages » avec sa couche monochrome (icône à thème), écran de démarrage de la plateforme.

États vides soignés : rien à trier aujourd'hui, permissions refusées, aucun média trouvé.

### Tutoriel de première utilisation (jalon 11, réalisé en dernier)

Objectif : expliquer le fonctionnement au premier lancement avec de petites animations et du texte. Il est réalisé **à la fin du développement**, pour couvrir toutes les fonctionnalités réellement livrées.

- Affiché **une seule fois** (état et numéro de version stockés dans DataStore), **avant** l'écran de permissions. Il peut être **passé** à tout moment et **revu depuis les réglages**. Si son contenu change fortement, on incrémente sa version pour le proposer à nouveau.
- Pages (carrousel M3 avec indicateur) :
  1. le principe « souvenirs du même jour » ;
  2. swipe droite = garder (il reviendra l'année suivante), swipe gauche = marquer (rien n'est supprimé sans confirmation) ;
  3. swipe haut = favori / partage ;
  4. rattrapage des 7 derniers jours et rappel quotidien ;
  5. confirmation groupée et corbeille restaurable.
- **Mode entraînement** : un dernier écran propose d'essayer le swipe sur des **cartes d'exemple intégrées à l'app**. Le tutoriel ne touche **jamais** aux vraies photos de l'utilisateur (aucun accès MediaStore, aucune décision enregistrée).
- Animations en **Compose** (pas de bibliothèque supplémentaire sauf justification en ADR), illustrations vectorielles.
- Accessibilité : respect du réglage système de suppression des animations (version statique), `contentDescription`, navigation par boutons possible sans geste, textes FR/EN.
- Revue finale : vérifier que le contenu couvre toutes les fonctionnalités livrées.

---

## 5. Architecture

### 5.1 Modules Gradle

```
:app        UI Compose, navigation, DI (Hilt), point d'entrée Android, notifications
:data       Implémentations Android : MediaStore, Room, DataStore, WorkManager
:domain     Kotlin JVM pur : modèles, use cases, ports (interfaces), services métier
```

Sens des dépendances (toujours vers le domaine) :

```
:app ───────► :domain ◄─────── :data
  │                              ▲
  └──────── (Hilt câble) ────────┘
```

`:domain` ne doit dépendre d'aucune bibliothèque Android. Un **test d'architecture** (Konsist ou ArchUnit) vérifie cette règle et l'absence de dépendance `:app` → `:data` en dehors du câblage DI.

### 5.2 Domaine (`:domain`)

- **Modèles** : `MediaId`, `MediaItem` (id, URI en `String`, type, date de prise, taille, durée), `MediaType`, `ReviewDay`, `ReviewDecision`, `SessionSummary`.
- **Services purs** : `SameDayMatcher` (règles §3.1, dont le 29 février), `ReviewWindowProvider` (7 jours, injecté avec une `Clock`), `DeckOrdering`.
- **Ports (interfaces)** :
  - `MediaSource` : lecture des médias par plages de dates ;
  - `ReviewRepository` : décisions par (média, année de revue) ;
  - `SettingsRepository` : préférences ;
  - `MediaActionRequestFactory` : prépare les demandes de confirmation système (corbeille, favori) sous forme d'objet opaque pour le domaine ;
  - `ReminderScheduler` : planification de la notification.
- **Use cases** (un par intention, `operator fun invoke`) : `GetReviewDays`, `GetDeckForDay`, `KeepMedia`, `MarkForDeletion`, `FavoriteMedia`, `UndoLastDecision`, `RequestFavorite`, `RecordFavoriteResult`, `GetSessionSummary`, `PrepareTrash`, `RecordTrashResult`, `CleanOrphanEntries`, `ScheduleDailyReminder`.
- Toute dépendance temporelle ou de concurrence est **injectée** (`Clock`, `CoroutineDispatcher`), jamais appelée en statique.

### 5.3 Données (`:data`)

- `MediaStoreMediaSource` : requêtes `ContentResolver` sur images + vidéos. **Ne pas** utiliser de fonctions SQL de date dans la sélection (restrictions de sélection depuis Android 11, risque de rejet) : calculer côté domaine des plages `[début, fin[` en millisecondes epoch par année et par jour, puis les passer à la requête.
- `RoomReviewRepository` : table des décisions, index unique `(mediaId, reviewYear)`.
- `DataStoreSettingsRepository`.
- `MediaStoreActionRequestFactory` : `createTrashRequest`, `createFavoriteRequest`, avec découpage en lots.
- `WorkManagerReminderScheduler` + worker (Hilt) qui calcule le nombre en attente et poste la notification.

### 5.4 Présentation (`:app`)

- Architecture **MVVM + flux unidirectionnel de données (UDF)** : chaque écran a un `UiState` immuable, des `UiEvent` entrants, des `UiEffect` sortants (navigation, lancement du dialogue système).
- ViewModel : n'expose que `StateFlow<UiState>` ; ne connaît que les use cases.
- Le lancement du dialogue de confirmation système (IntentSender) est fait par l'écran via `rememberLauncherForActivityResult` ; le résultat revient au ViewModel par un `UiEvent`.
- Navigation : Navigation Compose avec routes typées.
- Composables **sans état** (state hoisting), prévisualisables (`@Preview`).

### 5.5 Injection de dépendances
**Hilt** (KSP). Modules Hilt dans `:data` pour les implémentations et dans `:app` pour l'assemblage. En debug, un module de remplacement permet de substituer `MediaSource` par la version fixtures (§10).

---

## 6. Modèle de données (Room)

Table `review_entry` :

| Colonne | Type | Remarque |
|---|---|---|
| `media_id` | Long | ID MediaStore |
| `review_year` | Int | Année du jour cible (voir §3.3) |
| `decision` | Enum | `KEPT`, `MARKED_FOR_DELETION`, `TRASHED` |
| `favorite_pending` | Boolean | Favori demandé, non encore appliqué |
| `size_bytes` | Long | Pour l'espace libéré estimé |
| `decided_at` | Long | Epoch ms (utile pour l'undo) |

Clé primaire / index unique : `(media_id, review_year)`.

- Les migrations Room sont **exportées et testées** dès la v1 (schémas versionnés dans le dépôt).
- **Nettoyage des orphelins** : au démarrage et après chaque confirmation, supprimer les lignes dont le média n'existe plus dans MediaStore.

---

## 7. Stack technique

| Domaine | Choix |
|---|---|
| Langage / build | Kotlin, Gradle Kotlin DSL, **version catalog** (`libs.versions.toml`), KSP |
| UI | Jetpack Compose, **Material 3** |
| DI | Hilt |
| Async | Coroutines + Flow |
| Persistance | Room (décisions), DataStore (préférences) |
| Médias | **Coil 3** (images, miniatures vidéo), **Media3 ExoPlayer** (lecture vidéo, une seule instance réutilisée et bien libérée) |
| Tâches | WorkManager |
| Qualité | ktlint + detekt, Android Lint |
| CI | GitHub Actions : lint + tests unitaires + build à chaque push |

`compileSdk` / `targetSdk` : dernière version stable (le Play Store impose une cible récente). `minSdk` : 33.
`applicationId` suggéré : `fr.babeuloula.photoswipe` (à confirmer).

---

## 8. Design et interface

Objectif : une UI qui suit au maximum les recommandations de Google et évolue avec elles.

- **Material 3** partout, composants standards en priorité (pas de composants maison si un équivalent M3 existe). Adopter les évolutions *Material 3 Expressive* quand leurs APIs sont stables.
- **Palette ambre « planche-contact »**, la direction artistique de l'app, toujours appliquée : pas de couleurs dynamiques (Material You), décision de Jérémy du 2026-11-06 (ADR 0025, PS-102).
- **Thème clair / sombre** suivant le système par défaut, ou imposé dans les réglages (« Celui du téléphone », « Clair », « Sombre »), y compris pour les icônes des barres système.
- **Edge-to-edge**, gestion des insets, **retour prédictif** activé (`enableOnBackInvokedCallback`).
- **Mises en page adaptatives** (`WindowSizeClass`) : téléphone, pliable, tablette.
- Typographie, formes et mouvements : uniquement via le thème (`MaterialTheme`), aucune valeur en dur dispersée.
- **Direction « planche-contact »** (ADR 0025) : palette de repli ambre (graine #8A6A1E) aux neutres gris papier ; Atkinson Hyperlegible Next pour le texte et IBM Plex Mono pour les grands nombres et le repère « Il y a N ans », polices embarquées ; icônes Material Symbols Rounded seulement ; une seule couleur d'accent par écran (« Confirmer la suppression » en `primary`).
- **Accessibilité** : alternatives aux gestes (boutons), `contentDescription`, tailles de cible ≥ 48 dp, contrastes, support TalkBack et grande police.

---

## 9. Internationalisation (FR + EN dès le départ)

- Ressources par défaut en **anglais** (`values/`), traduction française (`values-fr/`).
- Pluriels via `plurals.xml` (ex. « 1 média », « 3 médias »). Aucune concaténation de chaînes.
- Dates et tailles formatées selon la locale (`DateTimeFormatter`, formateurs système).
- `res/xml/locales_config.xml` + `android:localeConfig` pour le **choix de langue par application**.
- Texte de notification localisé.
- Un test vérifie que chaque clé existe dans les deux langues, en plus du lint.

---

## 10. Fixtures et seeder

Trois niveaux, du plus simple au plus réaliste :

1. **Fixtures en mémoire** (`testFixtures` de `:domain`) : builders (`aMediaItem()`, `aReviewEntry()`), `FakeMediaSource`, `FakeReviewRepository`, `FixedClock`. Utilisés par tous les tests unitaires et UI.
2. **Données de démo en debug** : un écran « Outils développeur » (présent uniquement dans `src/debug`) permet de basculer sur `FakeMediaSource` avec un jeu de données généré : médias répartis sur plusieurs années pour aujourd'hui et les 6 jours précédents, y compris cas limites (29 février, plusieurs médias le même jour, grosse vidéo, média sans `DATE_TAKEN`).
3. **Seeder MediaStore réel** (debug) : insère des images et vidéos d'exemple dans MediaStore avec `DATE_TAKEN` choisi, pour tester le vrai chemin de requête sur émulateur.
   - Limite à documenter : les médias insérés par l'app lui appartiennent, donc les dialogues système de confirmation peuvent ne pas apparaître. Pour tester ces dialogues, fournir aussi un script `scripts/seed-emulator.sh` qui pousse des JPEG avec EXIF daté via `adb` puis déclenche un scan.

Les outils développeur ne doivent **jamais** être inclus dans la variante `release`.

---

## 11. Stratégie de tests

| Niveau | Cible | Outils |
|---|---|---|
| Unitaires domaine | `SameDayMatcher` (29 février, changement d'année, fenêtre de 7 jours), cycle de revue annuel, use cases, undo | JUnit 5, MockK, Turbine, kotlinx-coroutines-test |
| Unitaires présentation | ViewModels : états, événements, effets | JUnit, Turbine, fakes |
| Intégration données | DAO Room en mémoire, migrations ; requêtes MediaStore (instrumentés) | AndroidX Test, Room testing |
| UI / fonctionnels | Pile de swipe, boutons alternatifs, résumé, états vides, permissions | Compose UI Test + Hilt test, données fixtures |
| Architecture | Règles de dépendance entre modules, domaine sans Android | Konsist ou ArchUnit |
| Localisation | Clés FR/EN cohérentes | Lint + test dédié |

Objectifs : couverture du domaine **≥ 90 %**, tous les ViewModels testés, chaque cas limite de §14 couvert par au moins un test. Tests nommés en anglais, décrivant le comportement (`` `moves leap day media to February 28 on non-leap years` ``). Les tests unitaires tournent sans émulateur et sont exécutés en CI.

---

## 12. Conventions de code et documentation

- Respect de **SOLID** : une responsabilité par classe, dépendance sur des interfaces, injection par constructeur, pas de singletons statiques.
- Fonctions courtes, nommage explicite, pas de commentaire redondant : commenter le **pourquoi** (règles métier, contournements Android).
- **KDoc en français** sur toute API publique des modules.
- Livrables de documentation :
  - `README.md` : présentation, prérequis, lancer l'app, lancer les tests, utiliser le seeder.
  - `docs/architecture.md` : modules, schéma des dépendances, flux de données, justification des bibliothèques.
  - `docs/decisions/NNNN-titre.md` : décisions importantes (mini ADR).
  - `docs/testing.md` : stratégie et commandes.
- Format et analyse statiques (ktlint, detekt) bloquants en CI.

---

## 13. Permissions et Play Store

Permissions déclarées : `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_VISUAL_USER_SELECTED`, `POST_NOTIFICATIONS`. **Aucune** autre, en particulier pas `INTERNET` ni `MANAGE_EXTERNAL_STORAGE`.

Pour une publication future : Google encadre l'accès large aux photos et vidéos. Il faudra une **déclaration d'usage** justifiant que le tri de la bibliothèque est la fonction principale de l'app, une politique de confidentialité (même sans collecte de données) et le formulaire « Sécurité des données ». Rien de cela ne bloque la v1 perso, mais ne pas prendre de raccourci qui l'empêcherait (le sélecteur de photos système ne convient pas ici, car il faut interroger par date).

---

## 14. Cas limites à couvrir

- Média supprimé hors de l'app entre deux sessions (ligne orpheline, URI invalide).
- Permission révoquée ou réduite pendant une session.
- Accès partiel (Android 14+) : tri sur un sous-ensemble, bannière visible.
- Dialogue système refusé : marquages conservés, message clair.
- Mort du process en pleine session : reprise depuis la base, rien de perdu.
- Beaucoup de médias (milliers) : chargement paginé ou différé, miniatures à la bonne taille, pré-chargement de la carte suivante seulement.
- Vidéos : lecture, mémoire, libération du lecteur, rotation de l'écran.
- Médias sans `DATE_TAKEN`, captures d'écran, images WhatsApp.
- Changement de fuseau horaire ou d'heure d'été entre deux ouvertures.
- Jours sans aucun média, première utilisation sans historique.
- Changement de langue système pendant l'utilisation.

---

## 15. Roadmap et ordre de réalisation

**Jalons pour Claude Code, dans cet ordre, chacun livré avec ses tests :**

1. **Squelette** : modules, version catalog, Hilt, thème M3, CI, lint, i18n de base.
2. **Domaine** : modèles, `SameDayMatcher`, `ReviewWindowProvider`, use cases, fixtures + tests complets.
3. **Données** : Room, DataStore, `MediaStoreMediaSource`, tests d'intégration.
4. **Permissions + accueil** avec données de démo.
5. **Pile de swipe** (images), boutons alternatifs, undo.
6. **Vidéos** (Media3).
7. **Résumé + confirmation système** (corbeille, favoris).
8. **Notification** quotidienne conditionnelle.
9. **Réglages**, langue par application, finitions d'accessibilité.
10. **Seeder MediaStore + script adb**, documentation finale.
11. **Tutoriel de première utilisation** (pages animées, mode entraînement), réalisé en dernier pour couvrir toutes les fonctionnalités.

**Idées post-v1** : statistiques et espace libéré cumulé, widget « souvenir du jour », fenêtre de rattrapage réglable (7/30/90, déjà prévue comme paramètre interne), détection de doublons, vue des favoris.

---

## 16. Points ouverts à valider

1. Corbeille système uniquement en v1 (pas de suppression définitive) : OK ?
2. Comportement du menu haut : « Favori » = favori + gardé + carte suivante ; « Partager » = carte conservée. OK ?
3. Ordre de la pile : année décroissante puis heure croissante. OK ?
4. 29 février affiché le 28 février des années non bissextiles. OK ?
5. Heure de notification par défaut : 18:00. OK ?
6. `applicationId` : `fr.babeuloula.photoswipe`. À confirmer.
7. Effet réel sur Google Photos de la suppression locale d'un média sauvegardé : à vérifier sur appareil, **ne pas le promettre dans l'UI**. De même, le favori système (`IS_FAVORITE` de MediaStore) n'apparaît pas comme étoile dans Google Photos, qui garde ses propres favoris : constaté sur émulateur le 2026-11-06 (MediaStore à jour, pas d'étoile). Les galeries qui lisent MediaStore l'affichent.
8. Tutoriel : affiché **avant** l'écran de permissions, avec un mode entraînement sur cartes d'exemple. OK ?
9. Tutoriel : animations en Compose pur (sans Lottie), 5 pages + entraînement. OK ?
10. Accès « Gestion des médias » proposé sur l'accueil après le premier dialogue d'Android validé, avec une confirmation de PhotoSwipe à la place de celle d'Android (§3.5, ADR 0023) : décidé par Jérémy le 2026-11-05.
