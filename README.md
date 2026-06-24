# Domus

A native, Material 3 Expressive client for [Home Assistant](https://www.home-assistant.io/),
built with Kotlin Multiplatform. The official Home Assistant app is a WebView wrapper around
the web frontend — Domus is Android-first instead, with a shared Kotlin core that's also
designed to back a future Desktop app, the way [Keyguard](https://github.com/keyguardapp/keyguard)
does for Bitwarden.

## Why

The stock app trades performance and platform integration (dynamic color, predictive back,
widgets, etc.) for being a thin wrapper over the dashboard UI. Domus keeps the same backend
(your existing Home Assistant instance, REST + WebSocket APIs) but renders everything natively.

## Architecture

```
domus/
├── shared/       KMP module: HA API client, models, repository — no UI
├── androidApp/   Android app — Jetpack Compose, Material 3 Expressive
└── desktopApp/   Compose Multiplatform desktop app
```

- **`shared`** targets `androidTarget` and `jvm("desktop")`. It owns everything that isn't
  UI: the Ktor-based REST client (`api/HaRestApi.kt`), the WebSocket client for realtime
  `state_changed` events (`api/HaWebSocketClient.kt`), a repository that merges both into a
  single `StateFlow` (`data/HaRepository.kt`), domain models (`model/HaEntity.kt`), and
  `DesignTokens` — the seed color/spacing/shape values both UIs build their themes from.
- **`androidApp`** is a regular Android app using `androidx.compose.material3` directly (not
  Compose Multiplatform), so it picks up Material 3 Expressive components as they ship and
  gets full dynamic color (min SDK 31, so it's always available — no version branching).
- **`desktopApp`** is a Compose Multiplatform for Desktop app. It has no wallpaper to derive
  dynamic color from, so its theme is seeded from the same `DesignTokens.SEED_COLOR_ARGB`
  Android falls back to, keeping the two UIs visually consistent without sharing Compose code.

Each app owns its own UI, navigation, and view models — there's no shared Compose layer.
Consistency comes from both consuming the same `shared` module and design tokens.

## Requirements

- JDK 17+
- Android SDK (set `sdk.dir` in a local `local.properties`, or have `ANDROID_HOME` set —
  this file is gitignored since it's machine-specific)
- Android Studio (recommended for the Android app) and/or IntelliJ IDEA (for the desktop app)

## Building & running

```sh
# Android debug build
./gradlew :androidApp:assembleDebug

# Run the desktop app
./gradlew :desktopApp:run
```

## Status

Early scaffold. The connect/dashboard screens are placeholders; `HaRepository` is wired up
in `shared` but not yet connected to either app's UI, and there's no token persistence yet.
See commit history for what's landed so far.

## License

TBD.
