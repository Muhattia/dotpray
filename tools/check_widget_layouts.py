#!/usr/bin/env python3
"""Fail the build if a widget layout uses a class RemoteViews cannot inflate.

RemoteViews only understands a fixed set of view classes. A single <View/> is
enough for the launcher to show "Couldn't add widget", so this guards the
home-screen layouts shared by the three widget variants.
"""
import re, sys, os

ALLOWED = {
    "FrameLayout", "LinearLayout", "RelativeLayout", "GridLayout",
    "AbsoluteLayout", "AnalogClock", "Button", "Chronometer", "ImageButton",
    "ImageView", "ProgressBar", "TextView", "ListView",
    "GridView", "StackView", "AdapterViewFlipper", "AdapterViewAnimator",
    "ViewStub", "ScrollView", "HorizontalScrollView", "RadioGroup",
    "RadioButton", "Switch", "SlidingDrawer",
}

paths = sys.argv[1:] or [
    "app/src/main/res/layout/widget_prayer.xml",
    "app/src/main/res/layout/widget_prayer_small.xml",
    "app/src/main/res/layout/widget_preview_minimal.xml",
]

bad = 0
for path in paths:
    src = open(path, encoding="utf-8").read()
    src = re.sub(r"<!--.*?-->", "", src, flags=re.S)
    for tag in re.findall(r"<([A-Za-z][\w.]*)", src):
        simple = tag.split(".")[-1]
        if simple not in ALLOWED:
            print(f"{path}: {tag} is not RemoteViews-inflatable")
            bad += 1
    print(f"{os.path.basename(path)}: checked")

sys.exit(1 if bad else 0)
