"""Tests de fdroid_changelog.py : python3 -m unittest discover -s scripts"""

import unittest

import fdroid_changelog

NOTES = """## What's Changed
* feat(app): open the top photo full screen with zoom (PS-117) by @babeuloula in https://github.com/babeuloula/photoswipe/pull/121
* build(app): prepare the F-Droid release with fastlane metadata and a version file (PS-128) by @babeuloula in https://github.com/babeuloula/photoswipe/pull/132
* fix(data): send the reminder right on time with exact alarms (PS-125) by @babeuloula in https://github.com/babeuloula/photoswipe/pull/130


**Full Changelog**: https://github.com/babeuloula/photoswipe/compare/v1.1.0...v1.1.1
"""


class ChangelogTest(unittest.TestCase):
    def test_keeps_features_and_fixes_as_plain_sentences(self):
        self.assertEqual(
            "• Open the top photo full screen with zoom\n• Send the reminder right on time with exact alarms\n",
            fdroid_changelog.changelog(NOTES),
        )

    def test_reads_a_breaking_change_without_scope(self):
        notes = "* feat!: drop Android 12 by @someone in https://example.org/pull/1\n"

        self.assertEqual("• Drop Android 12\n", fdroid_changelog.changelog(notes))

    def test_falls_back_on_a_generic_sentence_without_user_facing_change(self):
        notes = "* build(app): bump AGP (PS-1) by @babeuloula in https://example.org/pull/2\n"

        self.assertEqual(fdroid_changelog.FALLBACK + "\n", fdroid_changelog.changelog(notes))

    def test_stays_under_the_f_droid_limit_by_dropping_whole_lines(self):
        line = "* feat(app): " + "a" * 90 + " (PS-1) by @babeuloula in https://example.org/pull/3\n"

        text = fdroid_changelog.changelog(line * 10)

        self.assertLessEqual(len(text), fdroid_changelog.MAX_LENGTH)
        self.assertTrue(text.endswith(fdroid_changelog.MORE + "\n"))
        self.assertTrue(all(row.startswith("• ") for row in text.splitlines()[:-1]))


if __name__ == "__main__":
    unittest.main()
