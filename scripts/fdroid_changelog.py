"""Notes de version F-Droid tirées des notes de release générées par GitHub (PS-129).

Lit sur l'entrée standard le Markdown de GitHub (« What's Changed », une ligne par PR au format
Conventional Commits) et écrit sur la sortie un texte simple pour
fastlane/metadata/android/en-US/changelogs/default.txt : une puce par PR feat ou fix, sans
préfixe ni numéro de ticket, 500 caractères au plus (limite de F-Droid).

Usage : gh api … --jq .body | python3 scripts/fdroid_changelog.py
"""

import re
import sys

MAX_LENGTH = 500
FALLBACK = "Bug fixes and improvements."
MORE = "• And more: see the release notes on GitHub."

# « * feat(app): open the photo (PS-117) by @someone in https://… »
_PR_LINE = re.compile(
    r"^\*\s+(?P<type>\w+)(?:\([^)]*\))?!?:\s*(?P<title>.+?)"
    r"(?:\s+\(PS-\d+\))?\s+by\s+@\S+\s+in\s+\S+\s*$"
)
_USER_FACING = {"feat", "fix"}


def changelog(notes: str) -> str:
    """Texte des notes de version F-Droid pour les notes de release [notes] de GitHub."""
    rows = []
    for line in notes.splitlines():
        match = _PR_LINE.match(line.strip())
        if match and match["type"] in _USER_FACING:
            title = match["title"]
            rows.append("• " + title[0].upper() + title[1:])
    if not rows:
        return FALLBACK + "\n"
    return _fit(rows)


def _fit(rows: list) -> str:
    """Garde des lignes entières tant que le texte tient dans [MAX_LENGTH], puis renvoie à GitHub."""
    text = "\n".join(rows) + "\n"
    if len(text) <= MAX_LENGTH:
        return text
    kept = []
    for row in rows:
        candidate = "\n".join(kept + [row, MORE]) + "\n"
        if len(candidate) > MAX_LENGTH:
            break
        kept.append(row)
    return "\n".join(kept + [MORE]) + "\n"


if __name__ == "__main__":
    sys.stdout.write(changelog(sys.stdin.read()))
