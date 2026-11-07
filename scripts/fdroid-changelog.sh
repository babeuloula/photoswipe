#!/usr/bin/env bash
# Écrit les notes de version F-Droid de la prochaine release (PS-129) à partir des notes que GitHub
# générera pour elle : fastlane/metadata/android/en-US/changelogs/default.txt, lu par F-Droid dans
# le commit qu'il compile (les autres langues reprennent l'anglais).
#
# À lancer AVANT de créer la release, puis commiter le fichier sur main : la release doit pointer
# sur ce commit. Dans l'interface de GitHub, « Generate release notes » donne le même contenu.
#
# Usage :
#   scripts/fdroid-changelog.sh v1.2.0          # notes depuis le dernier tag jusqu'à main
#   scripts/fdroid-changelog.sh v1.2.0 <ref>    # jusqu'à une autre branche ou un autre commit
#
# Prérequis : gh (authentifié) et python3.
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
    echo "Usage: $0 vX.Y.Z [target]" >&2
    exit 1
fi

readonly TAG="$1"
readonly TARGET="${2:-main}"
SCRIPTS="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPTS
readonly OUTPUT="$SCRIPTS/../fastlane/metadata/android/en-US/changelogs/default.txt"

# Depuis le dernier tag officiel : les tags -dev (pré-versions de debug, PS-130) ne comptent pas.
PREVIOUS="$(git -C "$SCRIPTS/.." tag --list "v*" --sort=-v:refname | grep -v -- "-dev" | grep -vx "$TAG" | head -1 || true)"
readonly PREVIOUS
args=(-f tag_name="$TAG" -f target_commitish="$TARGET")
if [[ -n "$PREVIOUS" ]]; then
    args+=(-f previous_tag_name="$PREVIOUS")
fi

mkdir -p "$(dirname "$OUTPUT")"
gh api "repos/{owner}/{repo}/releases/generate-notes" "${args[@]}" --jq .body |
    python3 "$SCRIPTS/fdroid_changelog.py" >"$OUTPUT"

echo "Wrote $OUTPUT:"
cat "$OUTPUT"
