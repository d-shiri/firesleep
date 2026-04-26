# FireSleep bridge — the thing that actually turns the TV(s) off

A tiny FastAPI service that runs on a box on your LAN (a Raspberry Pi is the
obvious choice — always on, always idle). The Fire TV app (`../android/`)
calls it when the sleep timer expires; this service then does the
vendor-specific work of telling each configured TV to power off.

Keeping vendor logic here, not in the APK, is the whole point. The app stays
tiny and TV-brand-agnostic; this directory is where you plug in support for
whatever TV you actually own. The bridge supports **multiple TVs** — each
with its own backend, host, and pairing key — and `POST /poweroff` fans out
to all of them.

The service has **no remote auth** — it trusts whoever can reach it on the
LAN. Bind it to a private interface (don't port-forward `8765`) and that's
the entire trust boundary.

## TV support

| Brand / OS          | Status             | Backend                              |
|---------------------|--------------------|--------------------------------------|
| LG webOS 23 (UR78)  | Tested, works      | `aiowebostv` over WS (`TV_BRAND=lg`) |
| Samsung Tizen       | Via Fire TV + CEC  | `adb-shell` (`TV_BRAND=firetv`)      |
| Sony Bravia         | Via Fire TV + CEC  | `adb-shell` (`TV_BRAND=firetv`)      |
| Vizio SmartCast     | Via Fire TV + CEC  | `adb-shell` (`TV_BRAND=firetv`)      |
| Hisense VIDAA       | Via Fire TV + CEC  | `adb-shell` (`TV_BRAND=firetv`)      |
| Roku TVs            | Via Fire TV + CEC  | `adb-shell` (`TV_BRAND=firetv`)      |

The Fire TV path uses ADB-on-TCP to send `KEYCODE_SLEEP` (223). The Fire
TV goes to standby, and CEC propagates power-off to the TV. One backend
covers any modern CEC-compliant TV — at the cost of ADB debugging staying
on permanently on the Fire TV. See "One-time pairing (Fire TV)" below.

If you add a new backend, please:

1. Keep the HTTP surface (`POST /poweroff`) exactly as-is so the Fire TV app
   doesn't need to change.
2. Drop it in as `bridge_<brand>.py` and gate it with an env var
   (e.g. `TV_BRAND=samsung`).
3. Add a short "how to pair" subsection to this README.

## Endpoints

| Method | Path                       | Purpose                                                                |
|--------|----------------------------|------------------------------------------------------------------------|
| GET    | `/health`                  | Liveness check                                                         |
| GET    | `/config`                  | Bridge-wide settings (log level)                                       |
| POST   | `/config`                  | Update bridge-wide settings                                            |
| GET    | `/tvs`                     | List every configured TV with its `paired` state                       |
| POST   | `/tvs`                     | Add a TV (`{name, brand, host}`)                                       |
| GET    | `/tvs/{id}`                | Single TV                                                              |
| PUT    | `/tvs/{id}`                | Update name / brand / host. Brand changes drop the paired flag.        |
| DELETE | `/tvs/{id}`                | Remove a TV and forget its pairing                                     |
| POST   | `/tvs/{id}/pair`           | Pair this TV (accept prompt on the device)                             |
| POST   | `/tvs/{id}/unpair`         | Forget this TV's pairing                                               |
| POST   | `/tvs/{id}/poweroff`       | Power off this TV                                                      |
| POST   | `/poweroff`                | Best-effort fan-out across all TVs. Returns 200 + per-TV results.      |

`POST /poweroff` always returns 200 when at least one TV is configured, so a
sleep-timer client (the Fire TV APK) doesn't need to know about individual
TVs and won't page on a single-TV failure. Inspect the `results` array or
the bridge log to see which TVs failed.

## Configuring TVs

Add TVs through the web UI (`http://<PI_IP>:8765/`) or via `POST /tvs`. Each
TV needs a `name`, a `brand` (`lg` or `firetv`), and a `host`. The host can
be a hostname or an IP — it's resolved at every connect, so DHCP changes
don't break things.

| Approach                          | Works in Docker bridge? | Notes                                           |
|-----------------------------------|-------------------------|-------------------------------------------------|
| Direct IP (`192.168.2.226`)       | Yes                     | Simple but breaks if the lease expires.         |
| DHCP-registered name (`LGwebOSTV`)| Yes                     | Works on most consumer routers automatically.   |
| mDNS (`LGwebOSTV.local`)          | **No** (in bridge mode) | Set `network_mode: host` in compose to use mDNS.|
| `/etc/hosts` entry on the Pi      | Yes                     | Always works; doesn't propagate to containers.  |

LG webOS sets the device hostname based on the TV's name in
*Settings → All Settings → General → About This TV → TV Name*. The default
is something like `LGwebOSTV`. Pick whatever shows up in your router's DHCP
client list.

> **Note:** Docker bridge networking blocks mDNS (multicast). If you want
> `.local` resolution from inside the container, switch the compose file to
> `network_mode: host`. For DHCP-registered names, bridge networking is fine.

### Bootstrap from env (first run only)

If `config.json` is absent or has no TVs, the bridge will create one TV
from `TV_HOST` / `TV_BRAND` env vars at startup. Once any TV exists, env
vars are ignored — the web UI / API is the source of truth.

### Migrating from the single-TV layout

If you're upgrading from the pre-multi-TV bridge, on first start the
service rewrites `config.json` into the new shape and renames
`lg_key.json` → `lg_key_<id>.json`. No re-pairing required.

## One-time pairing (LG webOS)

The first time the Pi talks to a webOS TV, the TV prompts on-screen for
permission. Approve it — the bridge writes `lg_key_<id>.json` (one per TV)
under `/data` so future sessions skip the prompt.

From the web UI: add the TV, click **Pair**, accept on the TV. Or by API:

```bash
curl -X POST http://<PI_IP>:8765/tvs/<id>/pair
```

## One-time pairing (Fire TV)

The Fire TV backend's `host` is the **Fire TV's** IP, not the TV's. The
Fire TV gets `KEYCODE_SLEEP` over ADB, goes to standby, and pulls the TV
down over HDMI-CEC. One-time setup per Fire TV:

1. *Settings → My Fire TV → Developer options → ADB debugging: ON*. (If
   "Developer options" isn't visible, open *About → Fire TV Stick* and
   click the device name seven times.)
2. Add the Fire TV in the web UI (or `POST /tvs`) and click **Pair**.
   The Fire TV shows "Allow USB debugging from this computer?" — accept
   and tick **"Always allow from this computer"** so the key sticks
   across reboots.
3. The bridge persists a single RSA key (`/data/adb_key{,.pub}`) shared
   across every Fire TV you pair — each Fire TV trusts the Pi
   independently.

If a Fire TV ever forgets the key (firmware updates have done this in the
past), click **Pair** again and re-accept on that Fire TV.

## Running the service

You don't need any env vars to start — open the web UI on first run and add
your TVs there. `TV_HOST` / `TV_BRAND` are honoured only as a one-shot
bootstrap when no TVs exist yet.

### Docker Compose (preferred)

```bash
cd server
docker compose up -d
# then browse to http://<PI_IP>:8765/ and add your TVs
```

The compose file builds the image, runs it as a non-root user with read-only
rootfs and dropped capabilities, persists config + pairing keys in a named
volume (`lg-key` — kept as-is for upgrade compatibility, despite now holding
all backend keys), and adds a healthcheck against `/health`.

### Docker (manual)

```bash
docker build -t firesleep-bridge .
docker run -d --name firesleep-bridge \
  --restart unless-stopped \
  -p 8765:8765 \
  -v lg-key:/data \
  firesleep-bridge
```

### systemd

`/etc/firesleep-bridge/env` is optional — leave it empty and configure via
the web UI, or pre-seed a single bootstrap TV:

```
TV_HOST=LGwebOSTV
TV_BRAND=lg
```

Install `firesleep-bridge.service` into `/etc/systemd/system/` and:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now firesleep-bridge
```

## Verifying

```bash
curl http://<PI_IP>:8765/health
# => {"ok": true}

curl http://<PI_IP>:8765/tvs
# => [{"id":"ab12cd34","name":"Living room","brand":"lg",...,"paired":true}, ...]

curl -X POST http://<PI_IP>:8765/poweroff
# Every configured TV powers off, => {"ok": true, "results": [...]}
```

## Configuration

| Env var    | Purpose                                                                            |
|------------|------------------------------------------------------------------------------------|
| `TV_HOST`  | First-run bootstrap. Creates one TV if none exist yet. Ignored thereafter.         |
| `TV_BRAND` | First-run bootstrap. `lg` (default) or `firetv`.                                   |
| `LOG_LEVEL`| `DEBUG`, `INFO` (default), `WARNING`, `ERROR`. Override at runtime via the web UI. |

Persistent state lives in `/data/config.json` (TV list + log level) plus
the per-TV pairing keys (`lg_key_<id>.json`, `adb_key{,.pub}`).

## Logs

Every HTTP request is logged to stdout with client IP, method, path, status,
and elapsed ms. App-level events (DNS resolve, TV connect, pair save,
power-off ack) are tagged under the `firesleep` logger.

```
$ docker compose logs -f firesleep-bridge
firesleep-bridge  | 2026-04-25 09:42:11 INFO  firesleep | startup: TV_HOST=LGwebOSTV, key_file=/data/lg_key.json, paired=True
firesleep-bridge  | 2026-04-25 09:42:30 INFO  firesleep | poweroff: starting
firesleep-bridge  | 2026-04-25 09:42:30 INFO  firesleep | connecting to TV at 192.168.2.226 (paired=True)
firesleep-bridge  | 2026-04-25 09:42:31 INFO  firesleep | poweroff: TV acknowledged
firesleep-bridge  | 2026-04-25 09:42:31 INFO  firesleep | 192.168.2.50 POST /poweroff 200 412ms
```

`/health` requests log at `DEBUG` so the Docker healthcheck (every 30 s)
doesn't drown out the interesting events. Set `LOG_LEVEL=DEBUG` if you want
to see them too.
