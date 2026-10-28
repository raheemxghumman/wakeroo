"""Builds Play Store screenshot mockups (1080x1920, 9:16) from raw emulator captures.

Put captures (1080x2400 PNGs named as in SLIDES) in tools/store/raw/, then run: python tools/store_mockups.py
Output goes to tools/store/final/. Fonts come from the app's res/font folder.
"""
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
RAW = os.path.join(HERE, "store", "raw")
OUT = os.path.join(HERE, "store", "final")
FONTS = os.path.join(HERE, "..", "app", "src", "main", "res", "font")
os.makedirs(OUT, exist_ok=True)

W, H = 1080, 1920
NIGHT_TOP, NIGHT_BOTTOM = (14, 17, 34), (38, 30, 72)
PAPER, LAVENDER = (245, 241, 234), (186, 170, 255)
GLOWS = {"coral": (255, 122, 89), "lavender": (150, 120, 255), "gold": (255, 196, 84), "mint": (90, 214, 170)}

SLIDES = [
    ("01_ring", "Won't stop until\nyou're up", "Solve a mission to turn it off", "coral"),
    ("02_mission", "Wake your brain up", "Math, typing, memory and more", "lavender"),
    ("07_picker", "10 wake-up missions", "Scan the sink, walk, shake. All free.", "gold"),
    ("scene_ring", "Wake up to a scene", "Animated sunrises, not a beep", "coral"),
    ("05_home", "Alarms you can trust", "Rings through Doze, reboots and\nDo Not Disturb", "mint"),
    ("03_briefing", "Start your day right", "Your plan, weather and streak", "gold"),
    ("08_morning", "Your morning, sorted", "Weather, quote and wake-up time", "lavender"),
    ("09_you", "Build a wake-up streak", "Calendar, stats and medals", "coral"),
]


def font(name, size):
    return ImageFont.truetype(os.path.join(FONTS, name), size)


def background(glow):
    bg = Image.new("RGB", (W, H))
    px = ImageDraw.Draw(bg)
    for y in range(H):
        t = y / (H - 1)
        px.line([(0, y), (W, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(NIGHT_TOP, NIGHT_BOTTOM)))
    # Soft colour glow behind the phone, plus a faint second one for depth.
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    c = GLOWS[glow]
    d.ellipse([W * 0.08, H * 0.36, W * 0.92, H * 0.98], fill=c + (150,))
    d.ellipse([-W * 0.3, -H * 0.12, W * 0.5, H * 0.22], fill=(120, 100, 255, 50))
    layer = layer.filter(ImageFilter.GaussianBlur(160))
    bg = Image.alpha_composite(bg.convert("RGBA"), layer)
    # A few stars, matching the app's night sky.
    import random
    rnd = random.Random(glow)
    d = ImageDraw.Draw(bg)
    for _ in range(40):
        x, y, r = rnd.randint(0, W), rnd.choice([rnd.randint(0, 90), rnd.randint(440, int(H * 0.5))]), rnd.choice([1, 1, 2, 2, 3])
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, rnd.randint(60, 170)))
    return bg


def rounded(img, radius):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.size[0] - 1, img.size[1] - 1], radius, fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def phone(shot, width):
    screen = shot.convert("RGBA")
    h = round(screen.height * width / screen.width)
    screen = screen.resize((width, h), Image.LANCZOS)
    bezel = 16
    body = Image.new("RGBA", (width + bezel * 2, h + bezel * 2), (0, 0, 0, 0))
    ImageDraw.Draw(body).rounded_rectangle([0, 0, body.width - 1, body.height - 1], 92, fill=(8, 9, 16, 255),
                                           outline=(70, 66, 96, 255), width=3)
    body.alpha_composite(rounded(screen, 76), (bezel, bezel))
    # Camera hole.
    cx = body.width // 2
    ImageDraw.Draw(body).ellipse([cx - 13, bezel + 22, cx + 13, bezel + 48], fill=(5, 5, 9, 255))
    return body


def build(name, title, sub, glow):
    src = os.path.join(RAW, name + ".png")
    shot = Image.open(src)
    canvas = background(glow)
    d = ImageDraw.Draw(canvas)
    t_font, s_font = font("outfit_bold.ttf", 84), font("inter_medium.ttf", 42)
    y = 120
    for line in title.split("\n"):
        w = d.textlength(line, font=t_font)
        d.text(((W - w) / 2, y), line, font=t_font, fill=PAPER)
        y += 100
    y += 16
    for line in sub.split("\n"):
        w = d.textlength(line, font=s_font)
        d.text(((W - w) / 2, y), line, font=s_font, fill=LAVENDER)
        y += 56
    body = phone(shot, 720)
    top = max(y + 60, 470)
    # Shadow under the phone.
    sh = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle([(W - body.width) // 2 + 10, top + 40, (W + body.width) // 2 - 10,
                                          top + body.height], 92, fill=(0, 0, 0, 150))
    canvas = Image.alpha_composite(canvas, sh.filter(ImageFilter.GaussianBlur(40)))
    canvas.alpha_composite(body, ((W - body.width) // 2, top))
    out = os.path.join(OUT, f"{len(os.listdir(OUT)) + 1:02d}_{name}.png" if False else f"{name}.png")
    canvas.convert("RGB").save(out, optimize=True)
    return out


if __name__ == "__main__":
    import sys
    only = sys.argv[1:]
    for i, (name, title, sub, glow) in enumerate(SLIDES, 1):
        if only and name not in only:
            continue
        if not os.path.exists(os.path.join(RAW, name + ".png")):
            print("missing", name)
            continue
        p = build(name, title, sub, glow)
        final = os.path.join(OUT, f"{i:02d}_{name}.png")
        os.replace(p, final)
        print(final)
