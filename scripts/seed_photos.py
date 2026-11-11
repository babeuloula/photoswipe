#!/usr/bin/env python3
"""Génère des photos JPEG datées par leur EXIF pour le script seed-emulator.sh (SPEC §10).

Une photo par année sur YEARS_BACK ans, pour aujourd'hui et les 6 jours précédents, dans le fuseau
de l'ordinateur, avec de quoi essayer les retours d'utilisateurs du 2026-11-11 :
- aujourd'hui et il y a BURST_DAYS[1] jours, une rafale de BURST_SIZE photos prises à BURST_GAP
  secondes d'intervalle il y a BURST_YEARS_AGO ans (photos proches, PS-140 ; vue d'ensemble, PS-139) ;
- il y a SINGLE_PHOTO_DAY jours, une seule photo, de l'an dernier : la supprimer vide le jour (PS-136).
Les images sont de vraies photos (scripts/fixtures/photos, photos de Jérémy, PS-145) prises tour à tour :
portrait, paysage, carré et panorama ; chaque photo d'une rafale est la même image un peu plus
recadrée. ffmpeg les réencode (sans leurs métadonnées d'origine) ; l'EXIF (DateTimeOriginal et son décalage horaire, que MediaStore lit pour remplir DATE_TAKEN) est
écrit ici, sans dépendance hors de la bibliothèque standard.

Usage : seed_photos.py DOSSIER [--years N] [--days N]
"""

import argparse
import datetime
import pathlib
import struct
import subprocess
import sys

YEARS_BACK = 5
WINDOW_DAYS = 7
FIRST_PHOTO_HOUR = 9
BURST_DAYS = (0, 4)
BURST_YEARS_AGO = 2
BURST_SIZE = 3
BURST_GAP = 4
BURST_HOUR = 15
SINGLE_PHOTO_DAY = 2
FIXTURES = pathlib.Path(__file__).resolve().parent / "fixtures" / "photos"
BURST_ZOOM = 0.06

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
        if days_ago == SINGLE_PHOTO_DAY:
            taken.append(local_time(minus_years(day, 1), FIRST_PHOTO_HOUR + 1))
            continue
        for years_ago in range(1, years + 1):
            taken.append(local_time(minus_years(day, years_ago), FIRST_PHOTO_HOUR + years_ago))
        if days_ago in BURST_DAYS:
            start = local_time(minus_years(day, BURST_YEARS_AGO), BURST_HOUR)
            taken.extend(start + datetime.timedelta(seconds=BURST_GAP * shot) for shot in range(BURST_SIZE))
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
    return taken.strftime("photoswipe-adb-%Y-%m-%d-%H%M%S.jpg")


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


def fixtures():
    """Photos de test, dans l'ordre de leur nom."""
    return sorted(FIXTURES.glob("*.jpg"))


def assign(taken, photos):
    """Photo de chaque prise de vue : tour à tour, et la même pour toutes les photos d'une rafale."""
    assigned = []
    index = -1
    previous = None
    for moment in taken:
        same_burst = previous is not None and datetime.timedelta(0) < moment - previous <= datetime.timedelta(seconds=BURST_GAP)
        if not same_burst:
            index += 1
        assigned.append(photos[index % len(photos)])
        previous = moment
    return assigned


def crop_filter(shot):
    """Filtre ffmpeg de la photo [shot] d'une rafale : la première entière, les suivantes un peu recadrées."""
    keep = 1 - BURST_ZOOM * shot
    return f"crop=iw*{keep:.2f}:ih*{keep:.2f}"


def photo_jpeg(path, shot):
    """JPEG de [path] réencodé par ffmpeg, recadré pour la photo [shot] d'une rafale."""
    return subprocess.run(
        [
            "ffmpeg", "-loglevel", "error", "-i", str(path), "-vf", crop_filter(shot),
            "-map_metadata", "-1", "-frames:v", "1", "-f", "image2", "-c:v", "mjpeg", "-q:v", "4", "-",
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
    for taken, source in zip(photos, assign(photos, fixtures())):
        jpeg = photo_jpeg(source, taken.second // BURST_GAP)
        (options.folder / file_name(taken)).write_bytes(with_exif(jpeg, taken))
    print(len(photos))


if __name__ == "__main__":
    main(sys.argv[1:])
