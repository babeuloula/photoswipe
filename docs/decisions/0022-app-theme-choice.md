# 0022 — Thème de l'application choisi dans les réglages

- **Statut** : accepté
- **Date** : 2026-11-06
- **Ticket** : PS-87

## Contexte
La SPEC §8 prévoyait un thème clair ou sombre qui suit le système. Jérémy a demandé de pouvoir l'imposer depuis les réglages. Le choix doit s'appliquer tout de suite, barres système comprises, et ne pas faire apparaître l'application un instant dans le mauvais thème au démarrage.

## Décision
- **Domaine** : `ThemeMode` (`SYSTEM` par défaut, `LIGHT`, `DARK`), lu et écrit par `SettingsRepository` (`observeThemeMode`, `saveThemeMode`), stocké par son nom dans DataStore ; un nom inconnu revient à `SYSTEM`.
- **Application** : `MainActivity` observe le thème et le passe à `PhotoSwipeTheme(darkTheme = …)`. Les couleurs dynamiques restent actives. Les icônes des barres système suivent le thème choisi : `enableEdgeToEdge` est rappelé avec `SystemBarStyle.auto { darkTheme }` à chaque changement.
- **Démarrage** : le choix enregistré est lu une fois, de façon bloquante, avant `setContent` (petit fichier local, quelques millisecondes), pour que le premier affichage soit déjà dans le bon thème.
- **Réglages** : section « Apparence », dialogue à boutons radio ; le `SettingsViewModel` écrit directement par le port, sans use case, faute de règle métier.

## Complément : noir pur (PS-176, 2026-11-13)
Pour économiser la batterie d'un écran OLED et mieux voir les photos dans le noir, un interrupteur « Noir pur » s'ajoute à la section « Apparence », désactivé par défaut.
- **Domaine** : un booléen à part, pas une quatrième valeur de `ThemeMode` : il ne change que le thème sombre, que celui-ci soit imposé ou suivi du téléphone. Lu et écrit par `SettingsRepository` (`observePureBlack`, `savePureBlack`), clé `pure_black` de DataStore, `false` si absente.
- **Palette** : `PureBlackColorScheme` (`ui/theme/Color.kt`) reprend le schéma sombre ; `background`, `surface`, `surfaceDim` et `surfaceContainerLowest` passent au noir, `surfaceContainerLow` prend l'ancien fond (#15130F) ; `surfaceContainer` et au-dessus gardent leurs gris, pour que cartes, listes segmentées et feuilles restent distinctes du fond. Les contrastes WCAG AA sont vérifiés par `ContrastTest`. `PhotoSwipeTheme(pureBlack = …)` l'ignore en thème clair.
- **Application** : `MainActivity` observe le choix comme le thème, le lit une fois avant `setContent`, et passe la barre de navigation à `SystemBarStyle.dark(Color.BLACK)` en noir pur, pour qu'une barre à trois boutons ne pose pas de voile gris sur le fond noir.
- **Limite** : l'écran de démarrage de la plateforme (`splash_background`) reste sur le fond sombre gris, faute de pouvoir lire DataStore avant lui.

## Alternatives envisagées
- **`AppCompatDelegate.setDefaultNightMode`** : demande AppCompat, absent du projet, et recrée l'activité.
- **Premier affichage avec le thème du téléphone, puis correction** : un éclair du mauvais thème à chaque lancement pour qui a fait un autre choix.
- **Écran de démarrage tenu jusqu'à la lecture** (`SplashScreen`) : une bibliothèque de plus pour quelques millisecondes.
