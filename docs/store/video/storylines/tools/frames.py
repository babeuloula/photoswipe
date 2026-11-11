#!/usr/bin/env python3
"""Contrôles d'une prise brute de screenrecord.

  frames.py motion PRISE.mp4               instants où l'image se met à bouger (recaler timings.json)
  frames.py smooth PRISE.mp4 JOURNAL.json  images enregistrées dans les 0,7 s de chaque glissement

screenrecord n'enregistre une image que quand l'écran change : une animation fluide donne ~20 à 30
images en 0,7 s ; moins de 10, la prise saccade (émulateur chargé) et se refait.
"""
import json
import subprocess
import sys


def motion(path):
    w, h = 54, 120
    n = w * h
    raw = subprocess.run(["ffmpeg", "-loglevel", "error", "-i", path, "-vf", f"fps=30,scale={w}:{h},format=gray",
                          "-f", "rawvideo", "-"], capture_output=True).stdout
    frames = [raw[i:i + n] for i in range(0, len(raw), n)]
    last = -9.0
    for i in range(1, len(frames)):
        diff = sum(abs(a - b) for a, b in zip(frames[i - 1], frames[i])) / n
        t = i / 30
        if diff > 1.5 and t - last > 0.6:
            print(f"{t:.2f}")
        if diff > 1.5:
            last = t


def smooth(path, log):
    out = subprocess.run(["ffprobe", "-v", "error", "-select_streams", "v", "-show_entries", "packet=pts_time",
                          "-of", "csv=p=0", path], capture_output=True, text=True).stdout
    pts = sorted(float(x.strip().rstrip(",")) for x in out.split() if x.strip())
    with open(log) as f:
        events = json.load(f)
    for e in events:
        if e["kind"] == "swipe":
            count = sum(1 for p in pts if e["t"] <= p < e["t"] + 0.7)
            print(f'{e["name"]:16} {count:3d} images / 0,7 s{"  <- saccade" if count < 10 else ""}')


if __name__ == "__main__":
    if sys.argv[1] == "motion":
        motion(sys.argv[2])
    else:
        smooth(sys.argv[2], sys.argv[3])
