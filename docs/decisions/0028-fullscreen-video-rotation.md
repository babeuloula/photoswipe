# 0028 — Le plein écran vidéo, seule exception au portrait

- **Statut** : accepté
- **Date** : 2026-11-07
- **Ticket** : PS-119

## Contexte
L'application est verrouillée en portrait (`screenOrientation="portrait"` dans le manifeste, décision de Jérémy, PS-32). Jérémy veut un plein écran vidéo, ouvert par un bouton sous celui du son, « seul écran où on pourra orienter le téléphone », sans zoom.

## Décision
- **Le plein écran est un `Dialog` plein écran** (`FullscreenVideo`, `ui/deck`), composé par la carte du dessus : il garde le lecteur unique de la pile (`LocalCardVideoPlayer`), donc la vidéo, sa position et le son. Tant qu'il est ouvert, la carte ne compose plus sa propre surface : un lecteur n'affiche qu'une surface à la fois.
- **L'orientation se décide pour toute l'activité** (`RotationPolicy`, `ui/common`, posée par `MainActivity`, PS-126) : les écrans qui autorisent la rotation s'y signalent (`AllowRotation`, un compteur) ; tant qu'il y en a un, `requestedOrientation = SCREEN_ORIENTATION_FULL_USER`, qui suit la rotation automatique du téléphone (verrou de rotation respecté), sinon `SCREEN_ORIENTATION_PORTRAIT`. L'orientation est relue après chaque composition : pendant la recréation due à une rotation, le portrait n'est pas demandé un instant, et si le plein écran n'est pas rouvert, le portrait revient. La première version (PS-119) rendait le portrait dans le `onDispose` de l'écran, sauf pendant un changement de configuration : le plein écran n'étant pas restauré après une rotation, toute l'application restait libre de tourner (bug PS-126).
- **L'état survit à la recréation** : la vidéo ouverte en plein écran est enregistrée par la pile (`LoadedDeck`, `rememberSaveable`, fournie aux cartes par `LocalFullscreenVideo`), hors de la carte composée dans un `BoxWithConstraints`, où elle n'était pas restaurée ; la vidéo et sa position par `VideoPlaybackState` (PS-52).
- **Immersif** : barres système masquées dans la fenêtre du dialogue, rappelées par un glissement depuis le bord. Commandes effacées après 3 s de lecture, sauf si l'accessibilité demande plus de temps (`calculateRecommendedTimeoutMillis`, lu avant la fenêtre du dialogue qui fournit ses propres réglages de plateforme).

## Alternatives envisagées
- **Activité dédiée sans verrou d'orientation** : orientation libre par le manifeste, mais il faudrait passer le lecteur (ou sa position) d'une activité à l'autre et en recréer un.
- **`configChanges="orientation|screenSize"` sur `MainActivity`** : évite la recréation à la rotation, mais toute l'application devrait gérer elle-même ses changements de configuration.
- **`SCREEN_ORIENTATION_SENSOR`** : pivoterait même avec la rotation automatique désactivée, contre le choix de l'utilisateur.

## Conséquences
- Une rotation pendant le plein écran recrée l'activité et le lecteur ; la lecture reprend à sa position.
- Sur grand écran, Android 16+ ignore déjà le verrou en portrait : rien ne change.
