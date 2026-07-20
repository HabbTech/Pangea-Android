# Pangea Brand — Design Authority

**Status:** authoritative for brand tokens in this repo (see the two-tier rule in `.skills/design-standards/SKILL.md`).
**Enforcement:** `core/ui/src/commonTest/.../theme/PangeaPaletteTest.kt` — if it fails, the palette was accidentally reverted; restore these values, never update the test.
**Full rationale + measurements:** `../porting/reports/color-brand-recommendation.md` (in the project folder, one level above this repo).

## Brand

Pangea = the supercontinent; dinosaur theme. Brand colour **`#F05511` — "FOSSIL"** in leet (F-0-5-5-1-1). Defined as `PangeaFossil` in `core/ui/.../theme/CustomColors.kt`. Used as the launcher-icon background and brand marks.

## Palette (M3 primary family) — intentional divergence from Meshtastic green

Precedent: upstream darkened its own brand green (#67EA94 → #2D8F52) because bright brand hues fail WCAG as text. Pangea applies the identical move to Fossil. All ratios measured against surfaces `#F5F6FA` (light) / `#1A1B26` (dark); AA text bar = 4.5:1.

| Role | Light | Ratio | Dark | Ratio |
|---|---|---|---|---|
| `primary` | `#C4430D` | 4.68 vs surface ✅ | `#FF9F6B` | 8.48 vs surface ✅ |
| `onPrimary` | `#FFFFFF` | 5.05 ✅ | `#4A1A00` | 7.24 ✅ |
| `primaryContainer` | `#FFDBCB` | decorative | `#7A2E04` | decorative |
| `onPrimaryContainer` | `#3B0900` | 13.26 ✅ | `#FFDBCB` | 7.32 ✅ |
| `inversePrimary` | `#FF9F6B` | 6.53 vs inverseSurface ✅ | `#C4430D` | — |

`tertiary` stays blue (`#2855A8` / `#B0BFF0`) — carries the Expert Mode captions, AA-clean and visually distinct from primary.

## PangeaFossil (`#F05511`) usage rules

- ✅ Launcher background, splash, hero/large graphics, decorative accents.
- ✅ Accent (even text) on dark surfaces — 4.89.
- ✅ Filled chip/badge with dark-rust `#3B0900` text — 4.91.
- ❌ Never white text on it (3.49). ❌ Never as text on light surfaces (3.23). Icons/borders on light are fine (≥3.0 component bar).

## Cross-platform

One brand, native dialects: iOS uses the same hues via its `AccentColor` asset (`#C4430D` light / `#FF9F6B` dark — see `porting/feature-requests/FR-004-ios-fossil-accent.md` in the project folder); Android/desktop map them through M3 roles. Chrome differs per platform by design; the hue is the brand.

## Changelog

- 2026-07-14 — v1: Fossil palette adopted, superseding the interim iOS-blue primary (`56582b3b3`) and inherited Meshtastic green before it. First AA-clean accent palette in the app's history.
