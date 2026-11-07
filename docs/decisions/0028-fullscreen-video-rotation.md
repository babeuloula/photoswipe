# 0028 — Le plein écran vidéo, seule exception au portrait

- **Statut** : accepté
- **Date** : 2026-11-07
- **Ticket** : PS-119

## Contexte
L'application est verrouillée en portrait (`screenOrientation="portrait"` dans le manifeste, décision de Jérémy, PS-32). Jérémy veut un plein écran vidéo, ouvert par un bouton sous celui du son, « seul écran où on pourra orienter le téléphone », sans zoom.

## Décision
- **Le plein écran est un `Dialog` plein écran** (`FullscreenVideo`, `ui/deck`), composé par la carte du dessus : il garde le lecteur unique de la pile (`LocalCardVideoPlayer`), donc la vidéo, sa position et le son. Tant qu'il est ouvert, la carte ne compose plus sa propre surface : un lecteur n'affiche qu'une surface à la fois.
- **L'orientation se libère à l'exécution** (`AllowRotation`) : `requestedOrientation = SCREEN_ORIENTATION_FULL_USER` à l'ouverture, qui suit la rotation automatique du téléphone (verrou de rotation respecté), puis `SCREEN_ORIENTATION_PORTRAIT` à la fermeture. Une rotation recrée l'activité : la fermeture n'est pas appliquée pendant ce changement de configuration (`isChangingConfigurations`).
- **L'état survit à la recréation** : le plein écran ouvert par `rememberSaveable`, la vidéo par `VideoPlaybackState` (PS-52).
- **Immersif** : barres système masquées dans la fenêtre du dialogue, rappelées par un glissement depuis le bord. Commandes effacées après 3 s de lecture, sauf si l'accessibilité demande plus de temps (`calculateRecommendedTimeoutMillis`, lu avant la fenêtre du dialogue qui fournit ses propres réglages de plateforme).

## Alternatives envisagées
- **Activité dédiée sans verrou d'orientation** : orientation libre par le manifeste, mais il faudrait passer le lecteur (ou sa position) d'une activité à l'autre et en recréer un.
- **`configChanges="orientation|screenSize"` sur `MainActivity`** : évite la recréation à la rotation, mais toute l'application devrait gérer elle-même ses changements de configuration.
- **`SCREEN_ORIENTATION_SENSOR`** : pivoterait même avec la rotation automatique désactivée, contre le choix de l'utilisateur.

## Conséquences
- Une rotation pendant le plein écran recrée l'activité et le lecteur ; la lecture reprend à sa position.
- Sur grand écran, Android 16+ ignore déjà le verrou en portrait : rien ne change.
