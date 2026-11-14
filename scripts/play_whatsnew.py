"""Notes de version du Play Store tirées des notes F-Droid (PS-152).

Lit fastlane/metadata/android/en-US/changelogs/default.txt, écrit par scripts/fdroid-changelog.sh,
et écrit dans un dossier les fichiers whatsnew-<langue> que l'action upload-google-play envoie avec
l'AAB : en-US et fr-FR, le français reprenant l'anglais comme sur F-Droid. Le Play Store limite ces
notes à 500 caractères par langue : le texte est coupé en lignes entières au besoin.

Usage : python3 scripts/play_whatsnew.py <default.txt> <dossier>
"""

import pathlib
import sys

MAX_LENGTH = 500
LANGUAGES = ("en-US", "fr-FR")
FALLBACK = "Bug fixes and improvements."


def whats_new(text: str) -> str:
    """Notes du Play Store pour les notes F-Droid [text] : lignes entières, [MAX_LENGTH] au plus."""
    lines = [line.rstrip() for line in text.strip().splitlines() if line.strip()]
    kept = []
    for line in lines:
        if len("\n".join(kept + [line])) > MAX_LENGTH:
            break
        kept.append(line)
    return "\n".join(kept) or FALLBACK


def write(changelog: pathlib.Path, directory: pathlib.Path) -> None:
    """Écrit dans [directory] un fichier whatsnew-<langue> par langue du Play Store."""
    notes = whats_new(changelog.read_text(encoding="utf-8"))
    directory.mkdir(parents=True, exist_ok=True)
    for language in LANGUAGES:
        (directory / f"whatsnew-{language}").write_text(notes, encoding="utf-8")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("Usage: play_whatsnew.py <default.txt> <directory>")
    write(pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2]))
