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
    W, H = d(360), d(1070)
    img = Image.new("RGB", (W, H), BG)
    dr = ImageDraw.Draw(img)
    dot_field(dr, W, H, alpha=16)

    L = d(22)
    R = W - d(22)
    y = d(20)

    f_black = doto("black", 26)
    f_label = font(MONO, 10)
    f_label_b = doto("bold", 10)
    f_label_faint = doto("bold", 9)
    f_value = font(MONO, 13)
    f_display = doto("bold", 30)
    f_display_big = doto("bold", 82)
    f_countdown = font(MONO, 22)

    # top bar: brand is primary, the wall clock is contextual and quiet
    dr.text((L, y), "DOTPRAY", font=f_black, fill=FG, anchor="la")
    spaced(dr, (L, y + d(31)), "PRAYER CLOCK / OFFLINE", f_label, FG_FAINT, 0.18)
    spaced(dr, (R, y + d(23)), "NOW", f_label, FG_FAINT, 0.14, anchor="ra")
    spaced(dr, (R, y + d(37)), "17:12:05", font(MONO, 12), FG_DIM, 0.04, anchor="ra")

    y += d(58)

    # location + date. The short locality is the primary label; the full
    # location and coordinates sit on the quieter line underneath.
    pill_h = d(32)
    pill_w = d(122)
    rounded(dr, [L, y, L + pill_w, y + pill_h], 100, fill=SURFACE, outline=LINE, width=1)
    dr.ellipse([L + d(12), y + d(13), L + d(18), y + d(19)], fill=ACCENT)
    spaced(dr, (L + d(26), y + d(10)), "CAIRO", f_label_b, FG, 0.18)

    spaced(dr, (R, y + d(1)), "FRI / 23 OCT 2026", f_label, FG_DIM, 0.18, anchor="ra")
    spaced(dr, (R, y + d(17)), "12 JUMADA AL-AWWAL 1448", f_label_faint, FG_FAINT, 0.14, anchor="ra")
    spaced(dr, (L, y + d(43)), "CAIRO, EG · 30.044°N / 31.236°E", f_label_faint,
           FG_FAINT, 0.14)

    y += d(58)

    # next-prayer card, with the countdown attached under the scheduled time
    card_top = y
    card_h = d(236)
    rounded(dr, [L, card_top, R, card_top + card_h], 26, fill=SURFACE, outline=LINE, width=1)

    draw_arc_glyph(dr, R - d(54), card_top + d(48), d(44), ARC_1, dashes=38,
                   start=-165, end=-15)
    draw_arc_glyph(dr, R - d(54), card_top + d(48), d(32), ARC_2, dashes=30,
                   start=-160, end=-20)

    cx = L + d(20)
    cy = card_top + d(18)
    dr.ellipse([cx, cy, cx + d(10), cy + d(10)], fill=ACCENT)
    dr.ellipse([cx - d(4), cy - d(4), cx + d(14), cy + d(14)], outline=(0x7A, 0x12, 0x19),
               width=d(1))
    spaced(dr, (cx + d(19), cy + d(1)), "NEXT PRAYER", f_label, FG_DIM, 0.18)
    spaced(dr, (R - d(20), cy + d(1)), "04 / 05", f_label_faint, FG_FAINT, 0.14,
           anchor="ra")

    name_y = card_top + d(60)
    dr.text((cx, name_y), "MAGHRIB", font=f_display, fill=FG, anchor="ls")

    time_y = card_top + d(130)
    dr.text((cx, time_y), "18:17", font=f_display_big, fill=FG, anchor="ls")

    count_y = card_top + d(172)
    spaced(dr, (cx, count_y), "IN", f_label_b, ACCENT, 0.14)
    dr.text((cx + d(24), count_y), "01:04:55", font=f_countdown, fill=FG,
            anchor="ls")

    # elapsed-time rail from the previous prayer to the next
    py = count_y + d(18)
    dr.line([cx, py, R - d(20), py], fill=TRACK, width=d(1.4))
    ex = cx + (R - d(20) - cx) * 0.56
    dr.line([cx, py, ex, py], fill=ACCENT, width=d(2.4))
    dr.ellipse([ex - d(2.6), py - d(2.6), ex + d(2.6), py + d(2.6)], fill=ACCENT)
    py += d(14)
    spaced(dr, (cx, py), "SINCE ASR 15:51", f_label_faint, FG_FAINT, 0.14)
    spaced(dr, (R - d(20), py), "56% ELAPSED", f_label_faint, FG_FAINT, 0.14, anchor="ra")

    y = card_top + card_h + d(24)

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
        row_h = d(57)
        active_row = name == "MAGHRIB"
        rounded(dr, [L, y, R, y + row_h], 18, fill=SURFACE_HIGH,
                outline=(0x3A, 0x14, 0x16) if active_row else LINE_SOFT, width=1)
        name_color = FG
        time_color = FG
        idx_color = ACCENT if active_row else FG_FAINT
        if active_row:
            dr.ellipse([L + d(12), y + d(24), L + d(18), y + d(30)], fill=ACCENT)
        spaced(dr, (L + d(28), y + d(21)), idx, f_label_b, idx_color, 0.18)
        dr.text((L + d(58), y + d(14)), name, font=f_pname, fill=name_color, anchor="la")
        dr.text((R - d(16), y + d(13)), t, font=f_ptime, fill=time_color, anchor="ra")
        if active_row:
            spaced(dr, (R - d(16), y + d(38)), "NEXT", f_label_b, ACCENT, 0.18,
                   anchor="ra")
        y += row_h + d(8)

    y += d(15)
    # location tile, full width; detailed location stays secondary to the hero
    tile_h = d(128)
    rounded(dr, [L, y, R, y + tile_h], 20, fill=SURFACE_HIGH, outline=LINE, width=1)
    spaced(dr, (L + d(16), y + d(16)), "CURRENT LOCATION", f_label_faint, FG_FAINT, 0.14)
    spaced(dr, (L + d(16), y + d(38)), "CAIRO, EG", f_value, FG, 0.06)
    spaced(dr, (L + d(16), y + d(60)), "30.044°N / 31.236°E", f_label_faint,
           FG_FAINT, 0.14)
    spaced(dr, (L + d(16), y + d(78)), "QIBLA 136° SE", f_label_faint, FG_FAINT, 0.14)
    spaced(dr, (L + d(16), y + d(100)), "GPS FIX · TAP TO REFRESH", f_label_b,
           ACCENT, 0.18)

    y += tile_h + d(22)
    # buttons
    bw = (R - L - d(10)) * 135 // 235
    rounded(dr, [L, y, L + bw, y + d(52)], 100, fill=FG)
    spaced(dr, (L + bw // 2, y + d(19)), "ADD WIDGET", font(MONO_B, 12), BG, 0.14,
           anchor="ma")
    bx2 = L + bw + d(10)
    rounded(dr, [bx2, y, R, y + d(52)], 100, outline=LINE, width=1)
    spaced(dr, ((bx2 + R) // 2, y + d(19)), "SETTINGS", font(MONO_B, 12), FG, 0.14,
           anchor="ma")

    y += d(70)
    spaced(dr, (W // 2, y), "Calculated on device · No ads · No tracking",
           f_label_faint, FG_FAINT, 0.14, anchor="ma")
    y += d(20)
    spaced(dr, (W // 2, y), "DOTPRAY 1.12 · DOTO (OFL)",
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
    elif align in ("center", "middle"):
        x -= total / 2
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
    header = d(17)
    name_base = d(46)
    time_base = d(82)
    count_base = d(98)
    right = DW - pad
    now_base = d(52)
    greg_base = d(74)
    hijri_base = d(85)
    divider = d(111)
    sched_name = d(123)
    sched_time = d(140)

    f_10b = doto("bold", 10)
    f_95b = doto("bold", 9.5)
    f_9b = doto("bold", 9)
    f_85b = doto("bold", 8.5)
    f_8b = doto("bold", 8)
    f_22 = doto("bold", 22)
    f_24 = doto("bold", 24)
    f_40 = doto("bold", 40)
    f_125 = doto("regular", 12.5)
    f_13 = doto("regular", 13)

    # left column: the next prayer, with the red status dot on the heading
    cd.ellipse([pad, header - d(6.5), pad + d(6), header - d(0.5)], fill=ACCENT)
    _spaced_px(cd, (pad + d(13), header), "NEXT PRAYER", f_10b, d(10), FG_DIM, 0.18)
    _spaced_px(cd, (pad, name_base), "MAGHRIB", f_22, d(22), FG, 0.04)
    _spaced_px(cd, (pad, time_base), "18:17", f_40, d(40), FG, -0.01)

    count_text = "01:04:55"
    label_w = sum(cd.textlength(ch, font=f_85b) for ch in "IN") + d(8.5) * 0.12
    gap = d(4)
    _spaced_px(cd, (pad, count_base), "IN", f_85b, d(8.5), ACCENT, 0.12)
    cd.text((pad + label_w + gap, count_base), count_text, font=f_125, fill=FG,
            anchor="ls")

    # right column: location at the top, then the quieter wall clock and dates
    _spaced_px(cd, (right, header), "AL MANTEQAH", f_95b, d(9.5), FG_FAINT, 0.12,
               "right")
    _spaced_px(cd, (right, now_base), "17:12:05", f_24, d(24), FG_DIM, 0.0, "right")
    _spaced_px(cd, (right, greg_base), "FRI 23 OCT", f_85b, d(8.5), FG_DIM, 0.08,
               "right")
    _spaced_px(cd, (right, hijri_base), "12 JUM I 1448", f_8b, d(8), FG_FAINT, 0.08,
               "right")

    # schedule, active prayer gets a small red label and a bright time.
    # Measure each block and spread the spare width into the gaps so the
    # strip is flush with both ends of the divider instead of leaving an
    # empty tail after Isha.
    cd.line([pad, divider, DW - pad, divider], fill=CARD_LINE, width=max(1, d(1)))
    progress_end = pad + (DW - pad * 2) * 0.56
    cd.line([pad, divider, progress_end, divider], fill=ACCENT,
            width=max(2, d(2)))
    cd.ellipse([progress_end - d(2.2), divider - d(2.2),
                progress_end + d(2.2), divider + d(2.2)], fill=ACCENT)
    names = [("FAJR", "05:34"), ("DHUHR", "12:40"), ("ASR", "15:51"),
             ("MAGHRIB", "18:17"), ("ISHA", "19:35")]
    blocks = []
    for n, t in names:
        nw = sum(cd.textlength(ch, font=f_9b) for ch in n) + d(9) * 0.08 * (len(n) - 1)
        tw = sum(cd.textlength(ch, font=f_13) for ch in t)
        blocks.append(max(nw, tw))
    gap = max(d(6), ((DW - pad * 2) - sum(blocks)) / (len(names) - 1))
    x = pad
    for i, (n, t) in enumerate(names):
        active = i == 3
        _spaced_px(cd, (x, sched_name), n, f_9b, d(9),
                   ACCENT if active else FG_FAINT, 0.08)
        cd.text((x, sched_time), t, font=f_13, fill=FG if active else INACTIVE,
                anchor="ls")
        x += blocks[i] + gap

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

    center = DW / 2
    name_base = d(58)
    time_base = d(89)
    count_base = d(106)

    f_20 = doto("bold", 20)
    f_35 = doto("bold", 35)
    f_12 = doto("regular", 12)
    f_82b = doto("bold", 8.16)

    _spaced_px(cd, (center, name_base), "MAGHRIB", f_20, d(20), FG, 0.04, "center")
    _spaced_px(cd, (center, time_base), "18:17", f_35, d(35), FG, -0.01, "center")

    count = "01:04:55"
    label_w = (sum(cd.textlength(ch, font=f_82b) for ch in "IN")
               + d(8.16) * 0.12)
    gap = d(4)
    value_w = sum(cd.textlength(ch, font=f_12) for ch in count)
    left = center - (label_w + gap + value_w) / 2
    _spaced_px(cd, (left, count_base), "IN", f_82b, d(8.16), ACCENT, 0.12)
    cd.text((left + label_w + gap, count_base), count, font=f_12, fill=FG,
            anchor="ls")

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
