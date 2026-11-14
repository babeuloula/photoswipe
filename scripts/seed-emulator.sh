#!/usr/bin/env bash
# Pousse sur l'émulateur (ou le téléphone) des photos JPEG datées par leur EXIF, puis lance un
# scan de MediaStore (SPEC §10). Contrairement au seeder des outils développeur, ces photos
# n'appartiennent pas à PhotoSwipe : les dialogues système de corbeille et de favori s'affichent.
#
# Usage :
#   scripts/seed-emulator.sh            # une photo par année sur 5 ans, aujourd'hui et les 6 jours précédents,
#                                       # des rafales (aujourd'hui, J-4), une seule photo à J-2 et
#                                       # deux photos animées (Google aujourd'hui, Samsung à J-1)
#   scripts/seed-emulator.sh --years 3  # options passées à seed_photos.py (--years, --days)
#   scripts/seed-emulator.sh --remove   # retire les photos poussées
#
# Prérequis : adb (SDK Android), python3 et ffmpeg. Avec plusieurs appareils, choisir la cible
# par la variable ANDROID_SERIAL (par exemple ANDROID_SERIAL=emulator-5554).
set -euo pipefail

readonly DEVICE_FOLDER="/sdcard/Pictures/PhotoSwipeAdb"
SCRIPTS="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly SCRIPTS

require() {
    command -v "$1" >/dev/null 2>&1 || {
        echo "Missing command: $1 ($2)" >&2
        exit 1
    }
}

# Fait relire le dossier par MediaStore. `scan_volume` n'existe qu'à partir d'Android 11 : sur
# Android 10, chaque fichier poussé est indexé par la diffusion MEDIA_SCANNER_SCAN_FILE, et les
# photos retirées sont effacées de MediaStore par leur dossier (PS-181).
scan_media() {
    if adb shell content call --uri content://media --method scan_volume --arg external_primary 2>&1 |
        grep -q "Exception"; then
        adb shell content delete --uri content://media/external/images/media \
            --where "relative_path=\'${DEVICE_FOLDER#/sdcard/}/\'" >/dev/null 2>&1 || true
        adb shell ls "$DEVICE_FOLDER" 2>/dev/null | tr -d '\r' | while read -r file; do
            adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
                -d "file://$DEVICE_FOLDER/$file" </dev/null >/dev/null
        done
    fi
}

require adb "Android SDK platform-tools"
adb get-state >/dev/null

if [[ "${1:-}" == "--remove" ]]; then
    adb shell rm -rf "$DEVICE_FOLDER"
    scan_media
    echo "Removed $DEVICE_FOLDER"
    exit 0
fi

require python3 "https://www.python.org"
require ffmpeg "brew install ffmpeg"

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

count="$(python3 "$SCRIPTS/seed_photos.py" "$work" "$@")"
adb shell mkdir -p "$DEVICE_FOLDER"
adb push "$work/." "$DEVICE_FOLDER/" >/dev/null
scan_media
echo "Pushed $count dated photos to $DEVICE_FOLDER"
