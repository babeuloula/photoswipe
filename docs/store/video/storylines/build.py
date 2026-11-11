#!/usr/bin/env python3
"""Génère la vidéo de la fiche « Un jour, un rituel » (storyline A) en français (a/) et en anglais (a-en/).

Chaque langue a sa prise d'écran continue, tournée en temps réel sur l'émulateur
(<dossier>/assets/footage/take.mp4, 1080 × 2400, 30 i/s) et ses instants dans <dossier>/timings.json :
instant de chaque geste dans la vidéo (journal de tools/take.py, recalé image par image avec
tools/frames.py). Rien n'est accéléré : la prise joue à vitesse réelle, d'une traite, sans coupe.

Usage : python3 build.py            # écrit a/index.html et a-en/index.html
"""
import json
import pathlib
import sys

HERE = pathlib.Path(__file__).resolve().parent

# Gestes de la prise, en coordonnées réelles de l'écran (px) ; l'instant « t » vient de timings.json.
GESTURES = {
    "SHADE": dict(kind="swipe", x1=540, y1=20, x2=560, y2=1350, ms=520),
    "TAP_NOTIF": dict(kind="tap", x=500, y=750),
    "KEEP_25": dict(kind="swipe", x1=500, y1=1120, x2=1060, y2=1060, ms=560),
    "DEL_25": dict(kind="swipe", x1=580, y1=1120, x2=20, y2=1070, ms=560),
    "UP_24": dict(kind="swipe", x1=540, y1=1300, x2=560, y2=520, ms=600),
    "FAV_24": dict(kind="tap", x=325, y=2052),
    "KEEP_24": dict(kind="swipe", x1=500, y1=1120, x2=1060, y2=1060, ms=560),
    "KEEP_23": dict(kind="swipe", x1=500, y1=1120, x2=1060, y2=1060, ms=560),
    "KEEP_22": dict(kind="swipe", x1=500, y1=1120, x2=1060, y2=1060, ms=560),
    "DEL_19": dict(kind="swipe", x1=580, y1=1120, x2=20, y2=1070, ms=560),
    "KEEP_17": dict(kind="swipe", x1=500, y1=1120, x2=1060, y2=1060, ms=560),
    "CONFIRM": dict(kind="tap", x=540, y=2052),
    "CANCEL": dict(kind="tap", x=378, y=1442),
}

TEXTS = {
    "fr": {
        "t1": ("Titre notification", "Chaque jour, vos souvenirs vous attendent"),
        "t2": ("Titre ouverture", "Ce jour-là, toutes les années passées"),
        "t3": ("Titre garder", "À droite, je garde"),
        "t4": ("Titre supprimer", "À gauche, j’écarte le doublon"),
        "t5": ("Titre favori", "Vers le haut, en favori"),
        "t6": ("Titre tri", "Un geste par souvenir"),
        "t7": ("Titre rythme", "Huit souvenirs en moins d’une minute"),
        "t8": ("Titre résumé", "Rien n’est supprimé sans vous"),
        "t9": ("Titre corbeille", "Restaurable 30 jours dans la corbeille"),
        "tagline": ("Accroche finale", "Vos souvenirs, un jour à la fois"),
        "stores": ("Où l’installer", "Sur Google Play et F-Droid"),
        "credit": ("Crédits", "Photos : Jérémy Reynaud · Musique : bibliothèque musicale HeyGen"),
    },
    "en": {
        "t1": ("Notification title", "Every day, your memories are waiting"),
        "t2": ("Opening title", "This day, in every past year"),
        "t3": ("Keep title", "Swipe right to keep"),
        "t4": ("Delete title", "Swipe left to drop the duplicate"),
        "t5": ("Favorite title", "Swipe up to favorite"),
        "t6": ("Sorting title", "One swipe per memory"),
        "t7": ("Pace title", "Eight memories in under a minute"),
        "t8": ("Summary title", "Nothing is deleted without you"),
        "t9": ("Trash title", "Restorable from the trash for 30 days"),
        "tagline": ("Closing line", "Your memories, one day at a time"),
        "stores": ("Where to get it", "On Google Play and F-Droid"),
        "credit": ("Credits", "Photos: Jérémy Reynaud · Music: HeyGen music library"),
    },
}

# Le premier titre anglais est plus long : plus étroit, il ne touche pas le téléphone en gros plan.
EXTRA_CSS = {"fr": "", "en": "      #t1 { width: 640px; }\n"}

HEAD = """<!doctype html>
<html lang="__LANG__" data-composition-variables='__VARS__'>
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=1920, height=1080" />
    <script src="https://cdn.jsdelivr.net/npm/gsap@3.15.0/dist/gsap.min.js"></script>
    <style>
      @font-face { font-family: "Atkinson Hyperlegible Next"; src: url("assets/fonts/atkinson_hyperlegible_next.ttf"); font-weight: 200 800; }
      @font-face { font-family: "Plex Mono PS"; src: url("assets/fonts/ibm_plex_mono_medium.ttf"); }
      :root { --ink: #2a1e08; --accent: #f2c96b; --frame: #231a0e;
        --font-display: "Atkinson Hyperlegible Next", sans-serif; --font-mono: "Plex Mono PS", monospace; }
      * { margin: 0; padding: 0; box-sizing: border-box; }
      html, body { width: 1920px; height: 1080px; overflow: hidden; background: #edd29a; }
      #root { position: relative; width: 100%; height: 100%; overflow: hidden; font-family: var(--font-display); color: var(--ink); }
      #ground { position: absolute; inset: 0; background: linear-gradient(160deg, #f5e3be 0%, #edd29a 55%, #e2bc72 100%); }
      #cam { position: absolute; left: 1166px; top: 46px; width: 470px; height: 988px; transform-origin: 50% 50%; }
      #phone { position: absolute; inset: 0; background: var(--frame); border-radius: 76px; padding: 22px; box-shadow: 0 40px 80px rgba(60, 40, 5, 0.35); }
      #screen { position: relative; width: 100%; height: 100%; border-radius: 56px; overflow: hidden; background: #fbf6ee; }
      #screen video { position: absolute; left: 0; top: 0; width: 100%; height: 100%; object-fit: cover; }
      #touch { position: absolute; left: 0; top: 0; width: 46px; height: 46px; border-radius: 50%; background: rgba(255, 255, 255, 0.55); border: 3px solid rgba(42, 30, 8, 0.55); box-shadow: 0 2px 10px rgba(0, 0, 0, 0.25); opacity: 0; z-index: 5; }
      .title { position: absolute; left: 144px; top: 0; height: 1080px; width: 900px; display: flex; flex-direction: column; justify-content: center; }
      .title h2 { text-wrap: balance; font-size: 84px; line-height: 1.08; font-weight: 600; letter-spacing: -0.01em; }
      .title .w { display: inline-block; margin-right: 0.24em; }
      .edge { display: inline-block; background: var(--ink); color: var(--accent); font-family: var(--font-mono); clip-path: polygon(0 0, calc(100% - 0.5em) 0, 100% 0.5em, 100% 100%, 0 100%); }
      #end { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; }
      #end-lockup { display: flex; align-items: center; gap: 40px; }
      #end-icon { width: 200px; height: 200px; border-radius: 48px; box-shadow: 0 24px 48px rgba(60, 40, 5, 0.3); }
      #end-name { font-size: 132px; font-weight: 600; letter-spacing: -0.02em; }
      #end-tagline { margin-top: 44px; font-size: 60px; font-weight: 600; }
      #end-stores { margin-top: 36px; font-size: 40px; padding: 18px 36px; border-radius: 16px; }
      #end-credit { position: absolute; bottom: 64px; left: 0; right: 0; text-align: center; font-size: 28px; opacity: 0.7; }
__CSS__    </style>
  </head>
  <body>
"""

SCRIPT_HEAD = """    <script>
      const vars = (window.__hyperframes && window.__hyperframes.getVariables && window.__hyperframes.getVariables()) || {};
      document.querySelectorAll("[data-var-text]").forEach((el) => {
        const value = vars[el.dataset.varText];
        if (typeof value === "string") el.textContent = value;
      });
      document.querySelectorAll(".title h2").forEach((h) => {
        h.innerHTML = h.textContent.trim().split(/\\s+/).map((w) => `<span class="w">${w}</span>`).join("");
      });
      const tl = gsap.timeline({ paused: true });
      const K = 426 / 1080;
      // Place le point (rx, ry) de l'écran réel en (tx, ty) de l'image, au facteur s.
      function focus(rx, ry, s, tx, ty) {
        const cx = 22 + rx * K, cy = 22 + ry * K;
        return { scale: s, x: tx - 1401 - s * (cx - 235), y: ty - 540 - s * (cy - 494) };
      }
      function cam(at, rx, ry, s, tx, ty, dur) {
        tl.to("#cam", Object.assign(focus(rx, ry, s, tx, ty), { duration: dur, ease: "power2.inOut" }), at);
      }
      // Le doigt : un point qui suit chaque geste de la prise, au même instant.
      function touch(g, at) {
        const p = (x, y) => ({ x: x * K - 23, y: y * K - 23 });
        if (g.kind === "tap") {
          tl.set("#touch", Object.assign(p(g.x, g.y), { opacity: 0, scale: 0.6 }), at - 0.12);
          tl.to("#touch", { opacity: 1, scale: 1, duration: 0.1, ease: "power2.out" }, at - 0.1);
          tl.to("#touch", { opacity: 0, scale: 1.25, duration: 0.25, ease: "power1.out" }, at + 0.08);
        } else {
          tl.set("#touch", Object.assign(p(g.x1, g.y1), { opacity: 0, scale: 0.8 }), at - 0.14);
          tl.to("#touch", { opacity: 1, scale: 1, duration: 0.1, ease: "power2.out" }, at - 0.12);
          tl.to("#touch", Object.assign(p(g.x2, g.y2), { duration: g.ms / 1000, ease: "none" }), at);
          tl.to("#touch", { opacity: 0, duration: 0.16, ease: "power1.out" }, at + g.ms / 1000);
        }
      }
      function title(id, start, dur, first) {
        const words = `#${id} .w`;
        if (!first) tl.fromTo(words, { opacity: 0, filter: "blur(6px)" }, { opacity: 1, filter: "blur(0px)", duration: 0.13, stagger: 0.067, ease: "power2.out" }, start + 0.1);
        tl.to(words, { opacity: 0, filter: "blur(6px)", duration: 0.18, stagger: 0.03, ease: "power2.in" }, start + dur - 0.28);
      }
      function endCard(at) {
        tl.fromTo("#end-icon", { scale: 0.4, opacity: 0 }, { scale: 1, opacity: 1, duration: 0.6, ease: "back.out(1.6)" }, at + 0.1);
        tl.fromTo("#end-name", { clipPath: "inset(0 100% 0 0)" }, { clipPath: "inset(0 0% 0 0)", duration: 0.5, ease: "power3.out" }, at + 0.45);
        tl.fromTo("#end-tagline", { opacity: 0, filter: "blur(8px)" }, { opacity: 1, filter: "blur(0px)", duration: 0.25 }, at + 1.0);
        tl.fromTo("#end-stores", { opacity: 0, y: 20 }, { opacity: 1, y: 0, duration: 0.4, ease: "expo.out" }, at + 1.4);
        tl.fromTo("#end-credit", { opacity: 0 }, { opacity: 0.7, duration: 0.3 }, at + 1.6);
      }
"""

SCRIPT_TAIL = """      window.__timelines = window.__timelines || {};
      window.__timelines["main"] = tl;
    </script>
  </body>
</html>
"""


def r(x):
    return round(x, 3)


def sfx(id_, file, at, vol, dur=0.6):
    return (f'      <audio id="{id_}" class="clip" data-start="{r(at)}" data-duration="{dur}" data-volume="{vol}" '
            f'src="assets/sfx/{file}" data-track-index="5"></audio>')


def film(lang, timings):
    """Page HyperFrames du film : une seule prise, la caméra s'approche et recule sans couper."""
    text = TEXTS[lang]
    g = {name: dict(GESTURES[name], t=timings[name]) for name in GESTURES if name in timings}
    gestures = list(g.values())
    notif, summary, dialog, end = (timings[k] for k in ("NOTIF_POSTED", "SUMMARY_SHOWN", "DIALOG_SHOWN", "TAKE_END"))
    o = notif - 1.05  # le film commence 1 s avant l'arrivée de la notification
    vend = end - o
    end_start, total = vend + 0.15, vend + 4.9
    t = lambda name: g[name]["t"]  # noqa: E731
    slots = [
        ("t1", 0.0, t("TAP_NOTIF") - o + 0.1),
        ("t2", t("TAP_NOTIF") - o + 0.1, t("KEEP_25") - t("TAP_NOTIF") - 0.15),
        ("t3", t("KEEP_25") - o - 0.05, t("DEL_25") - t("KEEP_25")),
        ("t4", t("DEL_25") - o - 0.05, t("UP_24") - t("DEL_25")),
        ("t5", t("UP_24") - o - 0.05, t("KEEP_24") - t("UP_24")),
        ("t6", t("KEEP_24") - o - 0.05, t("KEEP_22") - t("KEEP_24")),
        ("t7", t("KEEP_22") - o - 0.05, summary - t("KEEP_22")),
        ("t8", summary - o - 0.05, dialog - summary),
        ("t9", dialog - o - 0.05, end - dialog + 0.05),
    ]
    variables = [{"id": k, "type": "string", "label": label, "default": value} for k, (label, value) in text.items()]
    head = (HEAD.replace("__LANG__", lang)
            .replace("__VARS__", json.dumps(variables, ensure_ascii=False).replace("'", "’"))
            .replace("__CSS__", EXTRA_CSS[lang]))
    body = [
        f'    <div id="root" data-composition-id="main" data-start="0" data-duration="{r(total)}" data-width="1920" data-height="1080">',
        '      <div id="ground"></div>',
        '      <div id="cam">\n        <div id="phone">\n          <div id="screen">',
        f'            <video id="take" class="clip" data-start="0" data-duration="{r(vend)}" data-media-start="{r(o)}" '
        'src="assets/footage/take.mp4" muted playsinline data-track-index="1"></video>',
        '            <div id="touch"></div>\n          </div>\n        </div>\n      </div>',
    ]
    body += [f'      <div id="{k}" class="title clip" data-start="{r(s)}" data-duration="{r(d)}" data-track-index="2">'
             f'<h2 data-var-text="{k}">{text[k][1]}</h2></div>' for k, s, d in slots]
    body.append(
        f'      <div id="end" class="clip" data-start="{r(end_start)}" data-duration="{r(total - end_start)}" data-track-index="3">\n'
        '        <div id="end-lockup">\n          <img id="end-icon" src="assets/images/icon.png" alt="" />\n'
        '          <div id="end-name">PhotoSwipe</div>\n        </div>\n'
        f'        <div id="end-tagline" data-var-text="tagline">{text["tagline"][1]}</div>\n'
        f'        <div id="end-stores" class="edge" data-var-text="stores">{text["stores"][1]}</div>\n'
        f'        <div id="end-credit" data-var-text="credit">{text["credit"][1]}</div>\n      </div>')
    body.append(f'      <audio id="music" class="clip" data-start="0" data-duration="{r(total)}" data-volume="0.8" '
                'data-fade-out="1.5" src="assets/music.mp3" data-track-index="4"></audio>')
    body.append(sfx("sfx-notif", "notification.mp3", notif - o, 0.45, 2.4))
    # Un souffle par glissement (deux sons en alternance) ; un appui reste muet : un clic sonne « souris ».
    swipes = [x for x in gestures if x["kind"] == "swipe"]
    body += [sfx(f"sfx-{i}", "whoosh-short.mp3" if i % 2 == 0 else "whoosh.mp3", x["t"] - o + 0.08,
                 0.22 if i % 2 == 0 else 0.14) for i, x in enumerate(swipes)]
    body.append("    </div>")
    s = "".join(f'      title("{k}", {r(st)}, {r(d)}, {str(n == 0).lower()});\n' for n, (k, st, d) in enumerate(slots))
    s += "".join(f"      touch({json.dumps(x)}, {r(x['t'] - o)});\n" for x in gestures)
    s += f"""      tl.set("#cam", focus(540, 200, 2.6, 1430, 600), 0);
      cam({r(t('SHADE') - o - 0.1)}, 540, 820, 1.85, 1360, 560, 0.7);
      cam({r(t('TAP_NOTIF') - o + 0.3)}, 540, 1200, 1, 1401, 540, 0.8);
      cam({r(summary - o)}, 540, 620, 1.55, 1360, 540, 0.6);
      cam({r(dialog - o - 0.1)}, 540, 1250, 1.5, 1360, 540, 0.5);
      cam({r(vend - 0.55)}, 540, 1200, 1, 1401, 540, 0.4);
      tl.to("#cam", {{ y: 1250, duration: 0.4, ease: "power3.in" }}, {r(vend - 0.3)});
      endCard({r(end_start)});
"""
    return head + "\n".join(body) + "\n" + SCRIPT_HEAD + s + SCRIPT_TAIL


def main():
    for lang, folder in (("fr", "a"), ("en", "a-en")):
        timings = json.loads((HERE / folder / "timings.json").read_text())
        (HERE / folder / "index.html").write_text(film(lang, timings), encoding="utf-8")
        print(folder, "ok")


if __name__ == "__main__":
    sys.exit(main())
