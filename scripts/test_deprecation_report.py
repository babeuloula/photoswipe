"""Tests de deprecation_report.py : python3 -m unittest discover -s scripts"""

import unittest

import deprecation_report

ROOT = "/home/runner/work/photoswipe/photoswipe"

LOG = f"""> Configure project :app
The ReportingExtension.file(String) method has been deprecated. This is scheduled to be removed in Gradle 10. Please use the getBaseDirectory().file(String) method instead. Consult the upgrading guide for further information: https://docs.gradle.org/9.8.2/userguide/upgrading_version_9.html#reporting_extension_file
The Configuration.setVisible(boolean) method has been deprecated. This is scheduled to be removed in Gradle 11. Consult the upgrading guide for further information: https://docs.gradle.org/9.8.2/userguide/upgrading_version_9.html#deprecate-visible-property
The Configuration.setVisible(boolean) method has been deprecated. This is scheduled to be removed in Gradle 11. Consult the upgrading guide for further information: https://docs.gradle.org/9.8.2/userguide/upgrading_version_9.html#deprecate-visible-property
> Task :app:compileDebugKotlin
w: file://{ROOT}/app/src/main/kotlin/fr/babeuloula/photoswipe/ui/common/WindowLayout.kt:46:21 'fun currentWindowAdaptiveInfo(): WindowAdaptiveInfo' is deprecated. Please use V2 version of this function.
w: file://{ROOT}/app/src/test/kotlin/fr/babeuloula/photoswipe/ui/deck/DeckViewModelTest.kt:380:36 This declaration needs opt-in. Its usage should be marked with '@kotlinx.coroutines.ExperimentalCoroutinesApi'
> Task :data:compileReleaseKotlin
Problem found: Kotlin compiler: DEPRECATION (id: kotlin:compiler:warning:DEPRECATION)
  Kotlin compiler warning
    'val foo: Int' is deprecated. Use bar.
    Location: {ROOT}/data/src/main/kotlin/fr/babeuloula/photoswipe/data/Foo.kt:12
BUILD SUCCESSFUL in 2m
"""


class ParseTest(unittest.TestCase):
    def test_finds_kotlin_deprecations_with_their_file_and_line(self):
        report = deprecation_report.parse(LOG, ROOT)

        self.assertEqual(
            [
                deprecation_report.Code(
                    "app/src/main/kotlin/fr/babeuloula/photoswipe/ui/common/WindowLayout.kt",
                    46,
                    "'fun currentWindowAdaptiveInfo(): WindowAdaptiveInfo' is deprecated. Please use V2 version of this function.",
                ),
                deprecation_report.Code(
                    "data/src/main/kotlin/fr/babeuloula/photoswipe/data/Foo.kt",
                    12,
                    "'val foo: Int' is deprecated. Use bar.",
                ),
            ],
            report.code,
        )

    def test_ignores_warnings_that_are_not_deprecations(self):
        report = deprecation_report.parse(LOG, ROOT)

        self.assertFalse(any("opt-in" in item.message for item in report.code))

    def test_lists_each_gradle_deprecation_once(self):
        report = deprecation_report.parse(LOG, ROOT)

        self.assertEqual(2, len(report.gradle))
        self.assertTrue(report.gradle[0].startswith("The ReportingExtension.file(String) method has been deprecated."))

    def test_keeps_the_gradle_summary_when_details_are_hidden(self):
        log = "Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.\n"

        self.assertEqual(1, len(deprecation_report.parse(log, ROOT).gradle))

    def test_finds_nothing_in_a_clean_build(self):
        report = deprecation_report.parse("> Task :app:assembleDebug\nBUILD SUCCESSFUL in 1m\n", ROOT)

        self.assertTrue(report.empty)


class OutputTest(unittest.TestCase):
    def test_writes_a_markdown_comment_with_its_marker(self):
        markdown = deprecation_report.markdown(deprecation_report.parse(LOG, ROOT))

        self.assertTrue(markdown.startswith(deprecation_report.MARKER))
        self.assertIn("`app/src/main/kotlin/fr/babeuloula/photoswipe/ui/common/WindowLayout.kt:46`", markdown)
        self.assertIn("4 dépréciations", markdown)

    def test_says_when_everything_is_fixed(self):
        markdown = deprecation_report.markdown(deprecation_report.parse("", ROOT))

        self.assertIn("Plus aucune dépréciation", markdown)

    def test_annotates_each_kotlin_line_for_github(self):
        annotations = deprecation_report.annotations(deprecation_report.parse(LOG, ROOT))

        self.assertIn(
            "::warning file=app/src/main/kotlin/fr/babeuloula/photoswipe/ui/common/WindowLayout.kt,line=46,"
            "title=Deprecation::'fun currentWindowAdaptiveInfo(): WindowAdaptiveInfo' is deprecated. "
            "Please use V2 version of this function.",
            annotations,
        )
        self.assertEqual(4, len(annotations))


if __name__ == "__main__":
    unittest.main()
