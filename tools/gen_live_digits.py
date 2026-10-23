#!/usr/bin/env python3
"""Generate the white Doto digit-pair vectors used by the live widget fields.

RemoteViews cannot load an app font in the launcher process, but it can load
ordinary vector drawables.  The widget paints the static text into its bitmap
with the real Doto font and uses these 60 glyph pairs for the seconds field,
which is flipped by a single ViewFlipper inside the launcher.

A single pair flipper (rather than separate tens and units flippers) matters:
the launcher starts a flipper's timer when it is attached, so a 10-second tens
flipper would be out of phase with the wall clock.  A one-second, 60-child
flipper counts continuously and wraps correctly.

The paths are copied from Doto (SIL Open Font License).  They are white so the
widget can tint them with the current foreground colour at bind time.  Both
weights are needed: the wall clock is bold, the countdown is regular.
"""
import os
from fontTools.misc.transform import Transform
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FONT_DIR = os.path.join(ROOT, "app", "src", "main", "res", "font")
OUT = os.path.join(ROOT, "app", "src", "main", "res", "drawable")

TEMPLATE = '''<?xml version="1.0" encoding="utf-8"?>
<!-- Generated from Doto {weight} (SIL Open Font License); do not edit by hand. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="12dp"
    android:height="{height_dp}dp"
    android:viewportWidth="1200"
    android:viewportHeight="{height}">
    <group>
        <path
            android:fillColor="#FFFFFFFF"
            android:pathData="{tens}" />
    </group>
    <group
        android:translateX="600">
        <path
            android:fillColor="#FFFFFFFF"
            android:pathData="{ones}" />
    </group>
</vector>
'''

FACES = (
    ("regular", "doto_regular.ttf"),
    ("bold", "doto_bold.ttf"),
)


def digit_paths(font):
    glyphs = font.getGlyphSet()
    cmap = font.getBestCmap()
    bounds_pen = BoundsPen(glyphs)
    glyphs[cmap[ord("0")]].draw(bounds_pen)
    _, y_min, _, y_max = bounds_pen.bounds
    # Flip the font's y axis so the baseline lands at the bottom of the
    # viewport; the glyph then occupies 0..height exactly.
    baseline = y_max
    height = y_max - y_min
    paths = {}
    for ch in "0123456789":
        name = cmap[ord(ch)]
        pen = SVGPathPen(
            glyphs,
            ntos=lambda value: ("%.2f" % value).rstrip("0").rstrip("."),
        )
        glyphs[name].draw(TransformPen(pen, Transform(1, 0, 0, -1, 0, baseline)))
        paths[ch] = pen.getCommands()
    return height, paths


def main():
    for weight, filename in FACES:
        font = TTFont(os.path.join(FONT_DIR, filename))
        height, paths = digit_paths(font)
        for value in range(60):
            tens = paths["%d" % (value // 10)]
            ones = paths["%d" % (value % 10)]
            target = os.path.join(OUT, "live_pair_%s_%02d.xml" % (weight, value))
            with open(target, "w", encoding="utf-8") as handle:
                handle.write(TEMPLATE.format(
                    weight=weight.capitalize(),
                    height=height,
                    height_dp=round(height / 100.0, 2),
                    tens=tens,
                    ones=ones,
                ))
        print("wrote 60", weight, "pairs")


if __name__ == "__main__":
    main()
