# FireSleep — Android TV app

Single-module Kotlin + Jetpack Compose app, sideloaded onto a Fire TV Stick.
Depends only on Compose, androidx.security, and OkHttp. Designed for every
Fire TV Stick generation (minSdk 22); tested on a Fire TV Stick 4K Max.

The app is deliberately **TV-brand-agnostic** — it makes one authenticated
HTTP POST to a helper service on your LAN. All vendor-specific logic
(pairing, WebSocket chatter, etc.) lives in `../server/`. Tested end-to-end
with an LG webOS set; PRs welcome for other brands (see top-level README).

## Modules

```
app/src/main/java/com/firesleep/app/
├── MainActivity.kt            # Hosts Compose, routes Home ↔ Custom ↔ Confirm ↔ Overlay
├── ui/
│   ├── Theme.kt               # Design tokens (colors, typography)
│   ├── Moon.kt                # Crescent-moon glyph (Canvas)
│   ├── Modifiers.kt           # drawLeftBorder etc.
│   ├── HomeScreen.kt          # 5 presets + Custom row, D-pad ▲▼● navigation
│   ├── CustomScreen.kt        # Hours/minutes reels, ◀▶ switch, ▲▼ adjust
│   ├── ConfirmScreen.kt       # "45 min — TV off at 11:47 PM" flash
│   ├── OverlayScreen.kt       # Last-60s corner card with snooze/cancel
│   └── SettingsScreen.kt      # First-run Pi IP + token input
├── timer/
│   ├── TimerController.kt     # AlarmManager scheduling, shared state
│   ├── TimerReceiver.kt       # Warn-at-T-60 and expire-at-T broadcasts
│   └── PowerOffService.kt     # Foreground service, executes the HTTP call
├── net/
│   └── TvClient.kt            # OkHttp: GET /health, POST /poweroff
└── prefs/
    └── SecurePrefs.kt         # EncryptedSharedPreferences wrapper
```

## Build

```bash
cd android
./gradlew assembleDebug
```

(Requires JDK 17+. First build pulls Gradle 8.9 via the wrapper.)

## Install

```bash
adb connect <FIRE_TV_IP>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Permissions used

| Permission                       | Why                                      |
|----------------------------------|------------------------------------------|
| `INTERNET`                       | POST to the helper on your LAN           |
| `WAKE_LOCK`                      | Keep the device awake during countdown   |
| `FOREGROUND_SERVICE`             | Host the timer while app is backgrounded |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Exact timer via AlarmManager    |
| `POST_NOTIFICATIONS`             | Foreground-service notification          |

No storage, location, Bluetooth, camera, or mic. HTTP is restricted to
RFC1918 LAN ranges via `network_security_config.xml`.

## Focus model

All four screens use a single `Modifier.focusable().onKeyEvent { … }` at the
root of the screen. Key events are dispatched to the focused screen
container, which updates an internal "which row / which column is focused"
index. No per-element `focusable()` calls inside the ladders or reels, which
means nothing can "lose" focus when the state updates.

`Key.DirectionCenter`, `Key.Enter`, and `Key.NumPadEnter` all fire the select
action — Fire TV remotes use `DirectionCenter`; USB keyboards during
development use `Enter`.
