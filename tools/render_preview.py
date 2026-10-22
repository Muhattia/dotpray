#!/usr/bin/env python3
"""Render preview mock-ups of the dotpray app and its widgets.

These are design previews only - they mirror the real layouts and colours so
the README can show what the app looks like without a device.
"""
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(ROOT)
RES = os.path.join(PROJECT, "app", "src", "main", "res")
FONT_DIR = os.path.join(RES, "font")
OUT_DIR = os.path.join(PROJECT, "preview")

MONO = os.path.join(FONT_DIR, "doto_regular.ttf")
MONO_B = os.path.join(FONT_DIR, "doto_bold.ttf")

BG = (0, 0, 0)
SURFACE = (0x0A, 0x0A, 0x0A)
SURFACE_HIGH = (0x12, 0x12, 0x12)
LINE = (0x1F, 0x1F, 0x1F)
LINE_SOFT = (0x14, 0x14, 0x14)
FG = (0xF4, 0xF4, 0xF2)
FG_DIM = (0x8E, 0x8E, 0x8E)
FG_FAINT = (0x56, 0x56, 0x56)
ACCENT = (0xD7, 0x19, 0x21)

# Widget-only colours. The bitmap widget draws its own translucent card, so
# these are swapped (together with the main palette) when rendering the light
# picker previews.
CARD_FILL = (0, 0, 0)
CARD_LINE = (0x26, 0x26, 0x26)
CARD_DOT = (20, 20, 20)
INACTIVE = (0x56, 0x56, 0x56)
ARC_1 = (44, 44, 44)
ARC_2 = (30, 30, 30)
TRACK = (0x26, 0x26, 0x26)
FOOTER = (0x3A, 0x3A, 0x3A)

S = 3  # px per dp


def d(v):
    return int(round(v * S))


def set_palette(light):
    """Switch the module palette; render_* read these at call time."""
    global BG, SURFACE, SURFACE_HIGH, LINE, LINE_SOFT, FG, FG_DIM, FG_FAINT
    global CARD_FILL, CARD_LINE, CARD_DOT, INACTIVE, ARC_1, ARC_2, TRACK, FOOTER
    if light:
        BG = (0xFF, 0xFF, 0xFF)
        SURFACE = (0xF7, 0xF7, 0xF7)
        SURFACE_HIGH = (0xF0, 0xF0, 0xF0)
        LINE = (0xDC, 0xDC, 0xDC)
        LINE_SOFT = (0xED, 0xED, 0xED)
        FG = (0x0A, 0x0A, 0x0A)
        FG_DIM = (0x5E, 0x5E, 0x5E)
        FG_FAINT = (0x8A, 0x8A, 0x8A)
        CARD_FILL = (0xFF, 0xFF, 0xFF)
        CARD_LINE = (0xE2, 0xE2, 0xE2)
        CARD_DOT = (0xED, 0xED, 0xED)
        INACTIVE = (0x9A, 0x9A, 0x9A)
        ARC_1 = (0xE0, 0xE0, 0xE0)
        ARC_2 = (0xEA, 0xEA, 0xEA)
        TRACK = (0xE0, 0xE0, 0xE0)
        FOOTER = (0xBB, 0xBB, 0xBB)
    else:
        BG = (0, 0, 0)
        SURFACE = (0x0A, 0x0A, 0x0A)
        SURFACE_HIGH = (0x12, 0x12, 0x12)
        LINE = (0x1F, 0x1F, 0x1F)
        LINE_SOFT = (0x14, 0x14, 0x14)
        FG = (0xF4, 0xF4, 0xF2)
        FG_DIM = (0x8E, 0x8E, 0x8E)
        FG_FAINT = (0x56, 0x56, 0x56)
        CARD_FILL = (0, 0, 0)
        CARD_LINE = (0x26, 0x26, 0x26)
        CARD_DOT = (20, 20, 20)
        INACTIVE = (0x56, 0x56, 0x56)
        ARC_1 = (44, 44, 44)
        ARC_2 = (30, 30, 30)
        TRACK = (0x26, 0x26, 0x26)
        FOOTER = (0x3A, 0x3A, 0x3A)


def font(path, size_dp):
    return ImageFont.truetype(path, d(size_dp))


def doto(weight, size_dp):
    return ImageFont.truetype(os.path.join(FONT_DIR, "doto_%s.ttf" % weight), d(size_dp))


def spaced(draw, xy, text, fnt, fill, tracking=0.0, anchor="la"):
    """Draw text with letter spacing (Android letterSpacing emulation)."""
    x, y = xy
    widths = [draw.textlength(ch, font=fnt) for ch in text]
    total = sum(widths) + tracking * S * max(0, len(text) - 1)
    if anchor[0] == "r":
        x -= total
    elif anchor[0] == "m":
        x -= total / 2
    for ch, w in zip(text, widths):
        draw.text((x, y), ch, font=fnt, fill=fill, anchor="l" + anchor[1])
        x += w + tracking * S
    return x


def dotted_field(draw, w, h, spacing=22, radius=1.05, alpha=18):
    for gy in range(spacing, h + spacing, spacing):
        for gx in range(spacing, w + spacing, spacing):
            draw.ellipse([d(gx) - d(radius), d(gy) - d(radius),
                          d(gx) + d(radius), d(gy) + d(radius)], fill=(255, 255, 255, 0))


def dot_field(draw, w, h, spacing=22, radius=1.05, alpha=20):
    # On black the dots are white at a low alpha; on white they are black.
    tone = 255 - alpha if BG[0] > 127 else alpha
    for gy in range(spacing, h + spacing, spacing):
        for gx in range(spacing, w + spacing, spacing):
            r = d(radius)
            draw.ellipse([d(gx) - r, d(gy) - r, d(gx) + r, d(gy) + r],
                         fill=(tone, tone, tone))


def rounded(draw, box, radius, fill=None, outline=None, width=1):
    draw.rounded_rectangle(box, radius=d(radius), fill=fill, outline=outline,
                           width=d(width) if outline else 0)


def draw_arc_glyph(draw, cx, cy, r, color, dashes=44, start=-150, end=-30):
    import math
    for i in range(dashes):
        t0 = start + (end - start) * i / dashes
        t1 = t0 + (end - start) / dashes * 0.45
        for t in (t0, t1):
            a = math.radians(t)
            x = cx + r * math.cos(a)
            y = cy + r * math.sin(a)
            rr = d(1.1)
            draw.ellipse([d(x) - rr, d(y) - rr, d(x) + rr, d(y) + rr], fill=color)


# ────────────────────────────────────────────────────────────────────── app screen
def render_app(path=None):
    W, H = d(360), d(990)
    img = Image.new("RGB", (W, H), BG)
    dr = ImageDraw.Draw(img)
    dot_field(dr, W, H, alpha=16)

    L = d(22)
    R = W - d(22)
    y = d(20)

    f_black = doto("black", 26)
    f_label = font(MONO, 10)
    f_label_faint = font(MONO, 10)
    f_value = font(MONO, 13)
    f_display = doto("bold", 30)
    f_display_big = doto("bold", 84)

    # top bar
    dr.text((L, y), "DOTPRAY", font=f_black, fill=FG, anchor="la")
    y += d(31)
    spaced(dr, (L, y), "PRAYER CLOCK / OFFLINE", f_label, FG_FAINT, 0.18)
    y += d(20)

    y += d(14)
    # city pill + date
    pill_h = d(30)
    ph = d(15)
    dr.ellipse([L + d(12), y + (pill_h - d(6)) // 2, L + d(12) + d(6), y + (pill_h - d(6)) // 2 + d(6)],
               fill=ACCENT)
    spaced(dr, (L + d(26), y + d(9)), "CAIRO", font(MONO_B, 10), FG, 0.18)
    pill_w = d(96)
    rounded(dr, [L, y, L + pill_w, y + pill_h], 100, fill=SURFACE, outline=LINE, width=1)
    # redraw text over pill
    dr.ellipse([L + d(12), y + (pill_h - d(6)) // 2, L + d(12) + d(6), y + (pill_h - d(6)) // 2 + d(6)],
               fill=ACCENT)
    spaced(dr, (L + d(26), y + d(9)), "CAIRO", font(MONO_B, 10), FG, 0.18)

    spaced(dr, (R, y + d(3)), "THU / 22 OCT 2026", f_label, FG_DIM, 0.18, anchor="ra")
    spaced(dr, (R, y + d(18)), "11 JUMADA I 1448", f_label_faint, FG_FAINT, 0.14, anchor="ra")

    y += pill_h + d(18)

    # hero card
    card_top = y
    card_h = d(236)
    rounded(dr, [L, card_top, R, card_top + card_h], 26, fill=SURFACE, outline=LINE, width=1)

    # dotted glyph arc, top right
    draw_arc_glyph(dr, R - d(60), card_top + d(58), d(52), ARC_1, dashes=40,
                   start=-165, end=-15)
    draw_arc_glyph(dr, R - d(60), card_top + d(58), d(38), ARC_2, dashes=32,
                   start=-160, end=-20)

    cx = L + d(20)
    cy = card_top + d(18)
    # pulse dot
    dr.ellipse([cx, cy, cx + d(10), cy + d(10)], fill=ACCENT)
    dr.ellipse([cx - d(4), cy - d(4), cx + d(14), cy + d(14)], outline=(0x7A, 0x12, 0x19), width=d(1))
    spaced(dr, (cx + d(19), cy + d(1)), "NEXT PRAYER", f_label, FG_DIM, 0.18)
    spaced(dr, (R - d(20), cy + d(1)), "04 / 05", f_label_faint, FG_FAINT, 0.14, anchor="ra")

    ny = card_top + d(18) + d(10) + d(16)
    dr.text((cx, ny), "MAGHRIB", font=f_display, fill=FG, anchor="la")

    dr.text((cx, ny + d(34)), "18:17", font=f_display_big, fill=FG, anchor="la")

    cy2 = ny + d(34) + d(86)
    spaced(dr, (cx, cy2), "NEXT IN", f_label_faint, FG_FAINT, 0.14)
    spaced(dr, (cx + d(66), cy2), "01:25:12", f_value, FG, 0.06)

    # progress rail
    py = cy2 + d(18)
    dr.line([cx, py, R - d(20), py], fill=TRACK, width=d(1.4))
    ex = cx + (R - d(20) - cx) * 0.42
    dr.line([cx, py, ex, py], fill=ACCENT, width=d(2.4))
    dr.ellipse([ex - d(2.6), py - d(2.6), ex + d(2.6), py + d(2.6)], fill=ACCENT)

    py += d(12)
    spaced(dr, (cx, py), "SINCE ASR 15:51", f_label_faint, FG_FAINT, 0.14)
    spaced(dr, (R - d(20), py), "42% ELAPSED", f_label_faint, FG_FAINT, 0.14, anchor="ra")

    y = card_top + card_h + d(26)

    # today divider
    spaced(dr, (L, y), "TODAY", font(MONO_B, 10), FG, 0.18)
    dr.line([L + d(52), y + d(5), R - d(38), y + d(5)], fill=LINE, width=d(1))
    spaced(dr, (R, y), "24H", f_label_faint, FG_FAINT, 0.14, anchor="ra")
    y += d(22)

    prayers = [("01", "FAJR", "05:34"),
               ("02", "DHUHR", "12:40"),
               ("03", "ASR", "15:51"),
               ("04", "MAGHRIB", "18:17"),
               ("05", "ISHA", "19:35")]
    f_pname = doto("bold", 18)
    f_ptime = doto("bold", 24)
    for idx, name, t in prayers:
        row_h = d(54)
        active_row = name == "MAGHRIB"
        if active_row:
            rounded(dr, [L, y, R, y + row_h], 18, fill=FG)
            name_color = BG
            time_color = BG
            idx_color = (0x8A, 0x8A, 0x8A)
        else:
            rounded(dr, [L, y, R, y + row_h], 18, fill=SURFACE_HIGH, outline=LINE_SOFT, width=1)
            name_color = FG
            time_color = FG
            idx_color = FG_FAINT
        spaced(dr, (L + d(16), y + d(21)), idx, f_label_faint, idx_color, 0.18)
        dr.text((L + d(42), y + d(14)), name, font=f_pname, fill=name_color, anchor="la")
        dr.text((R - d(16), y + d(13)), t, font=f_ptime, fill=time_color, anchor="ra")
        if active_row:
            spaced(dr, (R - d(16), y + d(38)), "NEXT", font(MONO_B, 9), ACCENT, 0.18, anchor="ra")
        y += row_h + d(7)

    y += d(15)
    # location tile, full width
    tile_h = d(128)
    rounded(dr, [L, y, R, y + tile_h], 20, fill=SURFACE_HIGH, outline=LINE, width=1)
    spaced(dr, (L + d(16), y + d(16)), "CURRENT LOCATION", f_label_faint, FG_FAINT, 0.14)
    spaced(dr, (L + d(16), y + d(38)), "CAIRO, EG", f_value, FG, 0.06)
    spaced(dr, (L + d(16), y + d(60)), "30.044°N / 31.236°E", f_label_faint, FG_FAINT, 0.14)
    spaced(dr, (L + d(16), y + d(78)), "QIBLA 136° SE", f_label_faint, FG_FAINT, 0.14)
    spaced(dr, (L + d(16), y + d(100)), "GPS FIX · TAP TO REFRESH", f_label, ACCENT, 0.18)

    y += tile_h + d(22)
    # buttons
    bw = (R - L - d(10)) * 135 // 235
    rounded(dr, [L, y, L + bw, y + d(52)], 100, fill=FG)
    spaced(dr, (L + bw // 2, y + d(19)), "ADD WIDGET", font(MONO_B, 12), BG, 0.14, anchor="ma")
    bx2 = L + bw + d(10)
    rounded(dr, [bx2, y, R, y + d(52)], 100, outline=LINE, width=1)
    spaced(dr, ((bx2 + R) // 2, y + d(19)), "SETTINGS", font(MONO_B, 12), FG, 0.14, anchor="ma")

    y += d(70)
    spaced(dr, (W // 2, y), "Calculated on device · No ads · No tracking",
           f_label_faint, FG_FAINT, 0.14, anchor="ma")
    y += d(20)
    spaced(dr, (W // 2, y), "DOTPRAY 1.1 · DOTO (OFL)",
           f_label_faint, FOOTER, 0.14, anchor="ma")

    path = path or os.path.join(OUT_DIR, "app-screen.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("wrote " + os.path.relpath(path, PROJECT))


# ────────────────────────────────────────────────────────────────────── widgets
def _inside_round(x, y, w, h, r):
    if x < r and y < r:
        return (x - r) ** 2 + (y - r) ** 2 <= r * r
    if x > w - r and y < r:
        return (x - (w - r)) ** 2 + (y - r) ** 2 <= r * r
    if x < r and y > h - r:
        return (x - r) ** 2 + (y - (h - r)) ** 2 <= r * r
    if x > w - r and y > h - r:
        return (x - (w - r)) ** 2 + (y - (h - r)) ** 2 <= r * r
    return True


def _card(dr, img, w, h, radius=28):
    r = d(radius)
    dr.rounded_rectangle([0, 0, w - 1, h - 1], radius=r, fill=CARD_FILL,
                         outline=CARD_LINE, width=max(1, d(1)))
    step = d(22)
    rr = d(1)
    y = step / 2
    while y < h:
        x = step / 2
        while x < w:
            if _inside_round(x, y, w, h, r):
                dr.ellipse([x - rr, y - rr, x + rr, y + rr], fill=CARD_DOT)
            x += step
        y += step


def _spaced_px(dr, xy, text, fnt, size_px, color, tracking=0.0, align="left"):
    widths = [dr.textlength(ch, font=fnt) for ch in text]
    gap = tracking * size_px
    total = sum(widths) + gap * max(0, len(text) - 1)
    x, y = xy
    if align == "right":
        x -= total
    for ch, cw in zip(text, widths):
        dr.text((x, y), ch, font=fnt, fill=color, anchor="ls")
        x += cw + gap


def _compose(base, content, dw, dh):
    """Paste the design-space content scaled uniformly and centred."""
    w, h = base.size
    scale = min(w / dw, h / dh)
    cw, ch = max(1, round(dw * scale)), max(1, round(dh * scale))
    content = content.resize((cw, ch), Image.LANCZOS)
    base.paste(content, (round((w - cw) / 2), round((h - ch) / 2)), content)


def render_widget_wide(width_dp=326, height_dp=150, path=None, drawable=None):
    W, H = d(width_dp), d(height_dp)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    _card(dr, img, W, H)

    DW, DH = d(326), d(150)
    layer = Image.new("RGBA", (DW, DH), (0, 0, 0, 0))
    cd = ImageDraw.Draw(layer)

    pad = d(16)
    header = d(23)
    divider = DH - d(42)
    name_base = divider - d(52)
    meta_base = divider - d(30)
    count_base = divider - d(14)
    clock_base = divider - d(30)

    f_10b = doto("bold", 10)
    f_9b = doto("bold", 9)
    f_22 = doto("bold", 22)
    f_46 = doto("bold", 46)
    f_14 = doto("bold", 14)
    f_13 = doto("regular", 13)

    cd.ellipse([pad, d(16), pad + d(6), d(22)], fill=ACCENT)
    _spaced_px(cd, (pad + d(13), header), "NEXT PRAYER", f_10b, d(10), FG_DIM, 0.18)
    _spaced_px(cd, (DW - pad, header), "CAIRO", f_10b, d(10), FG_FAINT, 0.14, "right")

    cd.text((pad, name_base), "MAGHRIB", font=f_22, fill=FG, anchor="ls")
    _spaced_px(cd, (pad, meta_base), "THU 22 OCT · TODAY", f_10b, d(10), FG_FAINT, 0.04)
    _spaced_px(cd, (pad, count_base), "IN 1H 25M", f_13, d(13), FG, 0.06)
    cd.text((DW - pad, clock_base), "18:17", font=f_46, fill=FG, anchor="rs")

    # progress hairline
    cd.line([pad, divider, DW - pad, divider], fill=CARD_LINE, width=max(1, d(1)))
    cd.line([pad, divider, pad + (DW - pad * 2) * 0.42, divider], fill=ACCENT,
            width=max(2, d(2)))

    names = [("FAJR", "05:34"), ("DHUHR", "12:40"), ("ASR", "15:51"),
             ("MAGHRIB", "18:17"), ("ISHA", "19:35")]
    col = (DW - pad * 2) / 5
    for i, (n, t) in enumerate(names):
        x = pad + col * i
        active = i == 3
        _spaced_px(cd, (x, divider + d(16)), n, f_9b, d(9),
                   FG if active else FG_FAINT, 0.08)
        cd.text((x, divider + d(33)), t, font=f_14,
                fill=FG if active else INACTIVE, anchor="ls")

    _compose(img, layer, DW, DH)

    if path:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
        print("wrote " + os.path.relpath(path, PROJECT))
    if drawable:
        os.makedirs(drawable, exist_ok=True)
        img.save(os.path.join(drawable, "widget_preview.png"))
        print("wrote " + os.path.relpath(os.path.join(drawable, "widget_preview.png"), PROJECT))


def render_widget_small(width_dp=150, height_dp=150, path=None, drawable=None):
    W, H = d(width_dp), d(height_dp)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    _card(dr, img, W, H)

    DW, DH = d(150), d(150)
    layer = Image.new("RGBA", (DW, DH), (0, 0, 0, 0))
    cd = ImageDraw.Draw(layer)

    pad = d(14)
    header = d(23)
    meta_base = DH - d(14)
    count_base = DH - d(32)
    clock_base = DH - d(56)
    name_base = DH - d(100)

    f_10b = doto("bold", 10)
    f_19 = doto("bold", 19)
    f_36 = doto("bold", 36)
    f_11 = doto("regular", 11)

    cd.ellipse([pad, d(16.5), pad + d(5), d(21.5)], fill=ACCENT)
    _spaced_px(cd, (pad + d(10), header), "NEXT", f_10b, d(10), FG_DIM, 0.18)
    _spaced_px(cd, (DW - pad, header), "CAIRO", f_10b, d(10), FG_FAINT, 0.14, "right")

    cd.text((pad, name_base), "MAGHRIB", font=f_19, fill=FG, anchor="ls")
    cd.text((pad, clock_base), "18:17", font=f_36, fill=FG, anchor="ls")
    _spaced_px(cd, (pad, count_base), "IN 1H 25M", f_11, d(11), FG, 0.05)
    _spaced_px(cd, (pad, meta_base), "TODAY", f_10b, d(10), FG_FAINT, 0.14)

    _compose(img, layer, DW, DH)

    if path:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
        print("wrote " + os.path.relpath(path, PROJECT))
    if drawable:
        os.makedirs(drawable, exist_ok=True)
        img.save(os.path.join(drawable, "widget_preview_small.png"))
        print("wrote " + os.path.relpath(os.path.join(drawable, "widget_preview_small.png"), PROJECT))


if __name__ == "__main__":
    set_palette(False)
    render_app()
    render_widget_wide(path=os.path.join(OUT_DIR, "widget-wide.png"),
                       drawable=os.path.join(RES, "drawable-night-nodpi"))
    render_widget_small(path=os.path.join(OUT_DIR, "widget-small.png"),
                        drawable=os.path.join(RES, "drawable-night-nodpi"))

    set_palette(True)
    render_widget_wide(drawable=os.path.join(RES, "drawable-nodpi"))
    render_widget_small(drawable=os.path.join(RES, "drawable-nodpi"))
    render_app(path=os.path.join(OUT_DIR, "app-screen-light.png"))
