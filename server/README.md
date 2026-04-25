# FireSleep helper — the thing that actually turns the TV off

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

| Brand / OS          | Status             | Backend              |
|---------------------|--------------------|----------------------|
| LG webOS 23 (UR78)  | Tested, works      | `aiowebostv` over WS |
| Samsung Tizen       | Not implemented    | PR welcome           |
| Sony Bravia         | Not implemented    | PR welcome           |
| Vizio SmartCast     | Not implemented    | PR welcome           |
| Hisense VIDAA       | Not implemented    | PR welcome           |
| Roku TVs            | Not implemented    | PR welcome           |

If you add a new backend, please:

1. Keep the HTTP surface (`POST /poweroff`) exactly as-is so the Fire TV app
   doesn't need to change.
2. Drop it in as `tv_helper_<brand>.py` and gate it with an env var
   (e.g. `TV_BRAND=samsung`).
3. Add a short "how to pair" subsection to this README.

## Endpoints

| Method | Path        | Purpose                                       |
|--------|-------------|-----------------------------------------------|
| GET    | `/health`   | Liveness check, reports whether TV is paired  |
| POST   | `/pair`     | Trigger pairing (accept the prompt on the TV) |
| POST   | `/poweroff` | Power off the paired TV                       |

## One-time pairing (LG webOS)

The first time the Pi talks to the TV, webOS prompts for permission on the
TV screen. Approve it — the helper writes `~/lg_key.json` so future sessions
skip the prompt.

You can either:

- **Use the helper itself:** start the service (see below), then `curl -X
  POST http://<PI_IP>:8765/pair` and accept the prompt on the TV.
- **Run the standalone script:** `cd server && uv run test_pairing.py` after
  setting `TV_IP` at the top of the file.

## Running the service

The service needs `TV_IP` set in the environment. Optionally
`LG_KEY_FILE` if you want to override where the pairing key is stored.

### Docker (preferred)

```bash
docker build -t firesleep-helper .
docker run -d --name firesleep-helper \
  --restart unless-stopped \
  -p 8765:8765 \
  -e TV_IP=192.168.2.226 \
  -v "$HOME/lg_key.json:/root/lg_key.json" \
  firesleep-helper
```

### systemd

Drop env vars into `/etc/tv-helper/env`:

```
TV_IP=192.168.2.226
```

Install `tv-helper.service` into `/etc/systemd/system/` and:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now tv-helper
```

## Verifying

```bash
curl http://<PI_IP>:8765/health
# => {"ok": true, "paired": true}

curl -X POST http://<PI_IP>:8765/poweroff
# TV powers off, => {"ok": true}
```

## Configuration

| Env var       | Required | Purpose                              |
|---------------|----------|--------------------------------------|
| `TV_IP`       | yes      | LAN IP of the TV                     |
| `LG_KEY_FILE` | no       | Path to the cached pairing key file  |
