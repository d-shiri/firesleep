# FireSleep — a sleep timer for your TV, driven by the Fire TV remote

**Self-hosted. One remote. Actually turns the TV off.**

Most TVs have a sleep timer buried in a settings menu you can only reach with
the TV's own remote. If you watch everything through a Fire TV Stick, that
means juggling two remotes in the dark just to tell your TV to go to bed on
schedule — and if you forget, the TV stays on all night.

FireSleep is a sideloaded Fire TV app that adds a proper sleep timer *and*
actually powers the TV off, all driven with the Fire TV remote you already
have in your hand. No second remote, no cloud service, no account: your TV,
your Pi, your LAN.

## How it works

```
Fire TV remote
    ↓ D-pad
FireSleep app on the Fire TV Stick (this repo → android/)
    ↓ HTTP POST http://<pi>:8765/poweroff
FireSleep bridge on a Raspberry Pi (this repo → server/)
    ↓ talks to the TV over the LAN
Your TV → powers off
```

The bridge trusts your LAN — there's no token to copy or paste. Bind it to a
private interface, and the only thing that can reach it is whatever's on the
same network as your Fire TV.

The Fire TV app owns the timer (an `AlarmManager` alarm behind a foreground
service, so it survives backgrounding and doze). The Pi owns the
TV-vendor-specific bits (pairing state, WebSocket chatter, etc.). Keeping
vendor logic on the Pi means:

- The APK stays tiny — no Python, no WebSocket libs, no per-vendor SDKs.
- Adding a new TV brand is a change to the Pi bridge, not the app.

## TV compatibility

The project is split so the Fire TV side is **TV-agnostic** — it makes one
HTTP POST. Vendor-specific logic lives in `server/`.

- **LG webOS (tested):** UR78 / webOS 23. Uses `aiowebostv` over the LG's
  WebSocket API. Works end-to-end; this is the path I actually run.
- **Everything else:** should be straightforward. The Pi bridge is a single
  short FastAPI file — swap the `power_off()` implementation for your TV's
  control protocol and the app keeps working unchanged.

**Fire TV devices:** should work on any Fire TV Stick (minSdk 22 covers every
generation). I've only tested on a Fire TV Stick 4K Max — reports from other
sticks are welcome.

### PRs welcome

If you get this running with a Samsung, Sony, Vizio, Hisense, Roku TV, etc.,
please open a PR. Ideal shape:

- Drop a new bridge file next to `server/bridge.py` (e.g. `bridge_samsung.py`).
- Add a short section to `server/README.md` covering pairing + running.
- If your TV needs a different port or extra headers, make it env-configurable
  rather than hardcoded.

Same goes for Fire TV hardware reports — if something breaks on an older
stick, file an issue with the device model and `adb logcat` output.

## Repo layout

```
.
├── android/         # Sideloaded Fire TV app (Kotlin, Jetpack Compose for TV)
├── server/          # Raspberry Pi bridge (FastAPI + vendor-specific code)
├── design/          # Design spec and HTML/JSX mocks for the 4 screens
└── plan.md          # Original project brief / decisions log
```

Each side has its own README:

- **[`android/README.md`](android/README.md)** — build + sideload.
- **[`server/README.md`](server/README.md)** — pairing, systemd, Docker.

## User flow

1. **Home.** Five preset durations (15 / 30 / 45 / 60 / 90 min) plus a Custom row.
2. **Custom length.** Hours × minutes reels with a live summary pane.
3. **Confirmation.** Hero numeric flashes for ~3 s, then dismisses so you can
   keep watching.
4. **Last-60s overlay.** Top-right card with `+10 minutes` or `Keep watching`.
   Ignore it and the TV goes to sleep when it hits zero.

See `design/README.md` for the pixel-level spec.

## Building the Fire TV app

```bash
cd android
./gradlew assembleDebug
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk` (~10 MB).

## Sideloading onto a Fire TV Stick

Enable ADB on the device (Settings → My Fire TV → About → tap 7 × on the
device name → Settings → My Fire TV → Developer options → ADB debugging on),
then:

```bash
adb connect <FIRE_TV_IP>:5555
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

The app appears in *Your Apps & Games* thanks to the Leanback launcher entry.
On first launch it asks for the Pi's LAN IP — that's it.

## First-run configuration

- **Pi IP:** LAN IP of the Pi running `server/bridge.py`.

Stored in `EncryptedSharedPreferences` on the device. Nothing else to enter.

## Security posture

- `INTERNET` only. No storage, no location, no camera, no mic.
- Cleartext HTTP is allowed app-wide (Android's network-security-config can't
  restrict it to a CIDR range), but the app only ever talks to one
  user-entered LAN IP on `:8765` — that's the runtime trust boundary.
- The Pi bridge has no remote auth — it trusts whoever can reach it on the
  LAN. Bind it to a private interface (don't port-forward `8765`) and that's
  the entire trust boundary.
- Nothing leaves your LAN. No cloud, no analytics, no phone-home.

## Status

Works end-to-end for the author's LG + Pi + Fire TV Stick 4K Max setup.
Rough edges still open:

- Audio fade via `AudioManager` during the last 60 s isn't wired yet.

Contributions — especially TV-compatibility PRs — welcome.
