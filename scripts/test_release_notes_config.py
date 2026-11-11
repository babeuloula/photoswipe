"""Tests de la configuration des notes de version : python3 -m unittest discover -s scripts"""

import pathlib
import re
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent


class ReleaseNotesConfigTest(unittest.TestCase):
    """La PR de release, mergée après son tag, ne revient pas dans les notes de la release suivante (PS-135)."""

    def test_leaves_the_release_label_out_of_the_generated_notes(self):
        config = (ROOT / ".github" / "release.yml").read_text(encoding="utf-8")

        self.assertRegex(config, r"(?s)changelog:\s*\n\s*exclude:\s*\n\s*labels:\s*\n\s*-\s*release\b")

    def test_labels_every_release_pull_request_it_opens(self):
        workflow = (ROOT / ".github" / "workflows" / "release.yml").read_text(encoding="utf-8")
        creations = re.findall(r"gh pr create(?:[^\n]*\\\n)*[^\n]*", workflow)

        self.assertGreaterEqual(len(creations), 2)
        for creation in creations:
            self.assertIn("--label release", creation)


if __name__ == "__main__":
    unittest.main()
