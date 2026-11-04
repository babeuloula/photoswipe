# 0014 — Navigation typée et accès aux médias en porte d'entrée

- **Statut** : accepté
- **Date** : 2026-11-04
- **Ticket** : PS-35

## Contexte
La SPEC §5.4 demande Navigation Compose avec des routes typées, et le ticket des coquilles pour les écrans : permissions, accueil, pile, résumé, réglages. Le parcours d'accès aux médias existe déjà (`MediaAccessGate`, PS-33) et doit ramener à l'explication si l'accès est retiré, quel que soit l'écran.

## Décision
- **Navigation Compose 2.10.2** avec des routes `@Serializable` (plugin `org.jetbrains.kotlin.plugin.serialization` à la version de Kotlin, `kotlinx-serialization-core` 1.12.0) : `HomeRoute`, `DeckRoute(epochDay)`, `SummaryRoute`, `SettingsRoute`, dans `ui/navigation`.
- **Le jour cible voyage en `epochDay` (`Long`)** : les routes typées ne transportent que des types simples ; `DeckRoute.of(LocalDate)` et `DeckRoute.day` font la conversion.
- **Les permissions ne sont pas une destination** : `MediaAccessGate` entoure le `NavHost`. Un accès retiré remplace tout le graphe par l'explication, sans pile de retour incohérente ; l'accès rendu fait réapparaître le graphe.
- **Coquilles** : un `ShellScreen` commun (barre du haut avec retour, message d'attente, actions), remplacé écran par écran dans les tickets suivants.
- **Retour prédictif** : `android:enableOnBackInvokedCallback="true"` sur l'application (SPEC §8).
- **Horloge** : `MainActivity` reçoit la `Clock` par injection et la transmet à l'interface pour calculer « aujourd'hui ».

## Alternatives envisagées
- **Navigation 3** : API plus récente, mais la SPEC nomme Navigation Compose et ses routes typées couvrent le besoin.
- **Route `PermissionsRoute`** : il faudrait vider la pile de retour à chaque révocation et protéger chaque destination ; la porte d'entrée le fait en un seul endroit.
- **`LocalDate` dans la route avec un `NavType` personnalisé** : plus de code pour un gain nul.

## Conséquences
- Chaque écran réel remplace sa coquille dans `PhotoSwipeNavHost` et garde sa route.
- Les écrans du graphe ont leur propre `Scaffold` (marges des barres système) ; la bannière d'accès limité consomme la marge de la barre d'état au-dessus d'eux.
