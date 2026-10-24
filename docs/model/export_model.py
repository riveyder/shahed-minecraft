#!/usr/bin/env python3
"""Export the reference geometry to a real editable Blockbench free model.

The default export uses only Python's standard library. Pass --preview to also
render a geometry/texture preview; that optional path needs Pillow, NumPy and
Matplotlib. No Minecraft files or runtime assets are changed by this script.
"""

import argparse
import base64
import json
import os
from pathlib import Path
import uuid

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
GEOMETRY = HERE / "drone-model.json"
TEXTURE = ROOT / "src/main/resources/assets/shahed/textures/entity/drone.png"


def uid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, f"shahed:prototype/{name}"))


def face_uvs(part):
    u, v = part["uv"]
    x, y, z = part["size"]
    # Java ModelPart local Y is down; Blockbench free model Y is up.
    return {
        "west": [u, v + z, u + z, v + z + y],
        "north": [u + z, v + z, u + z + x, v + z + y],
        "east": [u + z + x, v + z, u + 2 * z + x, v + z + y],
        "south": [u + 2 * z + x, v + z, u + 2 * z + 2 * x, v + z + y],
        "down": [u + z, v, u + z + x, v + z],
        "up": [u + z + x, v, u + z + 2 * x, v + z],
    }


def export_blockbench(geometry):
    # Ground is local Java y=8: 24 - rootOffsetY(16) - localY.
    ground_y = 24 - geometry["root_offset"][1]
    elements = []
    for index, part in enumerate(geometry["parts"]):
        x, y, z = part["origin"]
        width, height, depth = part["size"]
        elements.append({
            "name": part["name"],
            "type": "cube",
            "uuid": uid(part["name"]),
            "from": [x, ground_y - y - height, z],
            "to": [x + width, ground_y - y, z + depth],
            "origin": [0, ground_y, 0],
            "rotation": [0, 0, 0],
            "box_uv": False,
            "autouv": 0,
            "color": index % 8,
            "rescale": False,
            "locked": False,
            "export": True,
            "faces": {face: {"uv": uv, "texture": 0}
                      for face, uv in face_uvs(part).items()},
        })
    model = {
        "meta": {"format_version": "4.10", "model_format": "free", "box_uv": False},
        "name": "SHAHED.exe - detailed fixed-wing FPV drone",
        "model_identifier": "shahed_drone_stage_two_polish",
        "visible_box": [5, 3, 0],
        "variable_placeholders": "",
        "resolution": {"width": geometry["texture_size"][0],
                       "height": geometry["texture_size"][1]},
        "elements": elements,
        "outliner": [{
            "name": "drone", "uuid": uid("root"), "origin": [0, ground_y, 0],
            "rotation": [0, 0, 0], "color": 0, "export": True,
            "mirror_uv": False, "isOpen": True,
            "children": [element["uuid"] for element in elements],
        }],
        "textures": [{
            "name": "drone.png", "id": "0", "uuid": uid("texture"),
            "namespace": "shahed", "folder": "entity", "particle": False,
            "render_mode": "default", "render_sides": "double",
            "frame_time": 1, "frame_order_type": "loop", "frame_order": "",
            "frame_interpolate": False, "visible": True, "internal": True,
            "saved": False,
            "source": "data:image/png;base64," + base64.b64encode(TEXTURE.read_bytes()).decode("ascii"),
        }],
        "animations": [],
    }
    (HERE / "drone.bbmodel").write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
    print(f"Exported {len(elements)} editable cubes to docs/model/drone.bbmodel")


def render_preview(geometry):
    os.environ.setdefault("MPLCONFIGDIR", "/tmp/shahed-matplotlib")
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    from matplotlib.patches import Rectangle
    from mpl_toolkits.mplot3d.art3d import Poly3DCollection
    import numpy as np
    from PIL import Image

    texture = np.asarray(Image.open(TEXTURE).convert("RGBA")) / 255.0
    ground_y = 24 - geometry["root_offset"][1]
    polygons, colors = [], []

    def world(x, y, z):
        # Match the renderer at yaw=0: nose points world +Z, up is world +Y.
        return np.array([x, -z, ground_y - y])

    for part in geometry["parts"]:
        x, y, z = part["origin"]
        dx, dy, dz = part["size"]
        face_corners = {
            "up": (world(x, y, z), world(x + dx, y, z), world(x, y, z + dz)),
            "down": (world(x, y + dy, z), world(x + dx, y + dy, z), world(x, y + dy, z + dz)),
            "north": (world(x, y, z), world(x + dx, y, z), world(x, y + dy, z)),
            "south": (world(x + dx, y, z + dz), world(x, y, z + dz), world(x + dx, y + dy, z + dz)),
            "west": (world(x, y, z + dz), world(x, y, z), world(x, y + dy, z + dz)),
            "east": (world(x + dx, y, z), world(x + dx, y, z + dz), world(x + dx, y + dy, z)),
        }
        for face, uv in face_uvs(part).items():
            u0, v0, u1, v1 = uv
            face_pixels = texture[v0:v1, u0:u1]
            if not np.all(face_pixels[:, :, 3] == 1):
                raise ValueError(f"Transparent texture pixels on {part['name']}/{face}")
            origin, end_u, end_v = face_corners[face]
            step_u = (end_u - origin) / (u1 - u0)
            step_v = (end_v - origin) / (v1 - v0)
            shade = {"up": 1, "down": .65, "north": .82, "south": .82,
                     "west": .9, "east": .9}[face]
            for row in range(v1 - v0):
                for column in range(u1 - u0):
                    a = origin + column * step_u + row * step_v
                    polygons.append([a, a + step_u, a + step_u + step_v, a + step_v])
                    rgba = face_pixels[row, column].copy()
                    rgba[:3] *= shade
                    colors.append(rgba)

    plt.rcParams.update({"font.family": "DejaVu Sans", "font.size": 10})
    figure = plt.figure(figsize=(16, 6), facecolor="#f2f5f6")
    ax = figure.add_subplot(131, projection="3d")
    ax.add_collection3d(Poly3DCollection(polygons, facecolors=colors, edgecolors="none"))
    ax.add_collection3d(Poly3DCollection([[[-30,-34,0], [30,-34,0], [30,34,0], [-30,34,0]]],
                                       facecolor="#dbe3e5", edgecolors="#b4c2c6", alpha=.4))
    ax.quiver(0, 31, 0.2, 0, 6, 0, color="#267f78", arrow_length_ratio=.2)
    ax.text(0, 37, 1, "Nose / +Z", color="#267f78")
    ax.set(xlim=(-30, 30), ylim=(-34, 39), zlim=(0, 22),
           xlabel="World X", ylabel="World Z (yaw 0)", zlabel="Height (pixels)")
    ax.set_box_aspect((60,73,22))
    ax.view_init(elev=25, azim=-56)
    ax.set_title("Textured geometry preview")

    top = figure.add_subplot(132)
    for part in sorted(geometry["parts"], key=lambda p: -p["origin"][1]):
        x, y, z = part["origin"]
        dx, dy, dz = part["size"]
        u0, v0, u1, v1 = face_uvs(part)["up"]
        color = texture[v0:v1, u0:u1, :3].mean(axis=(0, 1))
        top.add_patch(Rectangle((x, -z-dz), dx, dz, facecolor=color,
                                edgecolor="#40565d", linewidth=.55))
    top.annotate("Nose / world +Z", xy=(0,30), xytext=(0,36), ha="center",
                 arrowprops={"arrowstyle": "->", "color": "#267f78"}, color="#267f78")
    top.annotate("Rear pusher propeller", xy=(6,-28), xytext=(0,-34), ha="center",
                 arrowprops={"arrowstyle": "->", "color": "#40565d"})
    top.set(xlim=(-30,30), ylim=(-37,39), xlabel="World X (pixels)",
            ylabel="World Z (pixels)", title="Plan view: stepped delta silhouette")
    top.set_aspect("equal")
    top.grid(alpha=.2)

    side = figure.add_subplot(133)
    for part in geometry["parts"]:
        x, y, z = part["origin"]
        dx, dy, dz = part["size"]
        color = "#e4e6df" if "wing" in part["name"] or "nose" in part["name"] else "#839699"
        if "propeller" in part["name"] or "engine" in part["name"]:
            color = "#31484f"
        side.add_patch(Rectangle((-z-dz, ground_y-y-dy), dz, dy,
                                 facecolor=color, edgecolor="#40565d", linewidth=.7))
    side.axhline(0, color="#697f83", linewidth=1)
    side.text(24, .3, "Ground", color="#697f83")
    side.annotate("Resting skids / propeller clearance", xy=(-28,0), xytext=(-26,-3), ha="left",
                  arrowprops={"arrowstyle":"->", "color":"#ad7029"}, color="#ad7029")
    side.annotate("+Z / nose", xy=(34,7), xytext=(23,7), va="center",
                  arrowprops={"arrowstyle":"->", "color":"#267f78"}, color="#267f78")
    side.set(xlim=(-34,38), ylim=(-5,22), xlabel="World Z (pixels)",
             ylabel="Height above ground (pixels)", title="Side view: propeller clears ground")
    side.set_aspect("equal")
    side.grid(alpha=.2)
    figure.suptitle(f"SHAHED.exe - FPV Mayhem | Detailed fixed-wing drone | {len(geometry['parts'])} separate cubes", fontsize=15)
    figure.text(.5, .02, "Offline geometry preview only. Minecraft in-game appearance and yaw still require a client check.",
                ha="center", color="#40565d")
    figure.tight_layout(rect=(0,.05,1,.94))
    figure.savefig(HERE / "drone-preview.png", dpi=160, facecolor=figure.get_facecolor())
    plt.close(figure)
    print(f"Rendered docs/model/drone-preview.png; all {len(geometry['parts']) * 6} cube faces have opaque UV pixels")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preview", action="store_true")
    args = parser.parse_args()
    data = json.loads(GEOMETRY.read_text(encoding="utf-8"))
    export_blockbench(data)
    if args.preview:
        render_preview(data)
