#!/usr/bin/env python3
"""Génère des photos JPEG datées par leur EXIF pour le script seed-emulator.sh (SPEC §10).

Une photo par année sur YEARS_BACK ans, pour aujourd'hui et les 6 jours précédents, dans le fuseau
de l'ordinateur. Les pixels (une couleur par année) viennent de ffmpeg ; l'EXIF (DateTimeOriginal
et son décalage horaire, que MediaStore lit pour remplir DATE_TAKEN) est écrit ici, sans
dépendance hors de la bibliothèque standard.

Usage : seed_photos.py DOSSIER [--years N] [--days N]
"""

import argparse
import colorsys
import datetime
import pathlib
import struct
import subprocess
import sys

YEARS_BACK = 5
WINDOW_DAYS = 7
FIRST_PHOTO_HOUR = 9
WIDTH, HEIGHT = 900, 1200

TAG_EXIF_IFD = 0x8769
TAG_DATETIME_ORIGINAL = 0x9003
TAG_OFFSET_TIME_ORIGINAL = 0x9011
TYPE_ASCII = 2
TYPE_LONG = 4


def plan(today, years=YEARS_BACK, days=WINDOW_DAYS):
    """Dates de prise de vue (avec fuseau local) des photos à générer."""
    taken = []
    for days_ago in range(days):
        day = today - datetime.timedelta(days=days_ago)
        for years_ago in range(1, years + 1):
            taken.append(local_time(minus_years(day, years_ago), FIRST_PHOTO_HOUR + years_ago))
    return taken


def minus_years(day, years):
    """Même jour [years] ans plus tôt ; un 29 février devient le 28 février d'une année non bissextile."""
    try:
        return day.replace(year=day.year - years)
    except ValueError:
        return day.replace(year=day.year - years, day=28)


def local_time(day, hour):
    """[day] à [hour]:00 dans le fuseau de l'ordinateur, avec le décalage horaire de cette date."""
    return datetime.datetime(day.year, day.month, day.day, hour).astimezone()


def file_name(taken):
    return taken.strftime("photoswipe-adb-%Y-%m-%d-%H%M.jpg")


def exif_segment(taken):
    """Segment APP1 Exif (TIFF gros-boutiste) avec DateTimeOriginal et OffsetTimeOriginal."""
    date = taken.strftime("%Y:%m:%d %H:%M:%S").encode("ascii") + b"\0"
    offset = taken.strftime("%z")
    offset = (offset[:3] + ":" + offset[3:]).encode("ascii") + b"\0"
    ifd0_offset = 8
    exif_ifd_offset = ifd0_offset + 2 + 12 + 4
    data_offset = exif_ifd_offset + 2 + 2 * 12 + 4
    tiff = b"MM" + struct.pack(">HI", 42, ifd0_offset)
    tiff += struct.pack(">H", 1) + struct.pack(">HHII", TAG_EXIF_IFD, TYPE_LONG, 1, exif_ifd_offset) + struct.pack(">I", 0)
    tiff += struct.pack(">H", 2)
    tiff += struct.pack(">HHII", TAG_DATETIME_ORIGINAL, TYPE_ASCII, len(date), data_offset)
    tiff += struct.pack(">HHII", TAG_OFFSET_TIME_ORIGINAL, TYPE_ASCII, len(offset), data_offset + len(date))
    tiff += struct.pack(">I", 0) + date + offset
    payload = b"Exif\0\0" + tiff
    return b"\xff\xe1" + struct.pack(">H", len(payload) + 2) + payload


def with_exif(jpeg, taken):
    """Copie de [jpeg] avec le segment Exif de [taken] juste après le marqueur de début d'image."""
    if jpeg[:2] != b"\xff\xd8":
        raise ValueError("not a JPEG file")
    return jpeg[:2] + exif_segment(taken) + jpeg[2:]


def exif_dates(jpeg):
    """Relit (DateTimeOriginal, OffsetTimeOriginal) d'un JPEG écrit par [with_exif]."""
    start = jpeg.index(b"Exif\0\0") + 6
    tiff = jpeg[start:]
    (ifd0,) = struct.unpack(">I", tiff[4:8])
    _, _, _, exif_ifd = struct.unpack(">HHII", tiff[ifd0 + 2:ifd0 + 14])
    (count,) = struct.unpack(">H", tiff[exif_ifd:exif_ifd + 2])
    values = {}
    for index in range(count):
        entry = exif_ifd + 2 + index * 12
        tag, _, length, offset = struct.unpack(">HHII", tiff[entry:entry + 12])
        values[tag] = tiff[offset:offset + length - 1].decode("ascii")
    return values[TAG_DATETIME_ORIGINAL], values[TAG_OFFSET_TIME_ORIGINAL]


def color_of(year):
    """Couleur propre à l'année, au format hexadécimal de ffmpeg."""
    red, green, blue = colorsys.hsv_to_rgb((year * 47 % 360) / 360, 0.55, 0.7)
    return "0x%02x%02x%02x" % (int(red * 255), int(green * 255), int(blue * 255))


def solid_jpeg(color):
    """JPEG uni de la couleur [color], produit par ffmpeg."""
    return subprocess.run(
        [
            "ffmpeg", "-loglevel", "error", "-f", "lavfi", "-i", f"color=c={color}:s={WIDTH}x{HEIGHT}",
            "-frames:v", "1", "-f", "image2", "-c:v", "mjpeg", "-",
        ],
        check=True,
        capture_output=True,
    ).stdout


def main(arguments):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("folder", type=pathlib.Path)
    parser.add_argument("--years", type=int, default=YEARS_BACK)
    parser.add_argument("--days", type=int, default=WINDOW_DAYS)
    options = parser.parse_args(arguments)
    options.folder.mkdir(parents=True, exist_ok=True)
    photos = plan(datetime.date.today(), options.years, options.days)
    for taken in photos:
        (options.folder / file_name(taken)).write_bytes(with_exif(solid_jpeg(color_of(taken.year)), taken))
    print(len(photos))


if __name__ == "__main__":
    main(sys.argv[1:])
