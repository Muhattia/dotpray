#!/usr/bin/env python3
"""Shrink Doto's colon and re-centre it on the digit body.

Doto draws the colon as two large circles, almost three times the diameter of
the dot-matrix dots used by the digits. At hero sizes that makes the separator
compete with the time; at countdown sizes it becomes a heavy blob.

This script scales the colon outline to 45%, lifts it to the vertical centre of
the digit body, and keeps Doto's original 600-unit advance so the normal time
rhythm is unchanged. The glyph keeps its original horizontal centre inside the
cell. It is idempotent: already-shrunk fonts are skipped.

    python3 tools/shrink_colon.py
"""
from pathlib import Path

from fontTools.misc.transform import Transform
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.ttGlyphPen import TTGlyphPen
from fontTools.ttLib import TTFont

ROOT = Path(__file__).resolve().parent.parent
FONTS = (
    ROOT / "app" / "src" / "main" / "res" / "font" / "doto_regular.ttf",
    ROOT / "app" / "src" / "main" / "res" / "font" / "doto_bold.ttf",
)
FACTOR = 0.45
# Original colon widths are 260 (regular) and 282 (bold) units. Anything at or
# below this threshold has already been reduced by this script.
PATCHED_WIDTH = 150
COLON_ADVANCE = 600


def colon_bounds(font):
    glyph_set = font.getGlyphSet()
    name = font.getBestCmap()[ord(":")]
    pen = BoundsPen(glyph_set)
    glyph_set[name].draw(pen)
    return name, pen.bounds


def shrink(path):
    font = TTFont(path)
    name, bounds = colon_bounds(font)
    x0, y0, x1, y1 = bounds
    width = x1 - x0
    changed = False

    if width > PATCHED_WIDTH:
        cx = (x0 + x1) / 2.0
        cy = (y0 + y1) / 2.0

        # A colon belongs in the middle of the digit body, not around its own
        # original centre, which sits low because Doto draws it as two
        # oversized circles hanging below the baseline.
        glyph_set = font.getGlyphSet()
        digit = font.getBestCmap()[ord("0")]
        digit_pen = BoundsPen(glyph_set)
        glyph_set[digit].draw(digit_pen)
        digit_center = (digit_pen.bounds[1] + digit_pen.bounds[3]) / 2.0
        shift = digit_center - cy

        glyph = font["glyf"][name]
        pen = TTGlyphPen(font["glyf"])
        transform = (Transform()
                     .translate(0, shift)
                     .translate(cx, cy)
                     .scale(FACTOR, FACTOR)
                     .translate(-cx, -cy))
        glyph.draw(TransformPen(pen, transform), font["glyf"])
        new_glyph = pen.glyph()
        new_glyph.recalcBounds(font["glyf"])
        font["glyf"][name] = new_glyph
        changed = True
        width = new_glyph.xMax - new_glyph.xMin
        print(f"{path.name}: colon {x1 - x0:.0f} -> {width:.0f} units")

    glyph = font["glyf"][name]
    advance, lsb = font["hmtx"][name]
    if advance != COLON_ADVANCE or lsb != glyph.xMin:
        font["hmtx"][name] = (COLON_ADVANCE, glyph.xMin)
        changed = True
        print(f"{path.name}: advance {advance} -> {COLON_ADVANCE}, "
              f"lsb {lsb} -> {glyph.xMin}")

    if not changed:
        print(f"{path.name}: already patched ({width:.0f} units), skipped")
        return

    font.save(path)
    new_bounds = colon_bounds(TTFont(path))[1]
    print(f"{path.name}: colon now {new_bounds[2] - new_bounds[0]:.0f} units wide")


def main():
    for path in FONTS:
        shrink(path)


if __name__ == "__main__":
    main()
