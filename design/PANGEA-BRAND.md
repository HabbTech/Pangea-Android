# Pangea Brand — Design Authority

**Status:** authoritative for brand tokens in this repo (see the two-tier rule in `.skills/design-standards/SKILL.md`).
**Enforcement:** `core/ui/src/commonTest/.../theme/PangeaPaletteTest.kt` — if it fails, the palette was accidentally reverted; restore these values, never update the test.
**Full rationale + measurements:** `../porting/reports/color-brand-recommendation.md` (in the project folder, one level above this repo).

## Brand

Pangea = the supercontinent; dinosaur theme. Brand colour **`#F05511` — "FOSSIL"** in leet (F-0-5-5-1-1). Defined as `PangeaFossil` in `core/ui/.../theme/CustomColors.kt`. Used for brand marks.

## Logo and launcher icon

Source: `design/logo/pangea-logo.webp` (2000×2000): the Fossil footprint in contour lines, with pale grey (`#DBE2D7`) contours around it, on white. The launcher icon matches the iOS app icon: white background, the grey contours running to the edges, and the footprint inside the adaptive-icon safe zone so no launcher mask crops it. The themed (monochrome) icon is the footprint alone. Rebuild every density with `scripts/make-launcher-icon.py`; never hand-crop in Android Studio's Image Asset tool.

## Palette — the Pangea iPhone app's colours (v2)

Android matches the Pangea iPhone app pixel for pixel (Ben, 2026-10-18). The tint is Fossil itself, with white text, in light and dark, as iOS sets `.tint(Color.pangeaPrimary)`. Every other colour is the matching iOS system colour.

| Role | Light | Dark | iOS name |
|---|---|---|---|
| `primary` / `onPrimary` | `#F05511` / `#FFFFFF` | `#F05511` / `#FFFFFF` | `pangeaPrimary` (app tint) |
| `primaryContainer` / `onPrimaryContainer` | `#FFDBCB` / `#3B0900` | `#7A2E04` / `#FFDBCB` | `pangeaPrimaryContainer` |
| `tertiary` | `#2855A8` | `#B0BFF0` | `pangeaTertiary` (Expert Mode caption) |
| `background`, `surface` | `#F2F2F7` | `#000000` | systemGroupedBackground |
| `surfaceContainer*` (cards, rows) | `#FFFFFF` | `#1C1C1E` | secondarySystemGroupedBackground |
| `onSurface` / `onSurfaceVariant` | `#000000` / `#8A8A8E` | `#FFFFFF` / `#8D8D93` | label / secondaryLabel |
| `outline`, `outlineVariant` | `#C6C6C8` | `#38383A` | separator |
| `secondary` | `#8E8E93` | `#8E8E93` | systemGray |
| `error` | `#FF3B30` | `#FF453A` | systemRed |

The iOS-only colours (fills, grouped-row backgrounds, bubble greys) live in `core/ui/.../ios/IosColors`.

**Accessibility trade-off, accepted:** white text on `#F05511` is about 3.5:1. That's below WCAG AA for small text (4.5:1) and passes only for large text (3:1). iOS has the same trade-off, and its design guide notes it. If an accessibility review needs it, darken the filled buttons and your own bubbles in light mode to about `#C4430D` (the v1 value). `porting/reports/ios-look-android-alternatives.md` in the project folder tracks this.

## PangeaFossil (`#F05511`) usage rules

- ✅ The logo's footprint, splash, hero/large graphics, decorative accents.
- ✅ Accent (even text) on dark surfaces — 4.89.
- ✅ Filled chip/badge with dark-rust `#3B0900` text — 4.91.
- ✅ The app tint (v2): buttons, links, toggles, selected tab, your own chat bubbles, with white text, as on iOS. See the accessibility trade-off above.

## Cross-platform

Since v2, Android copies the Pangea iPhone app's look (layout, colours, type, components), not just its hues. iOS defines its colours in `Meshtastic/Extensions/Color.swift`. Where Android deliberately departs from Android conventions to match, `porting/reports/ios-look-android-alternatives.md` (project folder) lists the Android-style option.

## Changelog

- 2026-07-14 — v1: Fossil palette adopted, superseding the interim iOS-blue primary (`56582b3b3`) and inherited Meshtastic green before it. First AA-clean accent palette in the app's history.
- 2026-10-18 — v2: the tint is Fossil `#F05511` with white text in light and dark, and surfaces use iOS system colours, so Android matches the Pangea iPhone app exactly (Ben's decision). Supersedes v1's darkened `#C4430D` / `#FF9F6B` primaries.
