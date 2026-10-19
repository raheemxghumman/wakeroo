"""Builds every Wakeroo brand asset from one set of geometry.

Usage: python build_brand.py <brand-out-dir> <app/src/main/res>
"""
import math
import os
import sys

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

NIGHT, SURFACE, CORAL, LAV, GOLD, PAPER, DAY = (
    "#0E1120", "#1A1E33", "#FF7A59", "#B9A7FF", "#FFC857", "#F5F3EE", "#FFF8F2")
OUT, RES = sys.argv[1], sys.argv[2]
FONTS = os.path.join(RES, "font")

# ---------- mark geometry (100 x 100 design grid) ----------
HORIZON_Y, HORIZON_X = 74, (14, 86)
SUN = (54, 69, 20)                      # cx, cy, r: half disc resting on the horizon
LEAP = ((19, 70), (43, -4), (80, 35))   # quadratic curve: take-off, control, landing
DOT_R = 6.6


def q(t):
    (x0, y0), (cx, cy), (x2, y2) = LEAP
    return ((1 - t) ** 2 * x0 + 2 * (1 - t) * t * cx + t * t * x2,
            (1 - t) ** 2 * y0 + 2 * (1 - t) * t * cy + t * t * y2)


def leap_path():
    n = 80
    pts = [q(i / n) for i in range(n + 1)]
    left, right = [], []
    for i in range(n):
        (x0, y0), (x1, y1) = pts[i], pts[i + 1]
        dx, dy = x1 - x0, y1 - y0
        length = math.hypot(dx, dy)
        nx, ny = -dy / length, dx / length
        w = 0.8 + 2.4 * (i / n) ** 1.5    # thin at take-off, thick near landing
        left.append((x0 + nx * w, y0 + ny * w))
        right.append((x0 - nx * w, y0 - ny * w))
    poly = left + right[::-1]
    return "M" + " L".join(f"{x:.2f},{y:.2f}" for x, y in poly) + " Z"


def sun_path():
    cx, cy, r = SUN
    return f"M{cx - r},{cy} A{r},{r} 0 0 1 {cx + r},{cy} Z"


def mark_els(horizon=PAPER, sun=CORAL, leap=LAV, dot=GOLD):
    return [
        f'<line x1="{HORIZON_X[0]}" y1="{HORIZON_Y}" x2="{HORIZON_X[1]}" y2="{HORIZON_Y}" '
        f'stroke="{horizon}" stroke-width="5" stroke-linecap="round"/>',
        f'<path d="{sun_path()}" fill="{sun}"/>',
        f'<path d="{leap_path()}" fill="{leap}"/>',
        f'<circle cx="{LEAP[2][0]}" cy="{LEAP[2][1]}" r="{DOT_R}" fill="{dot}"/>',
    ]


def svg(els, vb="0 0 100 100", w=512, h=512, bg=None):
    vx, vy, vw, vh = vb.split()
    b = f'<rect x="{vx}" y="{vy}" width="{vw}" height="{vh}" fill="{bg}"/>' if bg else ""
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{vb}" width="{w}" height="{h}">'
            f'{b}{"".join(els)}</svg>\n')


def write(name, text):
    with open(os.path.join(OUT, name), "w", encoding="utf-8") as f:
        f.write(text)


# ---------- wordmark from real Outfit SemiBold outlines ----------
def wordmark_paths(text="wakeroo", font="outfit_semibold.ttf", size=100, tracking=-0.015):
    f = TTFont(os.path.join(FONTS, font))
    gs, cmap, hmtx = f.getGlyphSet(), f.getBestCmap(), f["hmtx"]
    s = size / f["head"].unitsPerEm
    x, paths = 0.0, []
    for ch in text:
        g = cmap[ord(ch)]
        pen = SVGPathPen(gs)
        gs[g].draw(TransformPen(pen, (s, 0, 0, -s, x, 0)))
        paths.append(pen.getCommands())
        x += hmtx[g][0] * s + tracking * size
    return paths, x - tracking * size, f["OS/2"].sxHeight * s


WM_PATHS, WM_W, X_H = wordmark_paths()


def wordmark_els(color=PAPER, accent=CORAL, dx=0.0, dy=0.0):
    # The final "o" is the sun: same glyph, coral fill.
    return [f'<path transform="translate({dx:.2f},{dy:.2f})" d="{d}" '
            f'fill="{accent if i == len(WM_PATHS) - 1 else color}"/>'
            for i, d in enumerate(WM_PATHS)]


def main():
    write("mark.svg", svg(mark_els()))
    write("mark-on-night.svg", svg(mark_els(), bg=NIGHT))
    write("mark-on-day.svg", svg(mark_els(horizon=NIGHT), bg=DAY))
    write("mark-mono-dark.svg", svg(mark_els(NIGHT, NIGHT, NIGHT, NIGHT)))
    write("mark-mono-light.svg", svg(mark_els(PAPER, PAPER, PAPER, PAPER)))

    pad, top = 6, X_H * 1.55
    vb = f"{-pad} {-top:.2f} {WM_W + 2 * pad:.2f} {top + 2 * pad:.2f}"
    size = dict(w=int((WM_W + 2 * pad) * 4), h=int((top + 2 * pad) * 4))
    write("wordmark.svg", svg(wordmark_els(), vb=vb, **size))
    write("wordmark-on-night.svg", svg(wordmark_els(), vb=vb, bg=NIGHT, **size))
    write("wordmark-on-day.svg", svg(wordmark_els(color=NIGHT, accent="#E85D3F"), vb=vb, bg=DAY, **size))

    # Horizontal lockup: mark sized so its horizon sits on the wordmark baseline.
    mk = 1.3
    mark_g = f'<g transform="translate(0,{-HORIZON_Y * mk:.2f}) scale({mk})">{"".join(mark_els())}</g>'
    gap = 6
    lw = 100 * mk + gap + WM_W
    lvb = f"-8 {-62 * mk:.2f} {lw + 16:.2f} {62 * mk + 14:.2f}"
    lsize = dict(w=int((lw + 16) * 3), h=int((62 * mk + 14) * 3))
    lock = [mark_g] + wordmark_els(dx=100 * mk + gap)
    write("lockup.svg", svg(lock, vb=lvb, **lsize))
    write("lockup-on-night.svg", svg(lock, vb=lvb, bg=NIGHT, **lsize))

    # Play Store icon, 512 x 512 full-bleed square (Play applies its own mask).
    write("play-icon-512.svg",
          svg([f'<g transform="translate({50 - 50 * 0.8:.2f},{50 - 48 * 0.8:.2f}) scale(0.8)">'
               f'{"".join(mark_els())}</g>'], bg=NIGHT))

    # Play Store feature graphic, 1024 x 500.
    sky = ('<defs><linearGradient id="sky" x1="0" y1="0" x2="0" y2="1">'
           f'<stop offset="0" stop-color="{NIGHT}"/><stop offset="0.6" stop-color="{SURFACE}"/>'
           '<stop offset="1" stop-color="#3A2B4E"/></linearGradient></defs>'
           '<rect width="1024" height="500" fill="url(#sky)"/>')
    fg = [sky,
          f'<g transform="translate(64,84) scale(3.0)">{"".join(mark_els())}</g>',
          f'<g transform="translate(424,268) scale(1.3)">{"".join(wordmark_els())}</g>',
          f'<text x="428" y="336" font-family="Inter, Segoe UI, sans-serif" font-size="34" '
          f'fill="{LAV}">Bounce out of bed.</text>']
    write("feature-graphic.svg",
          '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 500" width="1024" height="500">'
          f'{"".join(fg)}</svg>\n')

    write_android_icons()
    print(f"wordmark width {WM_W:.1f}, x-height {X_H:.1f}")


# ---------- Android vector drawables ----------
def vector(body):
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<!-- Generated by docs/brand/build_brand.py. Edit the script, not this file. -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp"\n    android:height="108dp"\n'
            '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
            f'{body}</vector>\n')


def mark_group(color):
    # Map the 100-unit mark into the 66dp adaptive-icon safe zone, centred on 54.
    # Visible content is centred on (50, 48) of the design grid.
    s = 0.72
    ox, oy = 54 - 50 * s, 54 - 48 * s
    x0, x1 = HORIZON_X
    h = 2.5
    horizon = (f"M{x0},{HORIZON_Y - h} H{x1} A{h},{h} 0 0 1 {x1},{HORIZON_Y + h} "
               f"H{x0} A{h},{h} 0 0 1 {x0},{HORIZON_Y - h} Z")
    cx, cy = LEAP[2]
    dot = (f"M{cx - DOT_R},{cy} a{DOT_R},{DOT_R} 0 1 0 {2 * DOT_R},0 "
           f"a{DOT_R},{DOT_R} 0 1 0 {-2 * DOT_R},0 Z")
    parts = [("horizon", horizon, "paper"), ("sun", sun_path(), "coral"),
             ("leap", leap_path(), "lavender"), ("landing point", dot, "gold")]
    xml = (f'    <group android:translateX="{ox:.2f}" android:translateY="{oy:.2f}" '
           f'android:scaleX="{s}" android:scaleY="{s}">\n')
    for name, d, key in parts:
        xml += f'        <!-- {name} -->\n        <path android:fillColor="{color(key)}" android:pathData="{d}" />\n'
    return xml + "    </group>\n"


def write_android_icons():
    refs = {"paper": "@color/paper", "coral": "@color/sunrise_coral",
            "lavender": "@color/dawn_lavender", "gold": "@color/morning_gold"}
    with open(os.path.join(RES, "drawable", "ic_launcher_foreground.xml"), "w", encoding="utf-8") as f:
        f.write(vector(mark_group(lambda k: refs[k])))
    with open(os.path.join(RES, "drawable", "ic_launcher_monochrome.xml"), "w", encoding="utf-8") as f:
        f.write(vector(mark_group(lambda k: "#FFFFFFFF")))


if __name__ == "__main__":
    main()
