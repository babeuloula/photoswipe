#!/usr/bin/env python3
"""Pousse sur l'émulateur les photos du tournage, datées par EXIF autour d'aujourd'hui.

Aujourd'hui : 8 souvenirs de 2017 à 2025 (un doublon de l'an dernier à 6 s d'écart, une balade
d'il y a 2 ans à mettre en favori) ; les jours d'avant : quelques photos pour la liste « À rattraper ».
Les photos de Jérémy ne sont pas dans Git : passer le dossier qui les contient.

Usage : python3 seed_photos.py DOSSIER_DES_PHOTOS
"""
import datetime
import pathlib
import subprocess
import sys
import tempfile

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[5] / "scripts"))
from seed_photos import with_exif  # noqa: E402  (EXIF écrit par le script du seeder adb)

DEVICE_FOLDER = "/sdcard/DCIM/Camera"

# (jours avant aujourd'hui, années avant, heure, fichier)
PLAN = [
    (0, 1, "15:20:00", "P1130392_5f4f44a74bce3.jpg"),
    (0, 1, "15:20:06", "P1130393_5f4f44a3c5e48.jpg"),
    (0, 2, "10:30:00", "P1010463-Modifier_5a7ee1bb6e24f.jpg"),
    (0, 2, "10:52:00", "P1010464_5a7f1e9e1b014.jpg"),
    (0, 3, "16:05:00", "P1050652_5b94000c4995f.jpg"),
    (0, 4, "11:40:00", "_DSC3850_6213cf7e3dcde.jpg"),
    (0, 7, "18:10:00", "P1050388_5b93ff823df94.jpg"),
    (0, 9, "14:15:00", "telephone_5a7f6bb9d73c8.jpg"),
    (1, 3, "13:00:00", "P1050450_5b93ffada9e56.jpg"),
    (1, 3, "13:02:00", "P1050452_5b93ffb43c122.jpg"),
    (2, 8, "12:00:00", "P1010429_5a7eddeb13f2c.jpg"),
    (2, 8, "12:20:00", "P1010435_5a7eded1956b6.jpg"),
    (3, 4, "09:30:00", "_DSC3838_6213cf76d588f.jpg"),
    (4, 6, "17:00:00", "P1130345_5f4f44b460606.jpg"),
    (4, 5, "15:00:00", "P1050480_5b93ffbc626fe.jpg"),
    (5, 6, "19:00:00", "P1010845_5ad42bbf0d0df.jpg"),
]


def taken(today, days_ago, years_ago, hms):
    day = today - datetime.timedelta(days=days_ago)
    day = day.replace(year=day.year - years_ago)
    h, m, s = map(int, hms.split(":"))
    return datetime.datetime(day.year, day.month, day.day, h, m, s).astimezone()


def main(source):
    today = datetime.date.today()
    with tempfile.TemporaryDirectory() as work:
        for days_ago, years_ago, hms, name in PLAN:
            when = taken(today, days_ago, years_ago, hms)
            jpeg = subprocess.run(
                ["ffmpeg", "-loglevel", "error", "-i", str(pathlib.Path(source) / name), "-map_metadata", "-1",
                 "-frames:v", "1", "-f", "image2", "-c:v", "mjpeg", "-q:v", "3", "-"],
                check=True, capture_output=True).stdout
            (pathlib.Path(work) / when.strftime("IMG_%Y%m%d_%H%M%S.jpg")).write_bytes(with_exif(jpeg, when))
        subprocess.run(["adb", "shell", "mkdir", "-p", DEVICE_FOLDER], check=True)
        subprocess.run(["adb", "push", f"{work}/.", f"{DEVICE_FOLDER}/"], check=True, capture_output=True)
    subprocess.run(["adb", "shell", "content", "call", "--uri", "content://media", "--method", "scan_volume",
                    "--arg", "external_primary"], check=True, capture_output=True)
    print(f"{len(PLAN)} photos in {DEVICE_FOLDER}")


if __name__ == "__main__":
    main(sys.argv[1])
