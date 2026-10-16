#!/usr/bin/env python3
"""Regenerate the original SHAHED.exe prototype pixel art (requires Pillow).

This is a development helper, not part of the Gradle or runtime dependency set.
Run from any directory: python3 docs/model/generate_assets.py
"""

import json
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/shahed"
WHITE = "#e4e6df"
LIGHT = "#b9c3c2"
GRAY = "#839699"
DARK = "#31484f"
INK = "#15272d"
TEAL = "#72ded1"
ORANGE = "#e8a84f"


def save(image, path):
    destination = ASSETS / path
    destination.parent.mkdir(parents=True, exist_ok=True)
    image.save(destination)


def pixel_text(draw, xy, text, color, scale=1):
    glyphs = {
        "A": ["010", "101", "111", "101", "101"],
        "E": ["111", "100", "110", "100", "111"],
        "F": ["111", "100", "110", "100", "100"],
        "K": ["101", "101", "110", "101", "101"],
        "P": ["110", "101", "110", "100", "100"],
        "R": ["110", "101", "110", "101", "101"],
        "V": ["101", "101", "101", "101", "010"],
    }
    x, y = xy
    for char in text:
        for row, bits in enumerate(glyphs[char]):
            for col, bit in enumerate(bits):
                if bit == "1":
                    draw.rectangle((x + col * scale, y + row * scale,
                                    x + (col + 1) * scale - 1,
                                    y + (row + 1) * scale - 1), fill=color)
        x += 4 * scale


def box_uv(draw, uv, size, colors):
    """Minecraft ModelPart.Cube box unwrap, with integer size dimensions."""
    u, v = uv
    x, y, z = size
    top, bottom, side = colors
    # Two depth-by-height sides, two width-by-height sides, top and bottom.
    rectangles = [
        ((u, v + z, u + z - 1, v + z + y - 1), side),
        ((u + z, v + z, u + z + x - 1, v + z + y - 1), side),
        ((u + z + x, v + z, u + z + x + z - 1, v + z + y - 1), side),
        ((u + 2 * z + x, v + z, u + 2 * z + 2 * x - 1, v + z + y - 1), side),
        # ModelPart local minimum Y is the visible top after renderer Y flip.
        ((u + z, v, u + z + x - 1, v + z - 1), bottom),
        ((u + z + x, v, u + z + 2 * x - 1, v + z - 1), top),
    ]
    for rect, color in rectangles:
        draw.rectangle(rect, fill=color)


def entity_texture():
    image = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    surfaces = [
        ((0, 0), (4, 4, 17), (WHITE, GRAY, LIGHT)),
        ((44, 0), (3, 3, 4), (WHITE, GRAY, LIGHT)),
        ((60, 0), (2, 2, 3), (DARK, INK, GRAY)),
        ((0, 24), (5, 1, 10), (WHITE, GRAY, LIGHT)),
        ((32, 24), (4, 1, 8), (WHITE, GRAY, LIGHT)),
        ((58, 24), (4, 1, 4), (WHITE, GRAY, LIGHT)),
        ((76, 24), (1, 5, 4), (WHITE, GRAY, LIGHT)),
        ((0, 38), (3, 3, 4), (DARK, INK, GRAY)),
        ((16, 38), (2, 2, 2), (GRAY, INK, DARK)),
        ((0, 48), (14, 1, 1), (DARK, INK, GRAY)),
        ((32, 48), (1, 14, 1), (DARK, INK, GRAY)),
        ((44, 38), (1, 6, 8), (GRAY, INK, DARK)),
        ((66, 38), (1, 4, 1), (DARK, INK, GRAY)),
    ]
    for uv, size, colors in surfaces:
        box_uv(draw, uv, size, colors)
    # Top fuselage service panels, orientation marks, and status indicator.
    draw.rectangle((22, 3, 23, 7), fill=LIGHT)
    draw.rectangle((22, 10, 23, 13), fill=DARK)
    draw.point((22, 11), fill=TEAL)
    draw.line((21, 16, 24, 16), fill=ORANGE)
    # Tiny original maker mark on both long fuselage sides: K R A (3x3).
    tiny = ["101110010", "110110111", "101101101"]
    for origin in (3, 24):
        for y, row in enumerate(tiny):
            for x, bit in enumerate(row):
                if bit == "1":
                    draw.point((origin + x, 17 + y), fill=DARK)
    # Wing top surface panel seams and identification tips.
    draw.line((15, 29, 19, 29), fill=LIGHT)
    draw.line((15, 32, 19, 32), fill=LIGHT)
    draw.line((44, 29, 47, 29), fill=LIGHT)
    draw.rectangle((66, 24, 69, 24), fill=ORANGE)
    draw.line((81, 24, 81, 27), fill=ORANGE)
    # Engine vents, propeller tips, and antenna end.
    for x in (1, 3, 8, 10):
        draw.line((x, 42, x, 44), fill=INK)
    draw.rectangle((1, 48, 2, 49), fill=ORANGE)
    draw.rectangle((13, 48, 14, 49), fill=ORANGE)
    draw.rectangle((32, 49, 35, 50), fill=ORANGE)
    draw.rectangle((32, 61, 35, 62), fill=ORANGE)
    save(image, "textures/entity/drone.png")


def drone(draw, origin=(0, 0), scale=1):
    ox, oy = origin
    def poly(points, fill):
        draw.polygon([(ox+x*scale, oy+y*scale) for x, y in points], fill=fill)
    # Top-down delta shape, nose points up and the pusher propeller is behind.
    poly([(15,2),(17,2),(19,15),(29,25),(29,27),(18,25),(18,28),
          (14,28),(14,25),(3,27),(3,25),(13,15)], INK)
    poly([(15,3),(16,3),(18,17),(27,25),(18,23),(14,23),(5,25),(14,17)], WHITE)
    poly([(14,14),(16,11),(18,23),(16,26),(14,23)], LIGHT)
    poly([(15,6),(16,6),(17,10),(15,10)], DARK)
    poly([(5,24),(8,23),(8,25),(5,26)], ORANGE)
    poly([(24,23),(27,24),(27,26),(24,25)], ORANGE)
    poly([(15,17),(16,16),(17,22),(15,22)], GRAY)
    poly([(9,28),(23,28),(23,29),(9,29)], DARK)
    poly([(15,26),(17,26),(17,30),(15,30)], GRAY)


def item_textures():
    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    drone(draw)
    save(image, "textures/item/drone.png")

    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.line((7, 11, 4, 2), fill=DARK, width=2)
    draw.line((25, 11, 28, 2), fill=DARK, width=2)
    draw.rounded_rectangle((3, 9, 28, 27), radius=4, fill=INK)
    draw.rounded_rectangle((4, 10, 27, 26), radius=3, fill=GRAY)
    draw.rectangle((11, 11, 20, 17), fill=DARK)
    draw.rectangle((12, 12, 19, 15), fill=TEAL)
    draw.line((13, 14, 15, 13, 16, 14, 18, 12), fill=DARK)
    for x in (8, 23):
        draw.ellipse((x-3, 18, x+3, 24), fill=DARK)
        draw.ellipse((x-1, 19, x+1, 21), fill=WHITE)
    draw.rectangle((14, 20, 17, 24), fill=LIGHT)
    draw.point((15, 21), fill=ORANGE)
    draw.rectangle((5, 11, 7, 12), fill=ORANGE)
    draw.rectangle((24, 11, 26, 12), fill=TEAL)
    save(image, "textures/item/controller.png")

    image = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.line((7, 10, 5, 2), fill=GRAY, width=2)
    draw.line((26, 10, 28, 4), fill=GRAY, width=2)
    draw.rounded_rectangle((1, 10, 30, 24), radius=3, fill=INK)
    draw.rounded_rectangle((4, 8, 27, 25), radius=3, fill=GRAY)
    draw.rounded_rectangle((5, 10, 26, 22), radius=2, fill=DARK)
    draw.rectangle((7, 12, 14, 19), fill=TEAL)
    draw.rectangle((17, 12, 24, 19), fill=TEAL)
    draw.line((8, 12, 12, 12), fill=WHITE)
    draw.line((18, 12, 22, 12), fill=WHITE)
    draw.polygon([(13, 23), (16, 20), (19, 23)], fill=INK)
    draw.rectangle((12, 7, 19, 8), fill=LIGHT)
    draw.rectangle((27, 13, 29, 15), fill=ORANGE)
    save(image, "textures/item/fpv_goggles.png")


def icon():
    image = Image.new("RGBA", (128, 128), INK)
    draw = ImageDraw.Draw(image)
    draw.rectangle((4,4,123,123), outline=GRAY, width=2)
    for coordinate in range(16, 120, 16):
        draw.line((8, coordinate, 119, coordinate), fill="#21373d")
        draw.line((coordinate, 8, coordinate, 119), fill="#21373d")
    drone(draw, origin=(0, 3), scale=4)
    draw.rectangle((12, 10, 41, 31), fill=INK)
    pixel_text(draw, (15,13), "KRA", ORANGE, 3)
    draw.rectangle((77, 100, 116, 120), fill=INK)
    pixel_text(draw, (80,104), "FPV", TEAL, 3)
    save(image, "icon.png")


def item_definitions():
    for name in ("controller", "fpv_goggles", "drone"):
        (ASSETS / "items" / f"{name}.json").write_text(
            json.dumps({"model": {"type": "minecraft:model", "model": f"shahed:item/{name}"}}, indent=2) + "\n",
            encoding="utf-8")
        (ASSETS / "models/item" / f"{name}.json").write_text(
            json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": f"shahed:item/{name}"}}, indent=2) + "\n",
            encoding="utf-8")


if __name__ == "__main__":
    entity_texture()
    item_textures()
    icon()
    item_definitions()
    print("Generated original entity, item, icon, and item-model assets.")
