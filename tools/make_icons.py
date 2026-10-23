#!/usr/bin/env python3
"""Regenerate the Dotpray launcher and notification icons.

The coloured mark comes from design/dotpray-icon.svg; the monochrome status-bar
silhouette is design/dotpray-notification.svg (vectorised in
res/drawable/ic_notification.xml). Requires rsvg-convert (librsvg2-bin) and
Pillow. The generated PNGs are committed, so the Android build itself does not
need either tool.
"""

import re
import shutil
import subprocess
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SVG = ROOT / "design" / "dotpray-icon.svg"
RES = ROOT / "app" / "src" / "main" / "res"
PREVIEW = ROOT / "preview"

DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

MASTER = 1024
FOREGROUND = 432
FOREGROUND_CONTENT = 270  # 66/108 safe zone, measured against the icon canvas
NOTIFICATION_LARGE = 256

RESAMPLING = Image.Resampling.LANCZOS


def render(svg_text: str, size: int, output: Path, background: str | None = None) -> None:
    rsvg = shutil.which("rsvg-convert")
    if not rsvg:
        raise SystemExit("rsvg-convert is required (apt install librsvg2-bin)")
    with tempfile.NamedTemporaryFile("w", suffix=".svg", delete=False) as handle:
        handle.write(svg_text)
        source = Path(handle.name)
    command = [rsvg, "-w", str(size), "-h", str(size), "-o", str(output)]
    if background:
        command += ["-b", background]
    command.append(str(source))
    try:
        subprocess.run(command, check=True)
    finally:
        source.unlink(missing_ok=True)


def circle_mask(size: int) -> Image.Image:
    scale = 4
    mask = Image.new("L", (size * scale, size * scale), 0)
    ImageDraw.Draw(mask).ellipse(
        (0, 0, size * scale - 1, size * scale - 1), fill=255
    )
    return mask.resize((size, size), RESAMPLING)


def main() -> None:
    svg_text = SVG.read_text(encoding="utf-8")

    with tempfile.TemporaryDirectory() as tmp:
        tmpdir = Path(tmp)
        master_path = tmpdir / "master.png"
        render(svg_text, MASTER, master_path, background="#000000")
        master = Image.open(master_path).convert("RGBA")

        # The SVG's near-black rectangle is the sujood figure's negative
        # space. Removing it gives a transparent coloured mark for the
        # adaptive foreground; the adaptive background supplies the black.
        mark_svg = re.sub(r'\n<rect[^>]*fill="#(?:000000|050505)"[^>]*/>', "", svg_text)
        mark_path = tmpdir / "mark.png"
        render(mark_svg, MASTER, mark_path)
        mark = Image.open(mark_path).convert("RGBA")

    # Legacy square icons.
    for density, size in DENSITIES.items():
        icon = master.resize((size, size), RESAMPLING)
        icon.save(RES / f"mipmap-{density}" / "ic_launcher.png")

    # Legacy round icons.
    mask = circle_mask(MASTER)
    round_master = Image.new("RGBA", (MASTER, MASTER), (0, 0, 0, 0))
    round_master.paste(master, (0, 0), mask)
    for density, size in DENSITIES.items():
        icon = round_master.resize((size, size), RESAMPLING)
        icon.save(RES / f"mipmap-{density}" / "ic_launcher_round.png")

    # Adaptive foreground: transparent mark, kept inside the 66/108 safe zone.
    # 432 px is 108 dp at xxxhdpi; lower densities scale it down.
    foreground = Image.new("RGBA", (FOREGROUND, FOREGROUND), (0, 0, 0, 0))
    content = mark.resize((FOREGROUND_CONTENT, FOREGROUND_CONTENT), RESAMPLING)
    offset = (FOREGROUND - FOREGROUND_CONTENT) // 2
    foreground.alpha_composite(content, (offset, offset))
    foreground_dir = RES / "drawable-xxxhdpi"
    foreground_dir.mkdir(parents=True, exist_ok=True)
    foreground.save(foreground_dir / "ic_launcher_foreground.png")

    # Coloured logo used as the notification's large icon.
    large = master.resize((NOTIFICATION_LARGE, NOTIFICATION_LARGE), RESAMPLING)
    large_dir = RES / "drawable-nodpi"
    large_dir.mkdir(parents=True, exist_ok=True)
    large.save(large_dir / "ic_notification_large.png")

    # Human-readable previews.
    PREVIEW.mkdir(exist_ok=True)
    master.save(PREVIEW / "app-icon.png")
    round_master.save(PREVIEW / "app-icon-round.png")

    adaptive = Image.new("RGBA", (FOREGROUND, FOREGROUND), (0, 0, 0, 255))
    adaptive.alpha_composite(foreground, (0, 0))
    mask = Image.new("L", (FOREGROUND, FOREGROUND), 0)
    mask_size = int(round(FOREGROUND * 72 / 108))
    mask_offset = (FOREGROUND - mask_size) // 2
    ImageDraw.Draw(mask).ellipse(
        (mask_offset, mask_offset, mask_offset + mask_size - 1, mask_offset + mask_size - 1),
        fill=255,
    )
    adaptive.putalpha(mask)
    adaptive.save(PREVIEW / "app-icon-adaptive.png")


if __name__ == "__main__":
    main()
