#!/usr/bin/env python3
"""Tourne la prise continue de la vidéo A sur l'émulateur, en temps réel, et journalise chaque geste.

Écran d'accueil d'Android → notification du rappel (qui tombe à HH:MM) → volet → appui sur la
notification → tri des 8 souvenirs (garder, doublon supprimé, favori par la feuille « Plus
d'actions », garder…) → résumé → dialogue de la corbeille, annulé.

Avant : rappel activé et réglé à HH:MM dans l'app (reminder_time.py), aucune notification dans le
volet, base de PhotoSwipe sans décision sur les photos du tournage, émulateur au repos.
Une prise saccade si l'émulateur est chargé : vérifier avec frames.py --smooth.

Usage : python3 take.py JOURNAL.json HH:MM [--no-cancel]
  --no-cancel : ne pas toucher le dialogue (position de « Annuler » à vérifier dans une autre langue).
La vidéo est sur l'appareil dans /sdcard/take.mp4 (adb pull).
"""
import datetime
import hashlib
import json
import subprocess
import sys
import time

log = []
T0 = None


def sh(*args):
    subprocess.run(["adb", "shell", *args])


def mark(kind, **kw):
    log.append(dict(t=round(time.monotonic() - T0, 3), kind=kind, **kw))


def tap(name, x, y):
    mark("tap", name=name, x=x, y=y)
    sh("input", "tap", str(x), str(y))


def swipe(name, x1, y1, x2, y2, ms):
    mark("swipe", name=name, x1=x1, y1=y1, x2=x2, y2=y2, ms=ms)
    sh("input", "swipe", *map(str, (x1, y1, x2, y2, ms)))


def region(y0, y1, x0, x1):
    raw = subprocess.run(["adb", "exec-out", "screencap"], capture_output=True).stdout
    return hashlib.md5(b"".join(raw[16 + (y * 1080 + x0) * 4:16 + (y * 1080 + x1) * 4] for y in range(y0, y1, 4))).hexdigest()


def wait_change(box, base, limit=4.0):
    end = time.monotonic() + limit
    while time.monotonic() < end and region(*box) == base:
        time.sleep(0.1)


def keep(name):
    swipe(name, 500, 1120, 1060, 1060, 560)


def drop(name):
    swipe(name, 580, 1120, 20, 1070, 560)


def main(out, alarm, cancel):
    global T0
    sh("input", "keyevent", "HOME")
    h, m = map(int, alarm.split(":"))
    start = datetime.datetime.now().replace(hour=h, minute=m, second=0, microsecond=0) - datetime.timedelta(seconds=6)
    time.sleep(max(0, (start - datetime.datetime.now()).total_seconds()))
    rec = subprocess.Popen(["adb", "shell", "screenrecord", "--bit-rate", "12000000", "/sdcard/take.mp4"])
    T0 = time.monotonic()
    mark("rec")
    while "fr.babeuloula.photoswipe" not in subprocess.run(
            ["adb", "shell", "cmd", "notification", "list"], capture_output=True, text=True).stdout:
        time.sleep(0.3)
    mark("notif")
    time.sleep(1.6)
    swipe("shade", 540, 20, 560, 1350, 520)
    time.sleep(1.9)
    tap("notif", 500, 750)
    time.sleep(3.4)
    keep("keep-2025")
    time.sleep(2.0)
    drop("delete-2025-dup")
    time.sleep(2.0)
    sheet = (2030, 2080, 330, 520)  # libellé « Ajouter aux favoris » de la feuille
    base = region(*sheet)
    swipe("up-2024", 540, 1300, 560, 520, 600)
    wait_change(sheet, base)
    time.sleep(0.5)
    mark("sheet")
    time.sleep(1.0)
    tap("fav-2024", 325, 2052)
    time.sleep(2.0)
    for name in ("keep-2024b", "keep-2023", "keep-2022"):
        keep(name)
        time.sleep(1.8)
    drop("delete-2019")
    time.sleep(1.8)
    keep("keep-2017")
    time.sleep(3.4)
    tap("confirm", 540, 2052)
    time.sleep(3.2)
    if cancel:
        tap("cancel", 378, 1442)
        time.sleep(1.8)
    else:
        time.sleep(3.0)
    mark("stop")
    sh("pkill", "-INT", "screenrecord")
    rec.wait()
    with open(out, "w") as f:
        json.dump(log, f, indent=1)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], "--no-cancel" not in sys.argv)
