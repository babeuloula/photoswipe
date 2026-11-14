"""Tests de play_whatsnew.py : python3 -m unittest discover -s scripts"""

import pathlib
import tempfile
import unittest

import play_whatsnew


class WhatsNewTest(unittest.TestCase):
    def test_keeps_short_notes_as_they_are(self):
        notes = "• Open the top photo full screen\n• Send the reminder right on time\n"

        self.assertEqual(
            "• Open the top photo full screen\n• Send the reminder right on time",
            play_whatsnew.whats_new(notes),
        )

    def test_cuts_long_notes_on_whole_lines_within_the_play_store_limit(self):
        lines = [f"• Change number {n} with a sentence long enough to fill the notes" for n in range(20)]

        notes = play_whatsnew.whats_new("\n".join(lines))

        self.assertLessEqual(len(notes), play_whatsnew.MAX_LENGTH)
        self.assertTrue(notes.startswith(lines[0]))
        for line in notes.splitlines():
            self.assertIn(line, lines)

    def test_falls_back_on_a_generic_sentence_for_empty_notes(self):
        self.assertEqual(play_whatsnew.FALLBACK, play_whatsnew.whats_new("\n \n"))

    def test_writes_the_notes_for_english_and_french(self):
        with tempfile.TemporaryDirectory() as tmp:
            changelog = pathlib.Path(tmp, "default.txt")
            changelog.write_text("• Fix the video card\n", encoding="utf-8")
            out = pathlib.Path(tmp, "whatsnew")

            play_whatsnew.write(changelog, out)

            self.assertEqual(
                ["whatsnew-en-US", "whatsnew-fr-FR"],
                sorted(path.name for path in out.iterdir()),
            )
            self.assertEqual("• Fix the video card", (out / "whatsnew-fr-FR").read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main()
