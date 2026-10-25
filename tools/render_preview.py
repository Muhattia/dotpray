#!/usr/bin/env python3
"""Render preview mock-ups of the dotpray app and its widgets.

These are design previews only - they mirror the real layouts and colours so
the README can show what the app looks like without a device.
"""
import math
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.dirname(ROOT)
RES = os.path.join(PROJECT, "app", "src", "main", "res")
FONT_DIR = os.path.join(RES, "font")
OUT_DIR = os.path.join(PROJECT, "preview")

MONO = os.path.join(FONT_DIR, "doto_regular.ttf")
MONO_B = os.path.join(FONT_DIR, "doto_bold.ttf")
ARABIC = os.path.join(FONT_DIR, "alexandria_regular.ttf")
ARABIC_B = os.path.join(FONT_DIR, "alexandria_semibold.ttf")

BG = (0, 0, 0)
SURFACE = (0x0A, 0x0A, 0x0A)
SURFACE_HIGH = (0x12, 0x12, 0x12)
LINE = (0x1F, 0x1F, 0x1F)
LINE_SOFT = (0x14, 0x14, 0x14)
FG = (0xF4, 0xF4, 0xF2)
FG_DIM = (0x8E, 0x8E, 0x8E)
FG_FAINT = (0x7E, 0x7E, 0x7E)
ACCENT = (0xD7, 0x19, 0x21)
ACCENT_TEXT = (0xD7, 0x19, 0x21)

# Widget-only colours. The bitmap widget draws its own translucent card, so
# these are swapped (together with the main palette) when rendering the light
# picker previews.
CARD_FILL = (0, 0, 0)
CARD_LINE = (0x26, 0x26, 0x26)
CARD_DOT = (20, 20, 20)
INACTIVE = (0x7E, 0x7E, 0x7E)
TRACK = (0x26, 0x26, 0x26)
FOOTER = (0x7E, 0x7E, 0x7E)

S = 3  # px per dp


def d(v):
    return int(round(v * S))


def set_palette(light):
    """Switch the module palette; render_* read these at call time."""
    global BG, SURFACE, SURFACE_HIGH, LINE, LINE_SOFT, FG, FG_DIM, FG_FAINT
    global CARD_FILL, CARD_LINE, CARD_DOT, INACTIVE, TRACK, FOOTER
    global ACCENT_TEXT
    if light:
        BG = (0xFF, 0xFF, 0xFF)
        SURFACE = (0xF7, 0xF7, 0xF7)
        SURFACE_HIGH = (0xF0, 0xF0, 0xF0)
        LINE = (0xDC, 0xDC, 0xDC)
        LINE_SOFT = (0xED, 0xED, 0xED)
        FG = (0x0A, 0x0A, 0x0A)
        ACCENT_TEXT = (0xD7, 0x19, 0x21)
        FG_DIM = (0x5E, 0x5E, 0x5E)
        FG_FAINT = (0x6B, 0x6B, 0x6B)
        CARD_FILL = (0xFF, 0xFF, 0xFF)
        CARD_LINE = (0xE2, 0xE2, 0xE2)
        CARD_DOT = (0xED, 0xED, 0xED)
        INACTIVE = (0x6B, 0x6B, 0x6B)
        TRACK = (0xE0, 0xE0, 0xE0)
        FOOTER = (0x6B, 0x6B, 0x6B)
    else:
        BG = (0, 0, 0)
        SURFACE = (0x0A, 0x0A, 0x0A)
        SURFACE_HIGH = (0x12, 0x12, 0x12)
        LINE = (0x1F, 0x1F, 0x1F)
        LINE_SOFT = (0x14, 0x14, 0x14)
        FG = (0xF4, 0xF4, 0xF2)
        ACCENT_TEXT = (0xF4, 0x33, 0x3B)
        FG_DIM = (0x8E, 0x8E, 0x8E)
        FG_FAINT = (0x7E, 0x7E, 0x7E)
        CARD_FILL = (0, 0, 0)
        CARD_LINE = (0x26, 0x26, 0x26)
        CARD_DOT = (20, 20, 20)
        INACTIVE = (0x7E, 0x7E, 0x7E)
        TRACK = (0x26, 0x26, 0x26)
        FOOTER = (0x7E, 0x7E, 0x7E)


def font(path, size_dp):
    return ImageFont.truetype(path, d(size_dp))


def doto(weight, size_dp):
    return ImageFont.truetype(os.path.join(FONT_DIR, "doto_%s.ttf" % weight), d(size_dp))


def arabic(size_dp, bold=False):
    """A real RAQM font, so the preview shapes Arabic exactly like Android."""
    return ImageFont.truetype(ARABIC_B if bold else ARABIC, d(size_dp),
                              layout_engine=ImageFont.Layout.RAQM)


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
    step = d(spacing)
    r = d(radius)
    for gy in range(step, h + step, step):
        for gx in range(step, w + step, step):
            draw.ellipse([gx - r, gy - r, gx + r, gy + r],
                         fill=(tone, tone, tone))


def rounded(draw, box, radius, fill=None, outline=None, width=1):
    draw.rounded_rectangle(box, radius=d(radius), fill=fill, outline=outline,
                           width=d(width) if outline else 0)


def progress_dots(draw, left, right, y, fraction, rtl=False,
                  radius_dp=2.0, pitch_dp=9.0):
    """Mirror ProgressDots.java: evenly spaced 2 dp dots, completed in accent.

    The same helper drives the app rail and the widget previews, so a
    percentage looks identical everywhere. Dots are inset by one radius at
    each end and fill from the right when the layout is RTL.
    """
    radius = d(radius_dp)
    pitch = d(pitch_dp)
    start = left + radius
    end = right - radius
    usable = (end - start) - radius * 2
    if usable <= 0:
        count = 1
    else:
        step = max(pitch, radius * 2 + d(1))
        count = max(1, min(48, int(usable // step) + 1))
    if fraction <= 0:
        completed = 0
    elif fraction >= 1:
        completed = count
    else:
        completed = max(0, min(count, int(math.floor(count * fraction + 0.5))))
    cy = y + radius
    for index in range(count):
        if count <= 1:
            cx = (start + end) / 2
        else:
            first = start + radius
            last = end - radius
            cx = first + (last - first) * index / (count - 1)
        if completed >= count:
            lit = True
        elif completed <= 0:
            lit = False
        else:
            lit = index >= count - completed if rtl else index < completed
        draw.ellipse([cx - radius, cy - radius, cx + radius, cy + radius],
                     fill=ACCENT if lit else TRACK)


def _line_h(fnt):
    """Android line height in dp for a one-line TextView (no font padding)."""
    ascent, descent = fnt.getmetrics()
    return (ascent + descent) / S


def _text_width(dr, text, fnt, tracking=0.0):
    if not tracking:
        return dr.textlength(text, font=fnt)
    widths = [dr.textlength(ch, font=fnt) for ch in text]
    gap = tracking * fnt.size
    return sum(widths) + gap * max(0, len(text) - 1)


def _line_px(dr, x, top_px, text, fnt, fill, tracking=0.0, align="l"):
    """Draw one TextView line whose line box starts at top_px."""
    ascent, _ = fnt.getmetrics()
    baseline = top_px + ascent
    if tracking:
        widths = [dr.textlength(ch, font=fnt) for ch in text]
        gap = tracking * fnt.size
        total = sum(widths) + gap * max(0, len(text) - 1)
        if align == "r":
            x -= total
        elif align == "m":
            x -= total / 2
        for ch, width in zip(text, widths):
            dr.text((x, baseline), ch, font=fnt, fill=fill, anchor="ls")
            x += width + gap
    else:
        anchor = {"l": "ls", "r": "rs", "m": "ms"}[align]
        dr.text((x, baseline), text, font=fnt, fill=fill, anchor=anchor)


def _line(dr, x, top_dp, text, fnt, fill, tracking=0.0, align="l"):
    _line_px(dr, x, d(top_dp), text, fnt, fill, tracking, align)


def _mix(fg, bg, alpha):
    return tuple(round(bg[i] + (fg[i] - bg[i]) * alpha) for i in range(3))


def qibla_row(dr, L, R, y, rtl):
    """The Qibla entry row, mirroring activity_main.xml in both directions."""
    h = d(68)
    rounded(dr, [L, y, R, y + h], 20, fill=SURFACE_HIGH, outline=LINE,
            width=1)
    cy = y + d(34)
    cx = (R - d(28)) if rtl else (L + d(28))

    # ic_qibla: a 24 dp vector with a 9 dp ring and a packed needle.
    icon_r = d(9)
    dr.ellipse([cx - icon_r, cy - icon_r, cx + icon_r, cy + icon_r],
               outline=FG, width=max(1, d(1.25)))
    dr.polygon([(cx, cy - d(6)), (cx + d(3.2), cy + d(1)),
                (cx, cy - d(0.2)), (cx - d(3.2), cy + d(1))], fill=FG)

    label_font = arabic(8.5, True) if rtl else doto("bold", 10)
    label_h = _line_h(label_font)
    top = y + (h - d(label_h + 5 + label_h)) / 2
    edge = (R - d(54)) if rtl else (L + d(54))
    align = "r" if rtl else "l"
    _line_px(dr, edge, top, "القبلة" if rtl else "QIBLA", label_font, FG,
             0.0 if rtl else 0.18, align)
    _line_px(dr, edge, top + d(label_h + 5),
             "افتح البوصلة" if rtl else "OPEN COMPASS", label_font,
             FG_FAINT, 0.0 if rtl else 0.14, align)

    bearing_font = doto("bold", 14)
    bearing_h = _line_h(bearing_font)
    bearing_top = cy - d(bearing_h) / 2
    if rtl:
        _line_px(dr, L + d(38), bearing_top, "136°", bearing_font, FG, 0.04,
                 "l")
    else:
        _line_px(dr, R - d(38), bearing_top, "136°", bearing_font, FG, 0.04,
                 "r")

    # ic_chevron (16 dp), auto-mirrored: points towards the end.
    chev_box = (L + d(14)) if rtl else (R - d(30))
    chev = [(9, 5), (16, 12), (9, 19), (7.6, 17.6), (13.2, 12), (7.6, 6.4)]
    pts = []
    for vx, vy in chev:
        px = 24.0 - vx if rtl else vx
        pts.append((chev_box + d(px * 16.0 / 24.0),
                    cy - d(8) + d(vy * 16.0 / 24.0)))
    dr.polygon(pts, fill=FG_DIM)


APP_COPY = {
    False: {
        "tagline": "PRAYER CLOCK / OFFLINE",
        "now": "NOW",
        "date": "THU / 22 OCT 2026",
        "hijri": "11 JUMADA AL-AWWAL 1448",
        "city": "CAIRO",
        "detail": "CAIRO · 30.044°N / 31.236°E",
        "next": "NEXT PRAYER",
        "hero": "MAGHRIB",
        "countdown": "01:04",
        "in": "IN",
        "since": "SINCE ASR 15:51",
        "progress": "56% ELAPSED",
        "today": "TODAY",
        "clock_tag": "24H",
        "prayers": [("01", "FAJR", "05:34"),
                    ("02", "DHUHR", "12:40"),
                    ("03", "ASR", "15:51"),
                    ("04", "MAGHRIB", "18:17"),
                    ("05", "ISHA", "19:35")],
        "status": "NEXT",
        "tile_label": "CURRENT LOCATION",
        "tile_city": "CAIRO",
        "tile_coords": "30.044°N / 31.236°E",
        "hint": "GPS FIX · TAP TO REFRESH",
        "qibla_bearing": "136°",
        "add": "ADD WIDGET",
        "settings": "SETTINGS",
        "footer": "Calculated on device · No ads · No tracking",
        "version": "DOTPRAY 1.3",
    },
    True: {
        "tagline": "ساعة الصلاة / دون اتصال",
        "now": "الآن",
        "date": "الخميس / 22 أكتوبر 2026",
        "hijri": "11 جمادى الأولى 1448",
        "city": "القاهرة",
        "detail": "القاهرة · 30.044°ش / 31.236°ق",
        "next": "الصلاة القادمة",
        "hero": "المغرب",
        "countdown": "01:04",
        "in": "بعد",
        "since": "منذ العصر 15:51",
        "progress": "مضى 56%",
        "today": "اليوم",
        "clock_tag": "24 س",
        "prayers": [("01", "الفجر", "05:34"),
                    ("02", "الظهر", "12:40"),
                    ("03", "العصر", "15:51"),
                    ("04", "المغرب", "18:17"),
                    ("05", "العشاء", "19:35")],
        "status": "التالية",
        "tile_label": "الموقع الحالي",
        "tile_city": "القاهرة",
        "tile_coords": "30.044°ش / 31.236°ق",
        "hint": "تحديد GPS · اضغط للتحديث",
        "qibla_bearing": "136°",
        "add": "أضف ويدجت",
        "settings": "الإعدادات",
        "footer": "يُحسب على الجهاز · بلا إعلانات · بلا تتبّع",
        "version": "دوتبراي 1.3",
    },
}


def _render_app_screen(rtl, path=None):
    """Mirror activity_main.xml in either language, with real view metrics."""
    W, H = d(360), d(1240)
    img = Image.new("RGB", (W, H), BG)
    dr = ImageDraw.Draw(img)
    dot_field(dr, W, H, alpha=18)

    L = d(22)
    R = W - d(22)
    copy = APP_COPY[rtl]

    # Text sizes and faces come from values/dimens.xml and values-ar/dimens.xml.
    if rtl:
        f_brand = doto("black", 26)
        f_label = arabic(8.5, True)
        f_label_faint = f_label
        f_numeric = doto("bold", 10)
        f_location = arabic(12)
        f_hero = arabic(22, True)
        f_countdown = doto("regular", 22)
        f_pname = arabic(14, True)
        f_button = arabic(10, True)
    else:
        f_brand = doto("black", 26)
        f_label = doto("bold", 10)
        f_label_faint = doto("bold", 10)
        f_numeric = doto("bold", 10)
        f_location = doto("regular", 15)
        f_hero = doto("bold", 30)
        f_countdown = doto("regular", 22)
        f_pname = doto("bold", 18)
        f_button = doto("bold", 12)

    f_clock = doto("regular", 12)
    f_hero_time = doto("bold", 60)
    f_ptime = doto("bold", 24)
    f_status = arabic(8, True) if rtl else doto("bold", 9)
    # dateLabel/hijriLabel auto-size to their own maxima; the hijri line is
    # one step smaller than the Gregorian date on both locales.
    f_hijri = arabic(8, True) if rtl else doto("bold", 9)
    track_label = 0.0 if rtl else 0.18
    track_faint = 0.0 if rtl else 0.14
    track_name = 0.0 if rtl else 0.04

    brand_h = _line_h(f_brand)
    tag_h = _line_h(f_label_faint)
    clock_h = _line_h(f_clock)
    clock_label_h = _line_h(f_label_faint)
    chip_h = 8 + _line_h(f_label) + 8
    date_h = _line_h(f_label)
    hijri_h = _line_h(f_hijri)
    hero_header_h = max(10.0, _line_h(f_label))
    hero_name_h = _line_h(f_hero)
    hero_time_h = _line_h(f_hero_time)
    count_h = _line_h(f_countdown)
    since_h = _line_h(f_label)
    row_index_h = _line_h(f_numeric)
    row_name_h = _line_h(f_pname)
    row_time_h = _line_h(f_ptime)
    row_status_h = _line_h(f_status)
    row_block_h = row_time_h + 2 + row_status_h
    row_h = 13 + max(row_name_h, row_block_h) + 13
    tile_value_h = _line_h(f_location)
    button_text_h = _line_h(f_button)

    # ── brand + clock ─────────────────────────────────────────────────
    y = 20.0
    brand_top = y
    tag_top = brand_top + brand_h + 7
    clock_block_h = clock_label_h + 4 + clock_h
    header_h = max(brand_h + 7 + tag_h, clock_block_h)
    clock_top = y + header_h - clock_block_h

    _line(dr, L, brand_top, "DOTPRAY", f_brand, FG, 0.05)
    _line(dr, L, tag_top, copy["tagline"], f_label_faint, FG_FAINT,
          track_faint)
    _line(dr, R, clock_top, copy["now"], f_label_faint, FG_FAINT,
          track_faint, "r")
    _line(dr, R, clock_top + clock_label_h + 4, "17:12", f_clock, FG_DIM,
          0.06, "r")
    y += header_h

    # ── location + date ───────────────────────────────────────────────
    y += 16
    row_top = y
    pill_h = chip_h
    pill_top = row_top + (48 - pill_h) / 2
    city_w = _text_width(dr, copy["city"], f_label, track_label)
    pill_w = d(12 + 6 + 8 + 14) + city_w
    pill_l = (R - pill_w) if rtl else L
    rounded(dr, [pill_l, d(pill_top), pill_l + pill_w,
                 d(pill_top + pill_h)], 100, fill=SURFACE, outline=LINE,
            width=1)

    dot_cx = (pill_l + pill_w - d(15)) if rtl else (pill_l + d(15))
    dot_cy = d(pill_top + 8 + _line_h(f_label) / 2)
    dr.ellipse([dot_cx - d(3), dot_cy - d(3), dot_cx + d(3), dot_cy + d(3)],
               fill=ACCENT)
    city_text_x = (dot_cx - d(11)) if rtl else (dot_cx + d(11))
    _line(dr, city_text_x, pill_top + 8, copy["city"], f_label, FG,
          track_label, "r" if rtl else "l")

    dates_h = date_h + 6 + hijri_h
    date_top = row_top + (48 - dates_h) / 2
    date_x = L if rtl else R
    date_align = "l" if rtl else "r"
    _line(dr, date_x, date_top, copy["date"], f_label, FG_DIM, track_label,
          date_align)
    _line(dr, date_x, date_top + date_h + 6, copy["hijri"], f_hijri,
          FG_FAINT, track_faint, date_align)
    y = row_top + 48

    y += 7
    _line(dr, R if rtl else L, y, copy["detail"], f_label_faint, FG_FAINT,
          track_faint, "r" if rtl else "l")
    y += _line_h(f_label_faint)

    # ── next prayer card ──────────────────────────────────────────────
    y += 16
    card_top = y
    inner_top = card_top + 18
    header_text_top = inner_top + (hero_header_h - _line_h(f_label)) / 2
    name_top = inner_top + hero_header_h + 14
    time_top = name_top + hero_name_h + 2
    count_top = time_top + hero_time_h + 8
    count_row_h = max(_line_h(f_label), count_h)
    progress_top = count_top + count_row_h + 16
    since_top = progress_top + 6 + 9
    card_bottom = since_top + since_h + 18
    rounded(dr, [L, d(card_top), R, d(card_bottom)], 26, fill=SURFACE,
            outline=LINE, width=1)

    # PulseDotView: a 10 dp frame with a 2.2 dp dot and a soft ring.
    dot_cx = (R - d(25)) if rtl else (L + d(25))
    dot_cy = d(inner_top + hero_header_h / 2)
    dr.ellipse([dot_cx - d(2.2), dot_cy - d(2.2),
                dot_cx + d(2.2), dot_cy + d(2.2)], fill=ACCENT)
    dr.ellipse([dot_cx - d(3.5), dot_cy - d(3.5),
                dot_cx + d(3.5), dot_cy + d(3.5)],
               outline=_mix(ACCENT, SURFACE, 0.28), width=max(1, d(1)))

    header_x = (R - d(20) - d(10) - d(9)) if rtl else (L + d(20) + d(10) + d(9))
    _line(dr, header_x, header_text_top, copy["next"], f_label, FG_DIM,
          track_label, "r" if rtl else "l")
    _line(dr, (L + d(20)) if rtl else (R - d(20)), header_text_top, "04 / 05",
          f_numeric, FG_DIM, 0.18, "l" if rtl else "r")

    _line(dr, (R - d(20)) if rtl else (L + d(20)), name_top, copy["hero"],
          f_hero, FG, track_name, "r" if rtl else "l")
    _line(dr, (R - d(20)) if rtl else (L + d(20)), time_top, "18:17",
          f_hero_time, FG, 0.0, "r" if rtl else "l")

    label_top = count_top + (count_row_h - _line_h(f_label)) / 2
    label_w = _text_width(dr, copy["in"], f_label, track_label)
    if rtl:
        _line(dr, R - d(20), label_top, copy["in"], f_label, ACCENT_TEXT,
              track_label, "r")
        _line(dr, R - d(20) - label_w - d(8), count_top, copy["countdown"],
              f_countdown, FG, 0.06, "r")
    else:
        _line(dr, L + d(20), label_top, copy["in"], f_label, ACCENT_TEXT,
              track_label, "l")
        _line(dr, L + d(20) + label_w + d(8), count_top, copy["countdown"],
              f_countdown, FG, 0.06, "l")

    progress_dots(dr, L + d(20), R - d(20), d(progress_top + 1), 0.56,
                  rtl=rtl)
    _line(dr, (R - d(20)) if rtl else (L + d(20)), since_top,
          copy["since"], f_label, FG_DIM, track_label, "r" if rtl else "l")
    _line(dr, (L + d(20)) if rtl else (R - d(20)), since_top,
          copy["progress"], f_label, FG_DIM, track_label,
          "l" if rtl else "r")

    # ── today divider ─────────────────────────────────────────────────
    y = card_bottom + 24
    today_h = _line_h(f_label)
    today_w = _text_width(dr, copy["today"], f_label, track_label)
    tag_w = _text_width(dr, copy["clock_tag"], f_label_faint, track_faint)
    line_y = y + today_h / 2
    if rtl:
        _line(dr, R, y, copy["today"], f_label, FG, track_label, "r")
        _line(dr, L, y, copy["clock_tag"], f_label_faint, FG_FAINT,
              track_faint, "l")
        line_start = L + tag_w + d(12)
        line_end = R - today_w - d(12)
    else:
        _line(dr, L, y, copy["today"], f_label, FG, track_label, "l")
        _line(dr, R, y, copy["clock_tag"], f_label_faint, FG_FAINT,
              track_faint, "r")
        line_start = L + today_w + d(12)
        line_end = R - tag_w - d(12)
    dr.line([(line_start, d(line_y)), (line_end, d(line_y))], fill=LINE,
            width=max(1, d(1)))
    y += today_h

    # ── today's prayers ───────────────────────────────────────────────
    y += 12
    for i, (index, name, time) in enumerate(copy["prayers"]):
        active = i == 3
        past = i < 3
        ink = FG if (active or not past) else FG_FAINT
        idx_color = ACCENT_TEXT if active else FG_DIM
        rounded(dr, [L, d(y), R, d(y + row_h)], 18, fill=SURFACE_HIGH,
                outline=(_mix(ACCENT, SURFACE_HIGH, 0.15) if active
                         else LINE_SOFT), width=1)
        if active:
            mcx = (R - d(15)) if rtl else (L + d(15))
            mcy = y + row_h / 2
            dr.ellipse([mcx - d(3), d(mcy - 3), mcx + d(3), d(mcy + 3)],
                       fill=ACCENT)

        _line(dr, (R - d(26)) if rtl else (L + d(26)),
              y + (row_h - row_index_h) / 2, index, f_numeric, idx_color,
              0.18, "r" if rtl else "l")
        _line(dr, (R - d(56)) if rtl else (L + d(56)),
              y + (row_h - row_name_h) / 2, name, f_pname, ink, track_name,
              "r" if rtl else "l")
        time_top = y + (row_h - row_block_h) / 2
        _line(dr, (L + d(16)) if rtl else (R - d(16)), time_top, time,
              f_ptime, ink, 0.0, "l" if rtl else "r")
        if active:
            _line(dr, (L + d(16)) if rtl else (R - d(16)),
                  time_top + row_time_h + 2, copy["status"], f_status,
                  ACCENT_TEXT, track_label, "l" if rtl else "r")

        y += row_h
        if i != len(copy["prayers"]) - 1:
            y += 8

    # ── current location tile ─────────────────────────────────────────
    y += 22
    tile_top = y
    tile_label_top = tile_top + 16
    tile_value_top = tile_label_top + _line_h(f_label) + 11
    tile_coords_top = tile_value_top + tile_value_h + 7
    tile_hint_top = tile_coords_top + _line_h(f_label_faint) + 12
    tile_bottom = tile_hint_top + _line_h(f_label_faint) + 16
    rounded(dr, [L, d(tile_top), R, d(tile_bottom)], 20, fill=SURFACE_HIGH,
            outline=LINE, width=1)
    tile_x = (R - d(16)) if rtl else (L + d(16))
    tile_align = "r" if rtl else "l"
    _line(dr, tile_x, tile_label_top, copy["tile_label"], f_label, FG_DIM,
          track_label, tile_align)
    _line(dr, tile_x, tile_value_top, copy["tile_city"], f_location, FG,
          0.0 if rtl else 0.06, tile_align)
    _line(dr, tile_x, tile_coords_top, copy["tile_coords"], f_label,
          FG_DIM, track_label, tile_align)
    _line(dr, tile_x, tile_hint_top, copy["hint"], f_label, FG,
          track_label, tile_align)
    y = tile_bottom

    # ── qibla ─────────────────────────────────────────────────────────
    y += 10
    qibla_row(dr, L, R, d(y), rtl)
    y += 68

    # ── actions ───────────────────────────────────────────────────────
    y += 22
    gap = d(10)
    add_w = int(round((R - L - gap) * 0.58))
    if rtl:
        add_box = [R - add_w, d(y), R, d(y + 52)]
        ghost_box = [L, d(y), R - add_w - gap, d(y + 52)]
    else:
        add_box = [L, d(y), L + add_w, d(y + 52)]
        ghost_box = [L + add_w + gap, d(y), R, d(y + 52)]
    rounded(dr, add_box, 100, fill=FG)
    rounded(dr, ghost_box, 100, fill=SURFACE, outline=LINE, width=1)
    button_top = y + (52 - button_text_h) / 2
    _line(dr, (add_box[0] + add_box[2]) / 2, button_top, copy["add"],
          f_button, BG, 0.0 if rtl else 0.14, "m")
    _line(dr, (ghost_box[0] + ghost_box[2]) / 2, button_top,
          copy["settings"], f_button, FG, 0.0 if rtl else 0.14, "m")
    y += 52

    # ── footer ────────────────────────────────────────────────────────
    y += 24
    _line(dr, W / 2, y, copy["footer"], f_label_faint, FG_FAINT,
          track_faint, "m")
    y += _line_h(f_label_faint)
    y += 8
    _line(dr, W / 2, y, copy["version"], f_label_faint, FG_FAINT,
          track_faint, "m")
    y += _line_h(f_label_faint) + 40

    if y > H / S:
        print("warning: app preview content ends at %.1f dp" % y)

    path = path or os.path.join(
        OUT_DIR, "app-screen-ar.png" if rtl else "app-screen.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("wrote " + os.path.relpath(path, PROJECT))


def render_app(path=None):
    """English, dark (or the current palette) home screen."""
    _render_app_screen(False, path)


def render_app_arabic(path=None):
    """Arabic RTL home screen with Alexandria copy."""
    _render_app_screen(True, path)
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


def render_widget_wide(width_dp=326, height_dp=160, path=None, drawable=None):
    W, H = d(width_dp), d(height_dp)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    _card(dr, img, W, H)

    DW, DH = d(326), d(160)
    layer = Image.new("RGBA", (DW, DH), (0, 0, 0, 0))
    cd = ImageDraw.Draw(layer)

    pad = d(16)
    header = d(17)
    name_base = d(44)
    time_base = d(84.5)
    count_base = d(103.5)
    right = DW - pad
    clock_base = d(44)
    greg_base = d(84.5)
    hijri_base = d(103.5)
    divider = d(114)
    sched_name = d(132)
    sched_time = d(150)

    f_10b = doto("bold", 10)
    f_95b = doto("bold", 9.5)
    f_85b = doto("bold", 8.5)
    f_8b = doto("bold", 8)
    f_22 = doto("bold", 22)
    f_35 = doto("bold", 35)
    f_14 = doto("regular", 14)
    f_15 = doto("bold", 15)
    f_10 = doto("bold", 10)
    f_98b = doto("bold", 9.8)

    # Four shared rows: clock with the prayer name, Gregorian date with the
    # prayer time, Hijri date with the countdown.
    cd.ellipse([pad, header - d(6.5), pad + d(6), header - d(0.5)], fill=ACCENT)
    _spaced_px(cd, (pad + d(13), header), "NEXT PRAYER", f_10b, d(10), FG_DIM, 0.18)
    _spaced_px(cd, (pad, name_base), "MAGHRIB", f_22, d(22), FG, 0.04)
    _spaced_px(cd, (pad, time_base), "18:17", f_35, d(35), FG, 0.0)

    count_text = "01:04"
    label_w = sum(cd.textlength(ch, font=f_98b) for ch in "IN") + d(9.8) * 0.12
    gap = d(5)
    _spaced_px(cd, (pad, count_base), "IN", f_98b, d(9.8), ACCENT_TEXT, 0.12)
    cd.text((pad + label_w + gap, count_base), count_text, font=f_14, fill=FG,
            anchor="ls")

    _spaced_px(cd, (right, header), "CAIRO", f_95b, d(9.5), FG_FAINT, 0.12,
               "right")
    _spaced_px(cd, (right, clock_base), "17:12", f_22, d(22), FG_DIM, 0.0,
               "right")
    _spaced_px(cd, (right, greg_base), "THU 22 OCT", f_85b, d(8.5), FG_DIM, 0.08,
               "right")
    _spaced_px(cd, (right, hijri_base), "11 JUM I 1448", f_8b, d(8), FG_FAINT, 0.08,
               "right")

    # schedule: five equal cells, each label and time centred. Equal cells
    # give every prayer the same weight and keep the strip symmetric; the
    # divider is the segmented progress rail, 2 dp dots at a 9 dp pitch.
    progress_dots(cd, pad, DW - pad, divider, 0.56)
    names = [("FAJR", "05:34"), ("DHUHR", "12:40"), ("ASR", "15:51"),
             ("MAGHRIB", "18:17"), ("ISHA", "19:35")]
    cell = (DW - pad * 2) / len(names)
    for i, (n, t) in enumerate(names):
        active = i == 3
        cx = pad + cell * (i + 0.5)
        _spaced_px(cd, (cx, sched_name), n, f_10, d(10),
                   ACCENT_TEXT if active else FG_FAINT, 0.08, "center")
        cd.text((cx, sched_time), t, font=f_15,
                fill=FG if active else INACTIVE, anchor="ms")

    _compose(img, layer, DW, DH)

    if path:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
        print("wrote " + os.path.relpath(path, PROJECT))
    if drawable:
        os.makedirs(drawable, exist_ok=True)
        img.save(os.path.join(drawable, "widget_preview.png"))
        print("wrote " + os.path.relpath(os.path.join(drawable, "widget_preview.png"), PROJECT))


def render_widget_small(width_dp=150, height_dp=150, path=None, drawable=None,
                        rail=True, drawable_name="widget_preview_small.png"):
    W, H = d(width_dp), d(height_dp)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    _card(dr, img, W, H)

    DW, DH = d(150), d(150)
    layer = Image.new("RGBA", (DW, DH), (0, 0, 0, 0))
    cd = ImageDraw.Draw(layer)

    center = DW / 2
    name_base = d(56)
    time_base = d(92)
    count_base = d(110)

    f_20 = doto("bold", 20)
    f_35 = doto("bold", 35)
    f_12 = doto("regular", 12)
    f_9b = doto("bold", 9)

    _spaced_px(cd, (center, name_base), "MAGHRIB", f_20, d(20), FG, 0.04, "center")
    _spaced_px(cd, (center, time_base), "18:17", f_35, d(35), FG, 0.0, "center")

    count = "01:04"
    label_w = (sum(cd.textlength(ch, font=f_9b) for ch in "IN")
               + d(9) * 0.12)
    gap = d(5)
    value_w = sum(cd.textlength(ch, font=f_12) for ch in count)
    left = center - (label_w + gap + value_w) / 2
    _spaced_px(cd, (left, count_base), "IN", f_9b, d(9), ACCENT_TEXT, 0.12)
    cd.text((left + label_w + gap, count_base), count, font=f_12, fill=FG,
            anchor="ls")

    # The small card has the same dotted elapsed rail as the wide card, under
    # the countdown. 56% matches the sample state used by the wide preview.
    # The minimal card is the same stack without this rail.
    if rail:
        progress_dots(cd, d(16), d(134), d(128), 0.56)

    _compose(img, layer, DW, DH)

    if path:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
        print("wrote " + os.path.relpath(path, PROJECT))
    if drawable:
        os.makedirs(drawable, exist_ok=True)
        target = os.path.join(drawable, drawable_name)
        img.save(target)
        print("wrote " + os.path.relpath(target, PROJECT))


def render_widget_minimal(width_dp=150, height_dp=150, path=None, drawable=None):
    render_widget_small(width_dp=width_dp, height_dp=height_dp, path=path,
                        drawable=drawable, rail=False,
                        drawable_name="widget_preview_minimal.png")


# ─────────────────────────────────────────────────────────────── qibla screen
def _qibla_blend(color, alpha):
    """Alpha-composite a palette colour over the current page background."""
    return tuple(int(round(BG[i] + (color[i] - BG[i]) * alpha / 255.0))
                 for i in range(3))


def _polar(cx, cy, radius, degrees):
    rad = math.radians(degrees)
    return cx + math.sin(rad) * radius, cy - math.cos(rad) * radius


def _compass_line(dr, cx, cy, r1, r2, degrees, color, width_px):
    x1, y1 = _polar(cx, cy, r1, degrees)
    x2, y2 = _polar(cx, cy, r2, degrees)
    dr.line([x1, y1, x2, y2], fill=color, width=width_px)


def _draw_qibla_compass(dr, cx, cy, radius, heading, qibla, live, aligned, rtl):
    """Mirror QiblaCompassView: rings, dot scale, ticks, needle, lettering."""
    red = ACCENT_TEXT
    fg_ring = _qibla_blend(FG, 190)
    line_soft = _qibla_blend(FG, 55)
    line_whisper = _qibla_blend(FG, 30)
    line_hub = _qibla_blend(FG, 60)
    tick_major = _qibla_blend(FG, 175)
    tick_minor = _qibla_blend(FG, 70)
    tick_north = _qibla_blend(FG, 235)
    dot_color = _qibla_blend(FG, 55)

    # Concentric rings
    for fraction, width, color in (
            (1.00, 1.2, fg_ring),
            (0.92, 1.0, line_soft),
            (0.70, 1.0, line_whisper),
            (0.46, 1.0, line_hub)):
        r = radius * fraction
        dr.ellipse([cx - r, cy - r, cx + r, cy + r], outline=color,
                   width=max(1, d(width)))

    # 5° dot markers, leaving the 10° spokes to the tick lines.
    rr = max(1, int(round(d(0.55))))
    for degrees in range(5, 360, 10):
        x, y = _polar(cx, cy, radius * 0.958, degrees)
        dr.ellipse([x - rr, y - rr, x + rr, y + rr], fill=dot_color)

    # 10° minor spokes
    for degrees in range(10, 360, 10):
        if degrees % 30 == 0:
            continue
        _compass_line(dr, cx, cy, radius * 0.935, radius * 0.985, degrees,
                      tick_minor, max(1, d(0.8)))

    # 30° major spokes
    for degrees in range(0, 360, 30):
        _compass_line(dr, cx, cy, radius * 0.900, radius * 0.985, degrees,
                      tick_major, max(1, d(1.25)))

    # North reads longer than every other mark.
    _compass_line(dr, cx, cy, radius * 0.885, radius, 0,
                  tick_north, max(1, d(1.45)))

    # Qibla needle. The aligned state thickens the needle, ring and dot,
    # exactly like setAligned() animates them to in the app.
    alignment = 1.0 if (live and aligned) else 0.0
    dial_heading = heading if live else 0.0
    needle_angle = qibla - dial_heading
    stem = radius * 0.84
    x1, y1 = _polar(cx, cy, radius * 0.025, needle_angle)
    x2, y2 = _polar(cx, cy, stem, needle_angle)
    dr.line([x1, y1, x2, y2], fill=red,
            width=max(1, d(1.4 + 0.4 * alignment)))

    tip_r = radius * 0.885
    tx, ty = _polar(cx, cy, tip_r, needle_angle)
    ring_r = d(6.2 + 1.2 * alignment)
    dr.ellipse([tx - ring_r, ty - ring_r, tx + ring_r, ty + ring_r],
               outline=red, width=max(1, d(1.2)))
    dot_r = d(2.5 + 0.5 * alignment)
    dr.ellipse([tx - dot_r, ty - dot_r, tx + dot_r, ty + dot_r], fill=red)

    # Upright cardinal letters; the needle passes behind their clear discs.
    labels = [("N", 0.0, True), ("E", 90.0, False),
              ("S", 180.0, False), ("W", 270.0, False)]
    if rtl:
        labels = [("\u0634", 0.0, True), ("\u0642", 90.0, False),
                  ("\u062c", 180.0, False), ("\u063a", 270.0, False)]
    for label, bearing, primary in labels:
        if rtl:
            face = arabic(12 if primary else 10.5, primary)
        else:
            face = doto("bold", 12 if primary else 10)
        angle = bearing - dial_heading
        x, y = _polar(cx, cy, radius * 0.80, angle)
        width = dr.textlength(label, font=face)
        ascent, descent = face.getmetrics()
        mask = max(d(7.5) if primary else d(6.5),
                   max(width / 2, (ascent + descent) / 2) + d(1.4))
        dr.ellipse([x - mask, y - mask, x + mask, y + mask], fill=BG)
        dr.text((x, y), label, font=face,
                fill=FG if primary else _qibla_blend(FG_DIM, 225),
                anchor="mm")

    # Centre pivot
    pivot = d(3.4)
    dr.ellipse([cx - pivot, cy - pivot, cx + pivot, cy + pivot], fill=BG)
    dr.ellipse([cx - pivot, cy - pivot, cx + pivot, cy + pivot],
               outline=_qibla_blend(FG_DIM, 150), width=max(1, d(1)))
    dr.ellipse([cx - d(1.1), cy - d(1.1), cx + d(1.1), cy + d(1.1)], fill=FG)

    if live:
        # Fixed index at the top: the phone's heading.
        index_color = red if aligned else _qibla_blend(FG, 235)
        dr.polygon([
            (cx - d(4.2), cy - radius - d(9.5)),
            (cx + d(4.2), cy - radius - d(9.5)),
            (cx, cy - radius - d(1.2)),
        ], fill=index_color)

    if live and aligned:
        # A short red rim segment marks the aligned state.
        dr.arc([cx - radius, cy - radius, cx + radius, cy + radius],
               264, 276, fill=red, width=max(1, d(2.2)))


def _qibla_pair(dr, label, value, label_font, value_font, x, side, rtl,
                label_track, value_track, label_top, value_top):
    """One footer stat pair, mirrored like the real two-weight row."""
    lw = _text_width(dr, label, label_font, label_track)
    vw = _text_width(dr, value, value_font, value_track)
    gap = d(8)
    if rtl and side == "right":
        # The heading group hugs the right edge: label first, value left of it.
        lx, vx = x - lw, x - lw - gap - vw
        la, va = "r", "r"
    elif rtl:
        # The offset group hugs the left edge: value at the margin, label right.
        vx, lx = x, x + vw + gap
        la, va = "l", "l"
    elif side == "left":
        # LTR heading group: label at the margin, value to its right.
        lx, vx = x, x + lw + gap
        la, va = "l", "l"
    else:
        # LTR offset group: value at the margin, label to its left.
        vx, lx = x - vw, x - vw - gap - lw
        la, va = "r", "r"
    _line_px(dr, lx, label_top, label, label_font, FG_FAINT, label_track, la)
    _line_px(dr, vx, value_top, value, value_font, FG, value_track, va)


def render_qibla(path=None, rtl=False, heading=214.0, qibla=136.0,
                 aligned=False, live=True):
    W, H = d(360), d(800)
    img = Image.new("RGB", (W, H), BG)
    dr = ImageDraw.Draw(img)
    dot_field(dr, W, H, alpha=18)

    L = d(22)
    R = W - d(22)
    content_top = 12.0
    header_h = 56.0
    header_bottom = content_top + header_h
    divider_dp = header_bottom + 2
    place_top = divider_dp + 1 + 10

    # Text faces and tracking come from the real styles.
    if rtl:
        title_font = arabic(17, True)
        subtitle_font = arabic(8.5, True)
        label_font = arabic(8.5, True)
        place_font = arabic(8.5, True)
        state_font = arabic(11, True)
        footer_font = arabic(8.5, True)
        title_track = 0.0
        label_track = 0.0
        state_track = 0.0
    else:
        title_font = doto("bold", 21)
        subtitle_font = doto("bold", 10)
        label_font = doto("bold", 10)
        place_font = doto("bold", 10)
        state_font = doto("bold", 13)
        footer_font = doto("bold", 10)
        title_track = 0.04
        label_track = 0.14
        state_track = 0.18

    value_font = doto("bold", 68)
    stat_font = doto("bold", 15)

    # ── header: 48 dp back frame, 6 dp gap, centred title block ───────
    title_h = _line_h(title_font)
    subtitle_h = _line_h(subtitle_font)
    block_h = title_h + 4 + subtitle_h
    block_top = content_top + (header_h - block_h) / 2
    if rtl:
        text_start = R - d(54)
        title_align = subtitle_align = "r"
    else:
        text_start = L + d(54)
        title_align = subtitle_align = "l"
    _line_px(dr, text_start, d(block_top), "\u0627\u0644\u0642\u0628\u0644\u0629"
             if rtl else "QIBLA", title_font, FG, title_track, title_align)
    _line_px(dr, text_start, d(block_top + title_h + 4),
             "\u0628\u0648\u0635\u0644\u0629" if rtl else "COMPASS",
             subtitle_font, FG_FAINT, label_track, subtitle_align)

    label_h = _line_h(label_font)
    tolerance_top = content_top + (header_h - label_h) / 2
    _line_px(dr, L if rtl else R, d(tolerance_top),
             "\u062f\u0642\u0629 3\u00b0" if rtl else "TOLERANCE 3\u00b0",
             label_font, FG_FAINT, label_track, "l" if rtl else "r")

    # ic_back, auto-mirrored inside its 48 dp frame.
    box_x = (R - d(35)) if rtl else (L + d(13))
    box_y = d(content_top + 17)
    arrow = [(15.4, 5.4), (14.0, 4.0), (6.0, 12.0), (14.0, 20.0),
             (15.4, 18.6), (8.8, 12.0)]
    pts = []
    for ax, ay in arrow:
        px = 22.0 - ax if rtl else ax
        pts.append((box_x + d(px * 22.0 / 24.0),
                    box_y + d(ay * 22.0 / 24.0)))
    dr.polygon(pts, fill=FG)

    dr.line([L, d(divider_dp), R, d(divider_dp)], fill=LINE,
            width=max(1, d(1)))

    # ── place line ────────────────────────────────────────────────────
    _line_px(dr, R if rtl else L, d(place_top),
             "\u0627\u0644\u0642\u0627\u0647\u0631\u0629 \u00b7 30.044\u00b0\u0634 / "
             "31.236\u00b0\u0642" if rtl else
             "CAIRO \u00b7 30.044\u00b0N / 31.236\u00b0E",
             place_font, FG_FAINT, label_track, "r" if rtl else "l")

    # ── real bottom-up readout metrics ────────────────────────────────
    value_h = _line_h(value_font)
    stat_h = _line_h(stat_font)
    stats_h = max(label_h, stat_h)
    state_h = max(6.0, _line_h(state_font))
    footer_h = _line_h(footer_font) + 2
    readout_h = (label_h + 2 + value_h + 6 + 1 + 12 + stats_h + 14
                 + state_h + 14 + footer_h)
    readout_top = 800.0 - 16.0 - readout_h
    compass_top = place_top + _line_h(place_font) + 2
    compass_bottom = readout_top - 8
    radius = min((R - L) / S, compass_bottom - compass_top) / 2 - 11.0
    cx = W / 2
    cy = d((compass_top + compass_bottom) / 2)
    _draw_qibla_compass(dr, cx, cy, d(radius), heading, qibla, live, aligned, rtl)

    # ── bearing readout ───────────────────────────────────────────────
    bearing_label = "\u0627\u062a\u062c\u0627\u0647 \u0627\u0644\u0642\u0628\u0644\u0629" \
        if rtl else "QIBLA BEARING"
    _line_px(dr, cx, d(readout_top), bearing_label, label_font, FG_FAINT,
             label_track, "m")

    value_top = readout_top + label_h + 2
    _line_px(dr, cx, d(value_top), "%03d\u00b0" % int(round(qibla)),
             value_font, FG, 0.0, "m")

    readout_line = value_top + value_h + 6
    dr.line([L, d(readout_line), R, d(readout_line)], fill=LINE,
            width=max(1, d(1)))

    stats_top = readout_line + 1 + 12
    label_y = stats_top + (stats_h - label_h) / 2
    stat_y = stats_top + (stats_h - stat_h) / 2
    heading_text = "%03d\u00b0" % int(round(heading)) if live else "---\u00b0"
    offset = _shortest_delta(heading, qibla) if live else 0
    offset_text = "%03d\u00b0" % int(round(abs(offset))) if live else "---\u00b0"
    heading_label = "\u0627\u062a\u062c\u0627\u0647 \u0627\u0644\u0647\u0627\u062a\u0641" \
        if rtl else "HEADING"
    offset_label = "\u0627\u0644\u0641\u0631\u0642" if rtl else "OFFSET"
    if rtl:
        _qibla_pair(dr, heading_label, heading_text, label_font, stat_font, R,
                    "right", True, label_track, 0.04, d(label_y), d(stat_y))
        _qibla_pair(dr, offset_label, offset_text, label_font, stat_font, L,
                    "left", True, label_track, 0.04, d(label_y), d(stat_y))
    else:
        _qibla_pair(dr, heading_label, heading_text, label_font, stat_font, L,
                    "left", False, label_track, 0.04, d(label_y), d(stat_y))
        _qibla_pair(dr, offset_label, offset_text, label_font, stat_font, R,
                    "right", False, label_track, 0.04, d(label_y), d(stat_y))

    # ── state row ─────────────────────────────────────────────────────
    if live and aligned:
        state_text = "\u0645\u062a\u0648\u0627\u0641\u0642" if rtl else "ALIGNED"
        state_color = ACCENT_TEXT
    elif live:
        if offset >= 0:
            state_text = "\u0627\u0646\u0639\u0637\u0641 \u064a\u0645\u064a\u0646\u064b\u0627" \
                if rtl else "TURN RIGHT"
        else:
            state_text = "\u0627\u0646\u0639\u0637\u0641 \u064a\u0633\u0627\u0631\u064b\u0627" \
                if rtl else "TURN LEFT"
        state_color = FG
    else:
        state_text = "\u0627\u0644\u0642\u0628\u0644\u0629 \u0641\u0642\u0637" \
            if rtl else "BEARING ONLY"
        state_color = FG

    state_top = stats_top + stats_h + 14
    state_text_w = _text_width(dr, state_text, state_font, state_track)
    group = d(6) + d(8) + state_text_w
    start = W / 2 - group / 2
    state_text_y = d(state_top + (state_h - _line_h(state_font)) / 2)
    dot_cy = d(state_top + state_h / 2)
    if rtl:
        _line_px(dr, start + state_text_w, state_text_y, state_text,
                 state_font, state_color, state_track, "r")
        dot_cx = start + group - d(3)
    else:
        _line_px(dr, start + d(6) + d(8), state_text_y, state_text,
                 state_font, state_color, state_track, "l")
        dot_cx = start + d(3)
    dr.ellipse([dot_cx - d(3), dot_cy - d(3), dot_cx + d(3), dot_cy + d(3)],
               fill=ACCENT if aligned else FG_FAINT)

    # ── footer ────────────────────────────────────────────────────────
    footer_top = state_top + state_h + 14
    footer_text = ("\u0623\u0645\u0633\u0643 \u0627\u0644\u0647\u0627\u062a\u0641 "
                   "\u0623\u0641\u0642\u064a\u064b\u0627 \u00b7 \u0623\u062f\u0631\u0647 "
                   "\u0628\u0628\u0637\u0621") if rtl else "HOLD FLAT \u00b7 TURN SLOWLY"
    _line_px(dr, cx, d(footer_top), footer_text, footer_font, FG_FAINT,
             label_track, "m")

    path = path or os.path.join(OUT_DIR, "qibla-screen.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    print("wrote " + os.path.relpath(path, PROJECT))


def _shortest_delta(heading, bearing):
    delta = (bearing - heading) % 360.0
    if delta > 180.0:
        delta -= 360.0
    if delta <= -180.0:
        delta += 360.0
    return delta


if __name__ == "__main__":
    set_palette(False)
    render_app()
    render_app_arabic()
    render_widget_wide(path=os.path.join(OUT_DIR, "widget-wide.png"),
                       drawable=os.path.join(RES, "drawable-night-nodpi"))
    render_widget_small(path=os.path.join(OUT_DIR, "widget-small.png"),
                        drawable=os.path.join(RES, "drawable-night-nodpi"))
    render_widget_minimal(path=os.path.join(OUT_DIR, "widget-minimal.png"),
                          drawable=os.path.join(RES, "drawable-night-nodpi"))
    render_qibla(path=os.path.join(OUT_DIR, "qibla-screen.png"),
                 heading=214.0, qibla=136.0, aligned=False)
    render_qibla(path=os.path.join(OUT_DIR, "qibla-screen-aligned.png"),
                 heading=136.0, qibla=136.0, aligned=True)
    render_qibla(path=os.path.join(OUT_DIR, "qibla-screen-ar.png"),
                 rtl=True, heading=214.0, qibla=136.0, aligned=False)

    set_palette(True)
    render_widget_wide(drawable=os.path.join(RES, "drawable-nodpi"))
    render_widget_small(drawable=os.path.join(RES, "drawable-nodpi"))
    render_widget_minimal(drawable=os.path.join(RES, "drawable-nodpi"))
    render_app(path=os.path.join(OUT_DIR, "app-screen-light.png"))
    render_qibla(path=os.path.join(OUT_DIR, "qibla-screen-light.png"),
                 heading=214.0, qibla=136.0, aligned=False)
