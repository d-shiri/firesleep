# FireSleep bridge — the thing that actually turns the TV off

A tiny FastAPI service that runs on a box on your LAN (a Raspberry Pi is the
obvious choice — always on, always idle). The Fire TV app (`../android/`)
calls it when the sleep timer expires; this service then does the
vendor-specific work of telling the TV to power off.

Keeping vendor logic here, not in the APK, is the whole point. The app stays
tiny and TV-brand-agnostic; this directory is where you plug in support for
whatever TV you actually own.

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

| Method | Path        | Purpose                                       |
|--------|-------------|-----------------------------------------------|
| GET    | `/health`   | Liveness check, reports whether TV is paired  |
| POST   | `/pair`     | Trigger pairing (accept the prompt on the TV) |
| POST   | `/poweroff` | Power off the paired TV                       |

## Configuring `TV_HOST`

The bridge takes a hostname *or* an IP — whichever is more stable on your LAN.
The hostname is resolved at every connect, so DHCP changes don't break it.

| Approach                          | Works in Docker bridge? | Notes                                           |
|-----------------------------------|-------------------------|-------------------------------------------------|
| Direct IP (`192.168.2.226`)       | Yes                     | Simple but breaks if the TV's lease expires.    |
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

## One-time pairing (LG webOS)

The first time the Pi talks to the TV, webOS prompts for permission on the
TV screen. Approve it — the bridge writes `~/lg_key.json` so future sessions
skip the prompt.

You can either:

- **Use the bridge itself:** start the service (see below), then `curl -X
  POST http://<PI_IP>:8765/pair` and accept the prompt on the TV.
- **Run the standalone script:** `cd server && TV_HOST=LGwebOSTV uv run
  test_pairing.py`.

## One-time pairing (Fire TV)

For the `TV_BRAND=firetv` backend, `TV_HOST` is the **Fire TV's** IP, not
the TV's. Setup:

1. On the Fire TV: *Settings → My Fire TV → Developer options → ADB
   debugging: ON*. (If "Developer options" isn't visible, open *About →
   Fire TV Stick* and click the device name seven times.)
2. From the bridge: `curl -X POST http://<PI_IP>:8765/pair`. The Fire TV
   shows "Allow USB debugging from this computer?" — accept and tick
   **"Always allow from this computer"** so the key sticks across reboots.
3. The bridge persists its RSA key as `/data/adb_key{,.pub}`.

If the Fire TV ever forgets the key (firmware updates have done this in
the past), `curl -X POST http://<PI_IP>:8765/pair` again and re-accept.

## Running the service

The service needs `TV_HOST` (hostname or IP) set in the environment.
Optionally `LG_KEY_FILE` if you want to override where the pairing key is
stored.

### Docker Compose (preferred)

```bash
cd server
TV_HOST=LGwebOSTV docker compose up -d
```

The compose file builds the image, runs it as a non-root user with read-only
rootfs and dropped capabilities, persists the LG pairing key in a named
volume (`lg-key`), and adds a healthcheck against `/health`.

### Docker (manual)

```bash
docker build -t firesleep-bridge .
docker run -d --name firesleep-bridge \
  --restart unless-stopped \
  -p 8765:8765 \
  -e TV_HOST=LGwebOSTV \
  -e LG_KEY_FILE=/data/lg_key.json \
  -v firesleep-key:/data \
  firesleep-bridge
```

### systemd

Drop env vars into `/etc/firesleep-bridge/env`:

```
TV_HOST=LGwebOSTV
```

Install `firesleep-bridge.service` into `/etc/systemd/system/` and:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now firesleep-bridge
```

## Verifying

```bash
curl http://<PI_IP>:8765/health
# => {"ok": true, "paired": true}

curl -X POST http://<PI_IP>:8765/poweroff
# TV powers off, => {"ok": true}
```

## Configuration

| Env var       | Required | Purpose                                                                       |
|---------------|----------|-------------------------------------------------------------------------------|
| `TV_HOST`     | yes      | TV (LG) or Fire TV hostname/IP — resolved at every connect                    |
| `TV_BRAND`    | no       | `lg` (default) or `firetv`. Picks which backend handles `/poweroff`.          |
| `LG_KEY_FILE` | no       | Path to the cached LG pairing key file                                        |
| `LOG_LEVEL`   | no       | `DEBUG`, `INFO` (default), `WARNING`, `ERROR`                                 |

`TV_BRAND` and `TV_HOST` can also be set from the web UI; the saved value
in `/data/config.json` takes precedence over env on subsequent restarts.

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
