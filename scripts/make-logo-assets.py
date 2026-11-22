"""Builds the in-app Pangea logo assets from design/logo/pangea-logo.webp.

  ic_meshtastic.xml  - the footprint's outer outline as a single-colour vector (tinted by Icon(); the resource keeps
                       its upstream name so merges stay easy)
  pangea_logo.webp   - the full-colour round logo, for the logo button at the top left of Chats and Settings (as iOS)

Needs numpy, Pillow and scikit-image. From the repo root:
  python scripts/make-logo-assets.py design/logo/pangea-logo.webp core/resources/src/commonMain/composeResources
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw
from skimage import measure

SRC, RES = sys.argv[1:3]
VIEWPORT = 24.0
PAD = 1.0  # viewport units of air around the footprint

rgb = np.asarray(Image.open(SRC).convert("RGB")).astype(np.float64)
h, w, _ = rgb.shape
mx, mn = rgb.max(axis=2), rgb.min(axis=2)
sat = (mx - mn) / np.maximum(mx, 1)
orange = (sat > 0.35) & (rgb[..., 0] > 150)

# Silhouette: everything inside the footprint's outermost line, holes filled.
mask = np.pad(orange, 2).astype(np.uint8)
outer = measure.find_contours(mask, 0.5)
outer = max(outer, key=len)  # the outermost ring is by far the longest
outline = measure.approximate_polygon(outer, tolerance=2.0) - 2  # undo the pad
ys, xs = outline[:, 0], outline[:, 1]
size = max(xs.max() - xs.min(), ys.max() - ys.min())
scale = (VIEWPORT - 2 * PAD) / size
ox = (VIEWPORT - (xs.max() - xs.min()) * scale) / 2 - xs.min() * scale
oy = (VIEWPORT - (ys.max() - ys.min()) * scale) / 2 - ys.min() * scale
pts = [(x * scale + ox, y * scale + oy) for y, x in outline]
path = "M" + " L".join(f"{x:.2f},{y:.2f}" for x, y in pts) + " Z"
print(f"outline: {len(pts)} points")

header = open(os.path.join(RES, "drawable", "ic_meshtastic.xml"), encoding="utf-8").read().split("-->", 1)[0] + "-->"
vector = f"""{header}

<!-- PANGEA BRAND MARK: the footprint's outer outline, traced from design/logo/pangea-logo.webp by
     scripts/make-logo-assets.py (don't hand-edit; rerun the script). Solid so it stays legible at
     nav-bar sizes. Keeps the upstream resource name for merge-friendliness; Icon() tints the fill. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
  <path
      android:fillColor="#FF000000"
      android:pathData="{path}" />
</vector>
"""
open(os.path.join(RES, "drawable", "ic_meshtastic.xml"), "w", encoding="utf-8", newline="\n").write(vector)

# Round full-colour logo: the logo cut to a circle on white, 288px (48dp at 6x headroom).
logo = Image.open(SRC).convert("RGBA")
n = 288
big = logo.resize((n * 4, n * 4), Image.LANCZOS)
circle = Image.new("L", big.size, 0)
ImageDraw.Draw(circle).ellipse((0, 0, big.size[0] - 1, big.size[1] - 1), fill=255)
white = Image.new("RGBA", big.size, (255, 255, 255, 255))
white.alpha_composite(big)
white.putalpha(circle)
white.resize((n, n), Image.LANCZOS).save(os.path.join(RES, "drawable", "pangea_logo.webp"), lossless=True, method=6)
print("done")
