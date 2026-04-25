# FireSleep — a real sleep timer for your TV, driven by the Fire TV remote

Most TVs hide their sleep timer in a settings menu you can only reach with the
TV's own remote. If you watch through a Fire TV Stick, that means juggling two
remotes in the dark — and if you forget, the TV stays on all night.

FireSleep is a sideloaded Fire TV app that adds a proper sleep timer **and**
actually powers the TV off, all from the Fire TV remote you already have. No
second remote, no cloud, no account: your TV, your Pi, your LAN.

## How it works

```
Fire TV remote → FireSleep app (android/) → HTTP POST → bridge on Pi (server/) → TV powers off
```

The Fire TV app owns the timer (`AlarmManager` behind a foreground service, so
it survives backgrounding and doze). The Pi owns the TV-vendor bits (pairing,
WebSocket chatter). Adding a new TV brand is a change to `server/`, not the
app.

## Tested

- **LG webOS** (UR78 / webOS 23) — works end-to-end.
- **Samsung** — works end-to-end.
- **Fire TV Stick 4K Max** — primary dev target; should work on any Fire TV
  (minSdk 22).

PRs welcome for Sony, Vizio, Hisense, Roku TVs — drop a sibling bridge file
next to `server/bridge.py` and document pairing in `server/README.md`.

## Quick start

```bash
# 1. Run the bridge on a Pi (or any Linux box on your LAN)
cd server && docker compose up -d --build
#    open http://<pi>:8765/ to set the TV host and pair

# 2. Build the Fire TV app
cd android && ./gradlew assembleDebug

# 3. Sideload onto the Fire TV
adb connect <FIRE_TV_IP>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On first launch the app asks for the Pi's LAN IP. That's it.

## Triple-press shortcut (optional)

FireSleep ships with an Accessibility Service that lets you bring up the timer
from any app — Netflix, YouTube, etc. — by triple-pressing the **≡ Menu**
button on the Fire TV remote.

Fire TV's Settings UI sometimes hides third-party accessibility services. If
the toggle doesn't appear under Settings → Accessibility, enable it once over
ADB:

```bash
adb shell settings put secure enabled_accessibility_services com.firesleep.app/com.firesleep.app.access.FireSleepAccessibilityService
adb shell settings put secure accessibility_enabled 1
```

## Repo layout

```
android/   # Sideloaded Fire TV app (Kotlin, Jetpack Compose for TV)
server/    # Raspberry Pi bridge (FastAPI + per-vendor code) + web admin UI
design/    # Pixel-level design spec
```

See **[`android/README.md`](android/README.md)** for build details and
**[`server/README.md`](server/README.md)** for pairing, Docker, and systemd
setup.

## Security

- App permissions: `INTERNET` only. No storage, location, camera, or mic.
- Cleartext HTTP is allowed app-wide (Android's network-security-config can't
  restrict to a CIDR), but the app only ever talks to the one user-entered LAN
  IP on `:8765`.
- The bridge has no remote auth — it trusts whoever can reach it on the LAN.
  Don't port-forward `8765`.
- Nothing leaves your LAN. No cloud, no analytics, no phone-home.
