
---

# Fire TV Sleep Timer — Project Handoff

## Goal
Sideloaded Android app for Amazon Fire TV Stick 4K Max that lets the user set a sleep timer using only the Fire TV remote. When the timer expires, the app calls a helper service on a Raspberry Pi, which powers off an LG webOS TV over the LAN.

## Why this architecture
- **HDMI-CEC doesn't work** for this LG (model UR78, webOS 23). Fire TV goes to standby but the TV stays on showing "No Signal". Tested and confirmed.
- **webOS WebSocket API works reliably.** Already tested with `aiowebostv` from the Pi — TV powers off cleanly.
- Keeping the webOS logic on the Pi (not in the Android app) avoids bundling Python/WebSocket complexity into the APK. Android app just makes one HTTP POST.

## System diagram
```
Fire TV remote
    ↓ (D-pad navigation)
Fire TV Android app (sideloaded, this repo)
    ↓ (timer expires → HTTP POST with token)
Raspberry Pi helper (FastAPI, already built & running)
    ↓ (aiowebostv WebSocket)
LG UR78 TV → powers off
```

## What already exists (do NOT rebuild)

### Raspberry Pi helper — done, running, tested
- **Location:** `~/tv_helper.py` Which will run on the Pi
- **Runs as:** docker container (prefered) or systemd
- **Listens on:** `http://<PI_IP>:8765`
- **Auth:** `X-Token` header with a secret stored in `/etc/tv-helper/env`
- **Endpoints:**
  - `GET /health` → `{"ok": true}` (no auth)
  - `POST /poweroff` → powers off LG (requires `X-Token` header)
- **LG pairing:** already done via test_pairing.py, client key stored in `~/lg_key.json` on the Pi
- **Tested:** `curl -X POST -H "X-Token: <token>" http://<PI_IP>:8765/poweroff` successfully turns off the LG

### Fire TV device prep — done
- Developer Options unlocked (7 taps on device name in Settings → My Fire TV → About)
- ADB debugging enabled
- ADB over network tested from laptop
- `adb shell input keyevent 223` correctly puts Fire TV in standby (but LG ignores CEC, hence this project)

## What Claude Code needs to build

A minimal Android TV app with:

### Functional requirements
1. **TV-optimized UI** (D-pad navigation, large focusable elements, no touch assumptions). The ./design folder contians the app's design. use that. The specificaitons are in ./design/README.md file
2. **On expiry** — POST to `http://<PI_IP>:8765/poweroff` with the `X-Token` header. On success, also put the Fire TV itself to sleep (system-level; safest is `finish()` + letting the TV power off kill the HDMI signal, but can also try sending a broadcast).
3. **Timer must survive** the app being backgrounded. Use a foreground `Service` + `AlarmManager` (AlarmManager is essential — if the device dozes, a `postDelayed` in an Activity will drift or die).
4. **First-run config screen**: ask for Pi IP and token. Store in `EncryptedSharedPreferences`. No hardcoded secrets in the APK.

### Non-functional requirements
- **Kotlin**, minSdk 22 (covers all Fire TV Sticks), targetSdk current
- **Single module**, single Activity is fine, keep dependencies minimal
- **Networking:** OkHttp (lightweight, well-supported). No Retrofit needed for one endpoint.
- **Security:**
  - Token in `EncryptedSharedPreferences` (androidx.security.crypto)
  - `android:usesCleartextTraffic="true"` scoped to the Pi IP only via `network_security_config.xml` (HTTP on LAN is fine, but be explicit)
  - `INTERNET` permission only; no location, no storage
- **Error handling:** if the POST fails (Pi offline, wrong token), show a clear on-screen error and do NOT silently fail — user needs to know the TV won't turn off
- **Leanback banner** in manifest so it appears in Fire TV's Your Apps & Games
- **Keep the code small.** This is a ~300-line app, not a framework

### Design file
User has a design file in the project directory. **Read it first** and follow its layout/colors/typography. 

### Suggested project structure
```
app/
  src/main/
    java/<pkg>/
      MainActivity.kt           # duration picker
      TimerActivity.kt          # countdown + cancel
      SettingsActivity.kt       # Pi IP + token (first run)
      PowerOffService.kt        # foreground service, fires HTTP call
      TimerReceiver.kt          # AlarmManager broadcast receiver
      SecurePrefs.kt            # EncryptedSharedPreferences wrapper
      TvClient.kt               # OkHttp POST /poweroff
    res/
      layout/
      values/
      xml/network_security_config.xml
    AndroidManifest.xml
build.gradle.kts
```

### Manifest essentials
- `<uses-feature android:name="android.software.leanback" android:required="false"/>`
- `<uses-feature android:name="android.hardware.touchscreen" android:required="false"/>`
- Launcher intent-filter with both `LAUNCHER` and `LEANBACK_LAUNCHER` categories
- `android:banner="@drawable/banner"` on `<application>`
- `INTERNET`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `SCHEDULE_EXACT_ALARM` (the last one: use `setExactAndAllowWhileIdle`; on Android 12+ request the permission)

### Build & deploy
Build APK, then from laptop and ask the user to test:
```bash
adb connect <FIRE_TV_IP>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
App should appear in Fire TV's Your Apps & Games row.

## Open decisions for Claude Code to ask the user
1. Exact duration presets the user wants
2. Whether the timer should also put the **Fire TV** to sleep (recommended yes — after the LG powers off, the stick is wasting ~2W), go with recommended
3. Should there be a "5-minute warning" before shutdown (on-screen overlay)? yes, this is also in the /design/readme.md file I guess

## Values to fill in (user will provide)
- `PI_IP` — the Raspberry Pi's LAN IP
- `TV_TOKEN` — the token generated during helper setup (also in `/etc/tv-helper/env` on the Pi)
- Fire TV IP for `adb install`

## Out of scope
- Multi-TV support
- Cloud / remote control from outside LAN
- Alexa integration
- Anything running on the Pi (already done)

---

Paste that into Claude Code, point it at your design file, and it should have everything it needs. Good luck — and nice job sticking with it through the CEC dead-end.


## Design
In design folder, read the README.md and the files in components/. Recreate the Lullaby sleep timer app as a native Fire TV app using Jetpack Compose for TV. Start a new Android project and scaffold the 4 screens.


Note: the python file should stay in this repo because I will release this as open source. but make sure the project has a nice stucutre and sepereates kotlin and py(server side ) code