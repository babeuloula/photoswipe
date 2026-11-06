"""Tests de seed_photos.py : python3 -m unittest discover -s scripts"""

import datetime
import unittest

import seed_photos

PARIS_SUMMER = datetime.timezone(datetime.timedelta(hours=2))
JPEG = b"\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01\x00\x00\x01\x00\x01\x00\x00\xff\xd9"


class PlanTest(unittest.TestCase):
    def test_plans_one_photo_per_previous_year_for_today_and_the_6_previous_days(self):
        today = datetime.date(2026, 11, 6)

        taken = seed_photos.plan(today)

        days = {(photo.month, photo.day) for photo in taken}
        expected_days = {((today - datetime.timedelta(days=n)).month, (today - datetime.timedelta(days=n)).day) for n in range(7)}
        self.assertEqual(expected_days, days)
        self.assertEqual(7 * 5, len(taken))
        self.assertEqual(list(range(2021, 2026)), sorted({photo.year for photo in taken}))

    def test_moves_leap_day_photos_to_february_28_on_non_leap_years(self):
        taken = seed_photos.plan(datetime.date(2028, 2, 29), years=1, days=1)

        self.assertEqual(datetime.date(2027, 2, 28), taken[0].date())

    def test_dates_every_photo_in_the_local_time_zone(self):
        taken = seed_photos.plan(datetime.date(2026, 11, 6))

        self.assertTrue(all(photo.tzinfo is not None for photo in taken))

    def test_names_every_file_after_its_date(self):
        taken = datetime.datetime(2021, 11, 6, 10, 0, tzinfo=PARIS_SUMMER)

        self.assertEqual("photoswipe-adb-2021-11-06-1000.jpg", seed_photos.file_name(taken))


class ExifTest(unittest.TestCase):
    def test_writes_the_date_taken_and_its_offset_into_the_jpeg(self):
        taken = datetime.datetime(2021, 7, 15, 18, 30, 5, tzinfo=PARIS_SUMMER)

        jpeg = seed_photos.with_exif(JPEG, taken)

        self.assertEqual(("2021:07:15 18:30:05", "+02:00"), seed_photos.exif_dates(jpeg))
        self.assertTrue(jpeg.startswith(b"\xff\xd8\xff\xe1"))
        self.assertTrue(jpeg.endswith(JPEG[2:]))

    def test_rejects_a_file_that_is_not_a_jpeg(self):
        with self.assertRaises(ValueError):
            seed_photos.with_exif(b"\x89PNG", datetime.datetime(2021, 7, 15, tzinfo=PARIS_SUMMER))

    def test_gives_each_year_its_own_color(self):
        self.assertNotEqual(seed_photos.color_of(2021), seed_photos.color_of(2022))
        self.assertRegex(seed_photos.color_of(2021), r"^0x[0-9a-f]{6}$")


if __name__ == "__main__":
    unittest.main()
