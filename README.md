# Pangea for Android

Pangea is an off-grid mesh communication app for Android, built for backcountry users: hikers, overlanders, search-and-rescue volunteers, and anyone who needs to stay connected far from cell service. It connects to LoRa mesh radios over Bluetooth, Wi-Fi, or USB and keeps you messaging, sharing location, and tracking your group when nothing else works.

Pangea is a downstream fork of [Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android). It stays close to upstream for easy syncing — module and file names are intentionally unchanged — while adding a distinct identity and a simplified out-of-box experience.

## What's different from upstream

### Expert Mode

Casual users see a clean settings surface: channels, sharing, display, and user settings. Advanced configuration — LoRa parameters, security, MQTT, serial modules, device power tuning, debug logging, administration — is hidden until **Expert Mode** is switched on in Settings. Sections stay visible with softer names ("Radio" instead of "Radio Configuration"); gated items reappear with an "Expert Mode" caption when enabled. First-run onboarding ends with a screen explaining the split.

### Power Mode

A three-tier battery profile (**Standard / Trail / Expedition**) selectable from Settings or by tapping the nav-bar logo:

- **Standard** — all features on.
- **Trail** — day-trip tuning: phone-position shares to the mesh are rate-limited (≥5 min), background work reduced.
- **Expedition** — multi-day off-grid: phone-GPS sharing off, widget refresh paused, background auto-reconnect off.

Android's own Battery Saver temporarily clamps the effective mode to Expedition. Architecturally, `PowerModeManager` (`core/repository` interface, `core/service` implementation) is the single gate all battery-hungry feature sites consult — new features with background cost should read its `effectiveMode` rather than invent their own switches.

## Getting started

1. Clone: `git clone https://github.com/HabbTech/Pangea-Android.git`
2. Install [Android Studio](https://developer.android.com/studio) (latest stable) and JDK 21.
3. `cp secrets.defaults.properties local.properties` (the google flavor fails without it).
4. Build: `./gradlew assembleFdroidDebug` or open in Android Studio and run the `fdroidDebug` variant.

Before pushing: `./gradlew spotlessApply spotlessCheck detekt assembleDebug test allTests`. See [AGENTS.md](AGENTS.md) for architecture, conventions, and the AI-assisted workflow this repo is set up for.

## Project structure

A Kotlin Multiplatform project producing the Android app (`androidApp/`) and a JVM desktop app (`desktopApp/`) from shared `core/*` and `feature/*` modules. Radio protocol handling, mesh state, and Power Mode gating live in `commonMain` and ship in both apps.

## License

GPL v3. See the [LICENSE](LICENSE) file for details.

Pangea is a downstream fork of [Meshtastic-Android](https://github.com/meshtastic/Meshtastic-Android), © Meshtastic LLC, GPL-3.0. Meshtastic® is a registered trademark of Meshtastic LLC. Credit and gratitude to the upstream maintainers whose work made this possible; app-level bugs unique to Pangea should be reported here rather than upstream.
