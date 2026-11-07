# 0032 — Release officielle par un workflow lancé à la main

- **Statut** : accepté
- **Date** : 2026-11-07
- **Tickets** : PS-130 (après PS-115, PS-128, PS-129), PS-133

## Contexte
F-Droid compile le commit du tag et y lit les notes de version (`fastlane/…/changelogs/default.txt`) : elles doivent donc être dans le commit tagué, écrites avant la release. Jérémy crée ses releases depuis GitHub et veut un seul geste, sans risque de publier une release officielle par erreur. `main` est protégée (PR et check obligatoire), et une PR ouverte par le jeton `GITHUB_TOKEN` ne déclenche aucun workflow.

## Décision
- **Release officielle = workflow lancé à la main** (`workflow_dispatch`, version `vX.Y.Z`, depuis `main`). Il crée en local la branche `release/vX.Y.Z` avec le commit des notes F-Droid, lance tests, qualité et build signé, puis seulement s'il a réussi : pousse le tag sur ce commit, crée la release (notes générées depuis le dernier tag officiel, AAB, APK, `version.properties`, « latest »), puis pousse la branche et ouvre la PR vers `main`. Une PR impossible à ouvrir donne un avertissement, pas un échec : la release est l'essentiel, la PR s'ouvre alors à la main.
- **La PR se merge en merge commit** : le commit tagué entre dans l'historique de `main`, et `git describe` y retrouve le tag.
- **Pas de tests relancés, mais une garde** (PS-133) : le workflow ne part que si le run « CI » du commit de `main` est terminé et vert. Le commit de release n'ajoute qu'un fichier texte à ce commit déjà testé.
- **Un jeton personnel à grain fin** (`PHOTOSWIPE_RELEASE_TOKEN`, PS-133, remplace le choix initial de s'en passer) ouvre la PR : une PR ouverte par `GITHUB_TOKEN` ne lance pas la CI et ne pourrait jamais satisfaire le check obligatoire. Avec ce jeton, la vraie CI tourne sur la PR et l'auto-merge la merge en merge commit une fois verte, sans aucun geste. Sans le jeton, la PR s'ouvre quand même et se merge à la main (contournement admin).
- **Tag poussé à la main : seulement `vX.Y.Z-dev.N`** (le workflow écoute tous les tags, même sans `v`). Il produit une pré-version (jamais « latest », donc jamais lue par F-Droid) avec l'APK **debug**, outils développeur compris. Tout autre tag poussé à la main est supprimé avec sa release et le workflow échoue. Les tags poussés par le workflow ne déclenchent pas d'événement `push`, donc pas ce contrôle.
- **Les tags `-dev` ne nomment pas de version** : `git describe --exclude "v*-dev*"` dans `app/build.gradle.kts`, et les notes partent du dernier tag officiel.

## Conséquences
- Réglages du dépôt requis : « Allow auto-merge » ; « Allow GitHub Actions to create and approve pull requests » pour le cas sans jeton.
- Le jeton expire : le renouveler (et mettre à jour le secret) avant, sinon la PR repasse au merge à la main.
- Les branches `release/v*` sont protégées contre la suppression, sans contournement (ruleset « release branches: no deletion ») : elles restent après le merge de leur PR.
- Une release ratée avant la publication ne laisse ni tag, ni branche, ni release ; une release réussie dont la PR n'est pas ouverte ou pas mergée reste valable, seuls les noms des builds debug suivants restent sur l'ancienne version.
- Un événement `push` lance le workflow du commit tagué : un tag posé à la main sur un commit d'avant PS-130 lance l'ancien workflow, qui publiait tout tag `v*`. Le contrôle ne vaut que pour les commits qui contiennent ce workflow.
- La clé de debug (`app/debug.keystore`) est publique depuis que le dépôt l'est : n'importe qui peut signer un APK qu'un téléphone ayant une version debug acceptera en mise à jour. Les APK debug des pré-versions sont destinés aux tests, pas à un usage quotidien.
- Tant que la CI n'a pas de crédits, la procédure se fait à la main (`docs/store/README.md`).
