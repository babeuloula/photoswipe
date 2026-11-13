# 0038 — Liens profonds et raccourcis vers la pile du jour

- **Statut** : accepté
- **Date** : 2026-11-13
- **Tickets** : PS-171 (liens profonds et raccourcis de l'icône)

## Contexte
Seule la notification du rappel ouvrait directement la pile du jour (`MainActivity.openDeckIntent`, par un extra privé). On veut lancer le tri depuis l'icône de l'application (appui long) et depuis une routine d'automatisation (Tasker, Raccourcis, `adb`), sans permission nouvelle, sans `INTERNET`, et sans rien exposer aux autres applications.

## Décision
- **Un schéma privé `photoswipe://`**, reçu par `MainActivity` (filtre `VIEW`, catégories `DEFAULT` et `BROWSABLE`) :
  - `photoswipe://today` : la pile du jour ;
  - `photoswipe://summary` : le résumé du tri d'aujourd'hui ;
  - `photoswipe://day/AAAA-MM-JJ` : la vue d'ensemble de cette date (souvenirs des années passées).
  Pas d'App Links en `https://` : ils demanderaient un domaine vérifié, et l'application n'a pas de site.
- **Un seul chemin pour toutes les demandes de lancement** : `MainActivity.launchRequestOf(intent)` lit l'extra du rappel ou le lien (`DeepLinks.parse`, package `link`) et produit une `LaunchRequest` (`Home`, `TodayDeck`, `Summary`, `DayOverview(day)`, `Deck(day)` pour le rappel). Elle n'est lue qu'au premier lancement ou par `onNewIntent` : une activité recréée garde sa navigation.
- **La demande attend le tutoriel et l'accès aux médias** : elle reste dans l'état de l'activité jusqu'à ce que `PhotoSwipeNavHost` soit composé, donc après `TutorialGate` et `MediaAccessGate`, comme un lancement ordinaire.
- **Résolue en écran après l'accès, par `LaunchRequestResolver`**, qui relit la galerie (`GetDayOverview`) pour ne jamais ouvrir un écran vide : la pile du jour seulement s'il y reste un souvenir à trier, la vue d'ensemble seulement si la date a au moins un souvenir ; sinon l'accueil. Le résumé s'ouvre toujours (il gère lui-même l'absence de marquage).
- **Un lien invalide ouvre l'accueil** : schéma, écran ou date inconnus (`photoswipe://day/2026-02-30`), segments en trop. Aucun lien ne plante l'application.
- **L'écran demandé s'empile au-dessus de l'accueil** (`popUpTo<HomeRoute>`, `launchSingleTop`) : « Retour » ramène toujours à l'accueil, même si l'application était déjà ouverte ailleurs.
- **Raccourcis statiques** dans `res/xml/shortcuts.xml` (méta-donnée `android.app.shortcuts` de l'activité de lancement) : « Trier aujourd'hui » (`photoswipe://today`) et « Résumé » (`photoswipe://summary`), intents explicites vers `MainActivity`. Icônes adaptatives `shortcut_*.xml` : le symbole Material Symbols Rounded (`ic_swipe`, `ic_summarize`) blanc sur le fond ambre de l'icône, avec couche monochrome.

## Conséquences
- Un lien ne fait qu'ouvrir un écran : il ne prend aucune décision, n'en annule aucune et ne renvoie rien. Aucun `ContentProvider`, aucun résultat d'activité, aucun compteur dans les raccourcis (statiques, sans libellé dynamique) : rien n'est exposé aux autres applications.
- `BROWSABLE` permet d'ouvrir un lien depuis une page web ou une application de notes. Le risque se limite à ouvrir un écran de l'application, ce qu'un appui sur l'icône permet déjà.
- `MainActivity` garde son mode de lancement `standard` : un lien envoyé alors que l'application est ouverte peut empiler une seconde instance selon les drapeaux de l'expéditeur (les raccourcis, eux, relancent la tâche). Sans conséquence sur les données.
- Le schéma est public : le README le documente. Le changer casserait les routines des utilisateurs.
- Les raccourcis sont statiques : pas de raccourci dynamique « jour à rattraper », qui demanderait de publier des données (nombres, dates) au lanceur.
