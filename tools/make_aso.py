#!/usr/bin/env python3
"""Build the Google Play ASO package for Dotpray.

Outputs:
  aso/screenshots/01-prayer-times.png ... 05-private.png   (1080x1920)
  aso/screenshots/06-team.png                              (1080x1920)
  aso/graphics/feature-graphic-1024x500.png
  aso/graphics/app-icon-512.png

The phone screenshots are composed from the layout-accurate previews in
preview/. They are marketing renders, not device captures.
"""
from pathlib import Path
import math
import textwrap

from PIL import Image, ImageDraw, ImageEnhance, ImageFont, ImageOps

ROOT = Path(__file__).resolve().parent.parent
PREVIEW = ROOT / "preview"
OUT = ROOT / "aso"
SHOTS = OUT / "screenshots"
GFX = OUT / "graphics"
TEAM = OUT / "team"
FONT_DIR = ROOT / "app" / "src" / "main" / "res" / "font"

W, H = 1080, 1920
M = 72

BG = (0, 0, 0)
PANEL = (5, 5, 5)
SURFACE = (10, 10, 10)
LINE = (31, 31, 31)
LINE_SOFT = (20, 20, 20)
FG = (244, 244, 242)
DIM = (142, 142, 142)
FAINT = (86, 86, 86)
RED = (215, 25, 33)
RED_DARK = (122, 18, 25)

LIGHT_BG = (255, 255, 255)
LIGHT_PANEL = (247, 247, 247)
LIGHT_LINE = (220, 220, 220)
LIGHT_FG = (10, 10, 10)
LIGHT_DIM = (94, 94, 94)
LIGHT_FAINT = (138, 138, 138)


def font(weight, size):
    return ImageFont.truetype(str(FONT_DIR / f"doto_{weight}.ttf"), size)


F_BLACK = font("black", 80)
F_BOLD = font("bold", 44)
F_BODY = font("regular", 28)
F_SMALL = font("regular", 22)
F_LABEL = font("bold", 19)
F_TINY = font("regular", 18)


def tracking_text(draw, xy, text, fnt, fill, tracking=0, anchor="la"):
    """Draw uppercase tracking text; anchor 'l', 'm', or 'r' for x."""
    x, y = xy
    widths = [draw.textlength(ch, font=fnt) for ch in text]
    total = sum(widths) + tracking * max(0, len(text) - 1)
    if anchor[0] == "m":
        x -= total / 2
    elif anchor[0] == "r":
        x -= total
    for ch, width in zip(text, widths):
        draw.text((x, y), ch, font=fnt, fill=fill, anchor="l" + anchor[1])
        x += width + tracking
    return x


def wrap(draw, text, fnt, max_width):
    words = text.split()
    lines, current = [], ""
    for word in words:
        trial = word if not current else current + " " + word
        if draw.textlength(trial, font=fnt) <= max_width:
            current = trial
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def draw_lines(draw, xy, lines, fnt, fill, line_height):
    x, y = xy
    for line in lines:
        draw.text((x, y), line, font=fnt, fill=fill, anchor="la")
        y += line_height
    return y


def dot_field(draw, w, h, light=False, spacing=36, radius=1.25):
    tone = (236, 236, 236) if light else (18, 18, 18)
    for gy in range(spacing // 2, h + spacing, spacing):
        for gx in range(spacing // 2, w + spacing, spacing):
            draw.ellipse([gx - radius, gy - radius, gx + radius, gy + radius], fill=tone)


def label(draw, x, y, text, fill=FAINT, light=False, dot=True):
    if dot:
        draw.ellipse([x, y + 3, x + 7, y + 10], fill=RED)
        x += 17
    tracking_text(draw, (x, y), text.upper(), F_LABEL, fill, 3.2)


def footer(draw, text, light=False, y=1848):
    tracking_text(draw, (W / 2, y), text.upper(), F_LABEL,
                  LIGHT_FAINT if light else FAINT, 3.0, anchor="ma")


def rounded_mask(size, radius):
    mask = Image.new("L", size, 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle([0, 0, size[0] - 1, size[1] - 1], radius=radius, fill=255)
    return mask


def paste_rounded(canvas, im, xy, radius):
    mask = rounded_mask(im.size, radius)
    canvas.paste(im, xy, mask)


def phone(canvas, screenshot, x, y, w, light=False, pad=10):
    """Paste a full app screenshot in a restrained phone frame."""
    h = round(w * screenshot.height / screenshot.width)
    draw = ImageDraw.Draw(canvas)
    frame = (LIGHT_PANEL if light else (7, 7, 7))
    outline = LIGHT_LINE if light else (38, 38, 38)
    draw.rounded_rectangle([x - pad, y - pad, x + w + pad, y + h + pad],
                           radius=34, fill=frame, outline=outline, width=2)
    shot = screenshot.resize((w, h), Image.Resampling.LANCZOS)
    paste_rounded(canvas, shot, (x, y), 25)
    # A hairline inside the frame makes the screen feel inset on light stocks.
    draw.rounded_rectangle([x, y, x + w, y + h], radius=25,
                           outline=(0, 0, 0, 70) if not light else (255, 255, 255, 140),
                           width=1)
    return h


def card_image():
    """The hero next-prayer card from the dark app preview."""
    app = Image.open(PREVIEW / "app-screen.png").convert("RGB")
    # Card bounds in the 1080x3210 render, with a little air around the border.
    return app.crop((58, 398, 1022, 1126))


def caption(draw, text, y, fill=DIM, size=30, tracking=0.3):
    tracking_text(draw, (W / 2, y), text.upper(), font("bold", size), fill,
                  tracking, anchor="ma")


def shot_prayer_times():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    dot_field(draw, W, H)
    label(draw, M, 62, "DOTPRAY / PRAYER CLOCK")
    draw_lines(draw, (M, 104), ["PRAYER TIMES,", "WITHOUT THE NOISE."],
               F_BLACK, FG, 82)
    sub = wrap(draw, "The next prayer, a live countdown, and the full day — on one quiet screen.",
               F_BODY, 940)
    draw_lines(draw, (M, 286), sub, F_BODY, DIM, 35)
    app = Image.open(PREVIEW / "app-screen.png").convert("RGB")
    phone(img, app, 300, 382, 480)
    footer(draw, "CALCULATED ON DEVICE  ·  NO ADS  ·  NO TRACKING")
    img.save(SHOTS / "01-prayer-times.png", optimize=True)
    return img


def shot_countdown():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    dot_field(draw, W, H)
    label(draw, M, 62, "DOTPRAY / NEXT PRAYER")
    draw_lines(draw, (M, 104), ["EVERY SECOND", "COUNTS."], F_BLACK, FG, 82)
    # Red full stop, on the baseline of the second line.
    draw.text((M, 104 + 82), "COUNTS.", font=F_BLACK, fill=RED, anchor="la")
    sub = wrap(draw, "A countdown that moves every second, with a rail from the last prayer.",
               F_BODY, 940)
    draw_lines(draw, (M, 286), sub, F_BODY, DIM, 35)

    card = card_image().resize((900, 679), Image.Resampling.LANCZOS)
    # The card carries its own hairline; paste it directly so there is no
    # second outline around the hero.
    paste_rounded(img, card, (90, 408), 70)

    y = 1250
    draw.line([M, y - 28, W - M, y - 28], fill=LINE, width=1)
    items = [
        ("01", "LIVE SECONDS", "Ticks every second."),
        ("02", "ELAPSED RAIL", "Since the last prayer."),
        ("03", "FIVE PRAYERS", "The whole day."),
    ]
    for i, (num, head, body) in enumerate(items):
        x = M + i * 328
        tracking_text(draw, (x, y), num, F_LABEL, RED, 3.0)
        tracking_text(draw, (x, y + 40), head, font("bold", 29), FG, 1.6)
        draw.text((x, y + 84), body, font=font("regular", 24), fill=DIM, anchor="la")
    caption(draw, "NO REFRESH. NO TAP. JUST TIME.", 1510, FG, 34, 1.4)
    footer(draw, "SINCE THE LAST PRAYER  ·  UNTIL THE NEXT")
    img.save(SHOTS / "02-live-countdown.png", optimize=True)
    return img


def shot_widgets():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    dot_field(draw, W, H)
    label(draw, M, 62, "DOTPRAY / HOME SCREEN")
    draw_lines(draw, (M, 104), ["YOUR NEXT PRAYER,", "AT A GLANCE."], F_BLACK, FG, 82)
    sub = wrap(draw, "Two widget sizes: a wide 4×2 schedule or a compact 2×2 clock.",
               F_BODY, 940)
    draw_lines(draw, (M, 286), sub, F_BODY, DIM, 35)

    wide = Image.open(PREVIEW / "widget-wide.png").convert("RGBA")
    wide = wide.resize((900, 414), Image.Resampling.LANCZOS)
    wide_panel = Image.new("RGBA", wide.size, (0, 0, 0, 0))
    wide_panel.alpha_composite(wide)
    img.paste(wide_panel, (90, 430), wide_panel)

    small = Image.open(PREVIEW / "widget-small.png").convert("RGBA")
    small = small.resize((340, 340), Image.Resampling.LANCZOS)
    img.paste(small, (370, 930), small)

    tracking_text(draw, (W / 2, 396), "4×2  /  WIDE", font("bold", 22), DIM, 3.2, anchor="ma")
    tracking_text(draw, (W / 2, 894), "2×2  /  COMPACT", font("bold", 22), DIM, 3.2, anchor="ma")

    draw.line([M, 1370, W - M, 1370], fill=LINE, width=1)
    caption(draw, "ONE GLANCE. THE WHOLE DAY.", 1430, FG, 40, 1.2)
    footer(draw, "RESIZES TO FIT  ·  FOLLOWS THE APP THEME")
    img.save(SHOTS / "03-widgets.png", optimize=True)
    return img


def shot_themes():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    dot_field(draw, W, H)
    label(draw, M, 62, "DOTPRAY / THEMES")
    draw_lines(draw, (M, 104), ["DARK. LIGHT.", "YOUR CHOICE."], F_BLACK, FG, 82)
    sub = wrap(draw, "Follow the system, or keep it fixed. Both widgets follow.",
               F_BODY, 940)
    draw_lines(draw, (M, 286), sub, F_BODY, DIM, 35)

    dark = Image.open(PREVIEW / "app-screen.png").convert("RGB")
    light = Image.open(PREVIEW / "app-screen-light.png").convert("RGB")
    phone(img, dark, 50, 400, 450)
    phone(img, light, 580, 400, 450, light=True)

    tracking_text(draw, (275, 1768), "DARK", F_LABEL, DIM, 3.0, anchor="ma")
    tracking_text(draw, (805, 1768), "LIGHT", F_LABEL, DIM, 3.0, anchor="ma")
    footer(draw, "SYSTEM  /  LIGHT  /  DARK  ·  BOTH WIDGETS FOLLOW", y=1824)
    img.save(SHOTS / "04-themes.png", optimize=True)
    return img


def team_photo(path, crop=None, centering=(0.5, 0.5), size=452):
    """Square, neutral-monochrome portraits that sit inside the dot-matrix system."""
    im = Image.open(path).convert("RGB")
    if crop:
        im = im.crop(crop)
    im = ImageOps.fit(im, (size, size), Image.Resampling.LANCZOS, centering=centering)
    gray = ImageOps.grayscale(im)
    gray = ImageOps.autocontrast(gray, cutoff=0.5)
    gray = ImageEnhance.Contrast(gray).enhance(1.06)
    gray = ImageEnhance.Sharpness(gray).enhance(1.08)
    return ImageOps.colorize(gray, black=(7, 7, 7), white=(244, 244, 242)).convert("RGB")


def shot_team():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    dot_field(draw, W, H)
    label(draw, M, 62, "DOTPRAY / CREDITS")

    icon = Image.open(PREVIEW / "app-icon.png").convert("RGBA").resize((140, 140), Image.Resampling.LANCZOS)
    paste_rounded(img, icon, (M, 108), 22)
    draw_lines(draw, (240, 104), ["TWO PEOPLE.", "ONE QUIET APP."], F_BLACK, FG, 82)
    draw.text((240, 298), "Developed by Muhammad Attia.", font=F_BODY, fill=DIM, anchor="la")
    draw.text((240, 335), "Original idea by Muhammad M. Hemdan.", font=F_BODY, fill=DIM, anchor="la")

    size, gap, top = 452, 32, 432
    x1, x2 = M, M + size + gap
    label(draw, x1, 397, "01 / DEVELOPER", fill=RED)
    label(draw, x2, 397, "02 / ORIGINAL IDEA", fill=RED)

    p1 = team_photo(TEAM / "muhammad-attia.jpg", crop=(250, 0, 1750, 1500))
    p2 = team_photo(TEAM / "muhammad-hemdan.png", centering=(0.5, 0.46))
    for x, photo in ((x1, p1), (x2, p2)):
        paste_rounded(img, photo, (x, top), 26)
        draw.rounded_rectangle([x, top, x + size, top + size], radius=26,
                               outline=(44, 44, 44), width=2)

    draw.text((x1, 908), "MUHAMMAD ATTIA", font=font("black", 38), fill=FG, anchor="la")
    draw.text((x2, 908), "MUHAMMAD M. HEMDAN", font=font("black", 38), fill=FG, anchor="la")
    link_font = font("regular", 24)
    draw.text((x1, 962), "github.com/Muhattia", font=link_font, fill=DIM, anchor="la")
    draw.text((x2, 962), "github.com/karasudidntwakeup", font=link_font, fill=DIM, anchor="la")

    draw.line([M, 1048, W - M, 1048], fill=LINE, width=1)
    label(draw, M, 1082, "OPEN SOURCE / THE REPO")
    draw.text((M, 1114), "github.com/Muhattia/dotpray",
              font=font("black", 50), fill=FG, anchor="la")
    draw.text((M, 1184), "Source code, releases, and issues — all public.",
              font=F_BODY, fill=DIM, anchor="la")

    # External-link arrow: the one deliberately non-typographic mark.
    ax, ay = W - M - 48, 1128
    draw.line([ax - 22, ay + 22, ax + 22, ay - 22], fill=RED, width=4)
    draw.line([ax + 22, ay - 22, ax - 2, ay - 22], fill=RED, width=4)
    draw.line([ax + 22, ay - 22, ax + 22, ay + 2], fill=RED, width=4)

    py, ph = 1272, 184
    draw.rounded_rectangle([M, py, W - M, py + ph], radius=22,
                           fill=SURFACE, outline=LINE, width=1)
    icon2 = Image.open(PREVIEW / "app-icon.png").convert("RGBA").resize((92, 92), Image.Resampling.LANCZOS)
    paste_rounded(img, icon2, (M + 28, py + 34), 18)
    draw.text((M + 146, py + 34), "DOTPRAY 1.2", font=font("black", 38), fill=FG, anchor="la")
    tracking_text(draw, (M + 150, py + 92), "OFFLINE PRAYER CLOCK", F_LABEL, FAINT, 2.8)
    tracking_text(draw, (W - M - 28, py + 50), "FREE", F_LABEL, RED, 3.0, anchor="ra")
    tracking_text(draw, (W - M - 28, py + 84), "NO ADS", F_LABEL, DIM, 3.0, anchor="ra")
    tracking_text(draw, (W - M - 28, py + 118), "NO TRACKING", F_LABEL, DIM, 3.0, anchor="ra")

    tracking_text(draw, (W / 2, 1636), "TWO PEOPLE. ONE IDEA. NO NOISE.",
                  font("black", 44), FG, 0.5, anchor="ma")
    footer(draw, "DOTPRAY 1.2  ·  MADE BY TWO PEOPLE")
    img.save(SHOTS / "06-team.png", optimize=True)
    return img


def shot_privacy():
    img = Image.new("RGB", (W, H), BG)
    draw = ImageDraw.Draw(img)
    dot_field(draw, W, H)
    label(draw, M, 62, "DOTPRAY / PRIVACY")

    icon = Image.open(PREVIEW / "app-icon.png").convert("RGBA").resize((140, 140), Image.Resampling.LANCZOS)
    paste_rounded(img, icon, (M, 108), 22)

    draw_lines(draw, (240, 104), ["PRIVATE BY", "DEFAULT."], F_BLACK, FG, 82)
    draw.text((240, 298), "A prayer app with no server.",
              font=F_BODY, fill=DIM, anchor="la")

    rows = [
        ("100% ON-DEVICE", "Prayer times are calculated locally."),
        ("NO ADS", "Nothing to tap, nothing to sell."),
        ("NO TRACKING", "No analytics, no accounts, no profiles."),
        ("NO INTERNET PERMISSION", "The app cannot send your data anywhere."),
    ]
    y = 444
    for i, (head, body) in enumerate(rows):
        draw.line([M, y, W - M, y], fill=LINE, width=1)
        draw.rectangle([M, y + 46, M + 8, y + 54], fill=RED)
        tracking_text(draw, (M + 26, y + 36), head, font("black", 42), FG, 0.8)
        draw.text((M + 26, y + 100), body, font=F_BODY, fill=DIM, anchor="la")
        y += 232

    draw.line([M, y, W - M, y], fill=LINE, width=1)
    draw.rounded_rectangle([M, 1428, W - M, 1578], radius=22, fill=SURFACE, outline=LINE, width=1)
    tracking_text(draw, (M + 28, 1454), "LOCATION IS USED ON-DEVICE", F_LABEL, RED, 2.6)
    body = wrap(draw, "To centre the times. No Dotpray server, no account, no upload.",
                font("regular", 24), 880)
    draw_lines(draw, (M + 28, 1498), body, font("regular", 24), DIM, 29)
    caption(draw, "NO ACCOUNT. NO SERVER. NO CATCH.", 1680, FG, 36, 1.2)
    footer(draw, "NO ADS  ·  NO TRACKING  ·  NO INTERNET PERMISSION")
    img.save(SHOTS / "05-private.png", optimize=True)
    return img


def feature_graphic():
    w, h = 1024, 500
    img = Image.new("RGB", (w, h), BG)
    draw = ImageDraw.Draw(img)
    dot_field(draw, w, h, spacing=30, radius=1.05)
    draw.rectangle([0, 0, 7, h], fill=RED)

    tracking_text(draw, (58, 52), "DOTPRAY / PRAYER CLOCK", F_LABEL, FAINT, 3.0)
    draw.text((54, 78), "DOTPRAY", font=font("black", 104), fill=FG, anchor="la")
    tracking_text(draw, (58, 204), "PRAYER TIMES, WITHOUT THE NOISE.", font("bold", 25), FG, 0.8)
    tracking_text(draw, (58, 258), "OFFLINE  ·  NO ADS  ·  NO TRACKING", F_LABEL, FAINT, 2.8)

    # A compact next-prayer card, using the product's real numbers and rail.
    x, y, cw, ch = 590, 92, 390, 320
    draw.rounded_rectangle([x, y, x + cw, y + ch], radius=26, fill=SURFACE, outline=LINE, width=2)
    draw.ellipse([x + 24, y + 22, x + 38, y + 36], fill=RED)
    tracking_text(draw, (x + 52, y + 20), "NEXT PRAYER", F_LABEL, DIM, 2.4)
    draw.text((x + 24, y + 54), "MAGHRIB", font=font("bold", 40), fill=FG, anchor="la")
    draw.text((x + 22, y + 100), "18:17", font=font("black", 96), fill=FG, anchor="la")
    tracking_text(draw, (x + 24, y + 208), "IN", F_LABEL, RED, 2.0)
    draw.text((x + 58, y + 198), "01:04:55", font=font("regular", 30), fill=FG, anchor="la")
    draw.line([x + 24, y + 250, x + cw - 24, y + 250], fill=(38, 38, 38), width=3)
    draw.line([x + 24, y + 250, x + 24 + (cw - 48) * 0.56, y + 250], fill=RED, width=5)
    draw.ellipse([x + 24 + (cw - 48) * 0.56 - 4, y + 246,
                  x + 24 + (cw - 48) * 0.56 + 4, y + 254], fill=RED)
    tracking_text(draw, (x + 24, y + 270), "SINCE ASR 15:51", F_TINY, FAINT, 1.2)
    tracking_text(draw, (x + cw - 24, y + 290), "56% ELAPSED", F_TINY, FAINT, 1.2, anchor="ra")

    img.save(GFX / "feature-graphic-1024x500.png", optimize=True)
    return img


def app_icon():
    icon = Image.open(PREVIEW / "app-icon.png").convert("RGBA")
    icon = icon.resize((512, 512), Image.Resampling.LANCZOS)
    icon.save(GFX / "app-icon-512.png", optimize=True)
    return icon


def main():
    SHOTS.mkdir(parents=True, exist_ok=True)
    GFX.mkdir(parents=True, exist_ok=True)
    shot_prayer_times()
    shot_countdown()
    shot_widgets()
    shot_themes()
    shot_team()
    shot_privacy()
    feature_graphic()
    app_icon()
    print("Wrote ASO assets to", OUT)


if __name__ == "__main__":
    main()
