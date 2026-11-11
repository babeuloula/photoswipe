#!/usr/bin/env python3
"""Règle l'heure du rappel dans PhotoSwipe, écran des réglages ouvert, rappel activé.

Le cadran ne prend que les multiples de 5 minutes au toucher. Téléphone en français : cadran 24 h ;
en anglais (en-US) : cadran 12 h, l'après-midi (PM) étant déjà choisi pour un rappel à 18:00.

Usage : python3 reminder_time.py HH:MM [--12h]
"""
import math
import subprocess
import sys
import time


def tap(x, y):
    subprocess.run(["adb", "shell", "input", "tap", str(int(x)), str(int(y))])


def dial(cx, cy, r, value, step):
    a = math.radians(value * step)
    tap(cx + r * math.sin(a), cy - r * math.cos(a))


def main(hhmm, twelve):
    h, m = map(int, hhmm.split(":"))
    tap(250, 620)  # ligne « Heure du rappel »
    time.sleep(1.2)
    if twelve:
        cx, cy, ok = 538, 1330, (820, 1855)
        dial(cx, cy, 262, h % 12, 30)
    else:
        cx, cy, ok = 532, 1288, (802, 1812)
        dial(cx, cy, 180 if h == 0 or h > 12 else 265, h % 12, 30)  # 13-23 et 0 sur le cercle intérieur
    time.sleep(0.8)
    dial(cx, cy, 262 if twelve else 265, m, 6)
    time.sleep(0.6)
    tap(*ok)


if __name__ == "__main__":
    main(sys.argv[1], "--12h" in sys.argv)
