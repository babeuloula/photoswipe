"""Rapport des dépréciations d'un build Gradle, pour la CI (PS-132).

Lit le journal d'un build lancé avec --warning-mode all et relève :
- les dépréciations Kotlin (« … is deprecated »), avec leur fichier et leur ligne, aux deux formats
  du compilateur (ligne « w: file://… » et bloc « Problem found … Location: ») ;
- les dépréciations Gradle (« … has been deprecated », ou le résumé « Deprecated Gradle features »).

Écrit les annotations GitHub (avertissements jaunes) sur la sortie standard et, avec --markdown,
le commentaire de PR. Ne fait jamais échouer la CI : le rapport informe, il ne bloque pas.

Usage : python3 scripts/deprecation_report.py build.log --root "$GITHUB_WORKSPACE" --markdown report.md
"""

import argparse
import re
from dataclasses import dataclass, field

MARKER = "<!-- photoswipe-deprecations -->"

_KOTLIN_LINE = re.compile(r"^w: (?:file://)?(?P<path>/\S+?):(?P<line>\d+)(?::\d+)? (?P<message>.*\bis deprecated\b.*)$")
_PROBLEM = re.compile(r"^Problem found: .*\bDEPRECATION\b")
_LOCATION = re.compile(r"^Location: (?P<path>/\S+?):(?P<line>\d+)")
_GRADLE = re.compile(r"\bhas been deprecated\b|^Deprecated Gradle features were used in this build")


@dataclass(frozen=True)
class Code:
    """Dépréciation dans le code : fichier relatif à la racine du dépôt, ligne et message."""

    path: str
    line: int
    message: str


@dataclass
class Report:
    """Dépréciations relevées dans un journal, sans doublon, dans l'ordre d'apparition."""

    code: list = field(default_factory=list)
    gradle: list = field(default_factory=list)

    @property
    def empty(self) -> bool:
        return not self.code and not self.gradle

    @property
    def count(self) -> int:
        return len(self.code) + len(self.gradle)


def parse(log: str, root: str) -> Report:
    """Dépréciations du journal [log], chemins rendus relatifs à [root]."""
    report = Report()
    lines = [_strip_ansi(line).rstrip() for line in log.splitlines()]
    for index, line in enumerate(lines):
        kotlin = _KOTLIN_LINE.match(line)
        if kotlin:
            _add(report.code, Code(_relative(kotlin["path"], root), int(kotlin["line"]), kotlin["message"]))
        elif _PROBLEM.match(line):
            problem = _problem(lines[index + 1 : index + 6], root)
            if problem:
                _add(report.code, problem)
        elif _GRADLE.search(line):
            _add(report.gradle, line.strip())
    return report


def markdown(report: Report) -> str:
    """Commentaire de PR : la liste des dépréciations, ou la confirmation qu'il n'y en a plus."""
    if report.empty:
        return f"{MARKER}\n### ✅ Plus aucune dépréciation\n\nLe build de ce commit n'en signale aucune.\n"
    noun = "dépréciation" if report.count == 1 else "dépréciations"
    parts = [
        MARKER,
        f"### ⚠️ {report.count} {noun} à corriger un jour",
        "",
        "Rien de bloquant : la CI reste verte. Liste mise à jour à chaque push.",
    ]
    if report.code:
        parts += ["", "**Dans le code**", "", "| Où | Quoi |", "|---|---|"]
        parts += [f"| `{item.path}:{item.line}` | {_cell(item.message)} |" for item in report.code]
    if report.gradle:
        parts += ["", "**Gradle et ses plugins**", ""]
        parts += [f"- {message}" for message in report.gradle]
    return "\n".join(parts) + "\n"


def annotations(report: Report) -> list:
    """Avertissements GitHub Actions : sur la ligne pour le code, sur le run pour Gradle."""
    lines = [
        f"::warning file={item.path},line={item.line},title=Deprecation::{_escape(item.message)}"
        for item in report.code
    ]
    lines += [f"::warning title=Gradle deprecation::{_escape(message)}" for message in report.gradle]
    return lines


def _problem(following: list, root: str):
    """Bloc « Problem found » : le message est la ligne « is deprecated », le fichier dans « Location: »."""
    message = next((line.strip() for line in following if "is deprecated" in line), None)
    location = next((_LOCATION.match(line.strip()) for line in following if _LOCATION.match(line.strip())), None)
    if message is None or location is None:
        return None
    return Code(_relative(location["path"], root), int(location["line"]), message)


def _add(items: list, item) -> None:
    if item not in items:
        items.append(item)


def _relative(path: str, root: str) -> str:
    prefix = root.rstrip("/") + "/"
    return path[len(prefix) :] if path.startswith(prefix) else path


def _strip_ansi(line: str) -> str:
    return re.sub(r"\x1b\[[0-9;]*[A-Za-z]", "", line)


def _cell(text: str) -> str:
    return text.replace("|", "\\|")


def _escape(text: str) -> str:
    """Échappement des commandes de workflow GitHub."""
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("log", help="journal du build Gradle")
    parser.add_argument("--root", default=".", help="racine du dépôt, retirée des chemins")
    parser.add_argument("--markdown", help="fichier où écrire le commentaire de PR")
    args = parser.parse_args()
    with open(args.log, encoding="utf-8", errors="replace") as file:
        report = parse(file.read(), args.root)
    for line in annotations(report):
        print(line)
    if args.markdown:
        with open(args.markdown, "w", encoding="utf-8") as file:
            file.write(markdown(report))


if __name__ == "__main__":
    main()
