"""Builds Pangea-Android's launcher icon from the 2000x2000 logo.

Adaptive icon (minSdk 26, so this is what every launcher shows):
  background  = white (colour resource)
  foreground  = the logo's lines on transparency: grey contours run to the edges, the orange footprint sits inside
                the safe zone (a 66dp circle of the 108dp canvas), so no launcher mask can crop it
  monochrome  = the footprint alone, for Android 13+ themed icons
Legacy ic_launcher / ic_launcher_round and the 512px Play Store image are the same picture, pre-masked.

Needs numpy and Pillow. From the repo root:
  python scripts/make-launcher-icon.py design/logo/pangea-logo.webp androidApp/src/main/res \
      androidApp/src/main/ic_launcher-playstore.png <preview.png>
The preview shows the icon through circle, squircle, rounded-square and teardrop masks, plus the themed version.
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

SRC, RES, PLAYSTORE, PREVIEW = sys.argv[1:5]

# Footprint's farthest point from the centre, in dp of the 108dp canvas. Safe zone is 33dp; ~29dp keeps 4dp clear of it, close to
# the 48dp keyline Android's own icons use, so it reads at home-screen size without crowding any mask.
FOOTPRINT_RADIUS_DP = 29.0
WORK = 2160  # render size for the 108dp canvas (20 px/dp), downscaled per density

src = np.asarray(Image.open(SRC).convert("RGB")).astype(np.float64)
h, w, _ = src.shape
white = np.array([255.0, 255.0, 255.0])

# Ink colours: the most saturated pixels are pure orange; light, low-saturation non-white pixels are the grey lines.
mx, mn = src.max(axis=2), src.min(axis=2)
sat = (mx - mn) / np.maximum(mx, 1)
orange = np.median(src[(sat > 0.75) & (mx > 180)], axis=0)
greyish = (sat < 0.12) & (mx < 236) & (mx > 190)
grey = np.median(src[greyish], axis=0)
print("orange ink", orange.round(), "grey ink", grey.round())

# Each pixel = a*ink + (1-a)*white. Solve a for both inks, keep the ink that explains the pixel best.
d = white - src  # (h, w, 3)
alphas, resid = [], []
for ink in (orange, grey):
    v = white - ink
    a = np.clip((d @ v) / (v @ v), 0.0, 1.0)
    alphas.append(a)
    resid.append(np.linalg.norm(d - a[..., None] * v, axis=2))
use_orange = resid[0] <= resid[1]
alpha = np.where(use_orange, alphas[0], alphas[1])
alpha[alpha < 0.02] = 0.0
is_orange = use_orange & (alpha > 0.0)


def layer(mask_orange, mask_grey):
    rgba = np.zeros((h, w, 4), dtype=np.float64)
    rgba[..., :3] = np.where(is_orange[..., None], orange, grey)
    a = np.where(is_orange, alpha * mask_orange, alpha * mask_grey)
    rgba[..., 3] = a * 255.0
    return Image.fromarray(rgba.round().astype(np.uint8), "RGBA")


logo = layer(1.0, 1.0)
footprint = layer(1.0, 0.0)

# Scale so the footprint's farthest orange pixel from the logo's centre lands on FOOTPRINT_RADIUS_DP.
ys, xs = np.nonzero(is_orange & (alpha > 0.5))
cx, cy = w / 2.0, h / 2.0
r_src = np.sqrt((xs - cx) ** 2 + (ys - cy) ** 2).max()
print(f"orange bbox x {xs.min()}..{xs.max()} y {ys.min()}..{ys.max()}, max radius {r_src:.0f}px of {w}")
scale = (FOOTPRINT_RADIUS_DP / 108.0) * WORK / r_src


def on_canvas(img):
    size = round(w * scale)
    scaled = img.convert("RGBa").resize((size, size), Image.LANCZOS).convert("RGBA")
    canvas = Image.new("RGBA", (WORK, WORK), (0, 0, 0, 0))
    off = (WORK - size) // 2
    canvas.alpha_composite(scaled, (off, off)) if off >= 0 else canvas.alpha_composite(
        scaled.crop((-off, -off, -off + WORK, -off + WORK))
    )
    return canvas


fg = on_canvas(logo)
mono = on_canvas(footprint)
mono_white = Image.new("RGBA", mono.size, (255, 255, 255, 0))
mono_white.putalpha(mono.getchannel("A"))


def down(img, px):
    return img.convert("RGBa").resize((px, px), Image.LANCZOS).convert("RGBA")


def full_icon(px, viewport_dp=72.0):
    """The icon as a launcher shows it: white + foreground, cropped to the central viewport."""
    composed = Image.new("RGBA", (WORK, WORK), (255, 255, 255, 255))
    composed.alpha_composite(fg)
    cut = round(WORK * (108.0 - viewport_dp) / 2 / 108.0)
    return down(composed.crop((cut, cut, WORK - cut, WORK - cut)), px)


def masked(img, shape):
    px = img.size[0]
    m = Image.new("L", (px * 4, px * 4), 0)
    dr = ImageDraw.Draw(m)
    if shape == "circle":
        dr.ellipse((0, 0, px * 4 - 1, px * 4 - 1), fill=255)
    elif shape == "rounded":
        dr.rounded_rectangle((0, 0, px * 4 - 1, px * 4 - 1), radius=px * 4 * 0.22, fill=255)
    elif shape == "squircle":
        n = px * 4
        yy, xx = np.mgrid[0:n, 0:n]
        u, v = (xx - n / 2) / (n / 2), (yy - n / 2) / (n / 2)
        m = Image.fromarray(((np.abs(u) ** 4 + np.abs(v) ** 4) <= 1).astype(np.uint8) * 255, "L")
    elif shape == "teardrop":
        dr.rounded_rectangle((0, 0, px * 4 - 1, px * 4 - 1), radius=px * 2, fill=255)
        dr.rectangle((px * 2, px * 2, px * 4 - 1, px * 4 - 1), fill=255)
    out = img.copy()
    out.putalpha(m.resize((px, px), Image.LANCZOS))
    return out


densities = {"mdpi": 1.0, "hdpi": 1.5, "xhdpi": 2.0, "xxhdpi": 3.0, "xxxhdpi": 4.0}
for name, f in densities.items():
    folder = os.path.join(RES, f"mipmap-{name}")
    adaptive = round(108 * f)
    legacy = round(48 * f)
    down(fg, adaptive).save(os.path.join(folder, "ic_launcher_foreground.webp"), lossless=True, method=6)
    down(mono_white, adaptive).save(os.path.join(folder, "ic_launcher_monochrome.webp"), lossless=True, method=6)
    icon = full_icon(legacy * 4)
    masked(icon, "rounded").resize((legacy, legacy), Image.LANCZOS).save(
        os.path.join(folder, "ic_launcher.webp"), lossless=True, method=6
    )
    masked(icon, "circle").resize((legacy, legacy), Image.LANCZOS).save(
        os.path.join(folder, "ic_launcher_round.webp"), lossless=True, method=6
    )

full_icon(512).convert("RGB").save(PLAYSTORE, optimize=True)

# Preview: each common mask at home-screen size (48dp at xxhdpi = 144px) and small (24dp), plus themed.
shapes = ["circle", "squircle", "rounded", "teardrop"]
tile = 190
sheet = Image.new("RGB", (tile * (len(shapes) + 1), tile * 2 + 30), "#3b4a5a")
for i, s in enumerate(shapes):
    big = masked(full_icon(144), s)
    sheet.paste(big, (i * tile + 23, 20), big)
    small = masked(full_icon(72), s)
    sheet.paste(small, (i * tile + 59, tile + 30), small)
themed = Image.new("RGBA", (144, 144), (0, 0, 0, 0))
bg = masked(Image.new("RGBA", (144, 144), "#1f3b2a"), "circle")
themed.alpha_composite(bg)
tint = Image.new("RGBA", (WORK, WORK), (190, 240, 200, 0))
tint.putalpha(mono.getchannel("A"))
cut = round(WORK * 18 / 108)
tinted = down(tint.crop((cut, cut, WORK - cut, WORK - cut)), 144)
themed.alpha_composite(tinted)
sheet.paste(themed, (len(shapes) * tile + 23, 20), themed)
sheet.save(PREVIEW)
print("done")
