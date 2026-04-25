import asyncio
import json
import logging
import os
import socket
import time
from pathlib import Path
from fastapi import FastAPI, HTTPException, Request
from aiowebostv import WebOsClient

TV_HOST = os.environ.get("TV_HOST")
KEY_FILE = Path(os.environ.get("LG_KEY_FILE", "~/lg_key.json")).expanduser()
LOG_LEVEL = os.environ.get("LOG_LEVEL", "INFO").upper()

if not TV_HOST:
    raise SystemExit("Set TV_HOST env var (TV hostname or IP)")

logging.basicConfig(
    level=LOG_LEVEL,
    format="%(asctime)s %(levelname)-5s %(name)s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger("firesleep")

app = FastAPI()

log.info("startup: TV_HOST=%s, key_file=%s, paired=%s", TV_HOST, KEY_FILE, KEY_FILE.exists())


@app.middleware("http")
async def access_log(request: Request, call_next):
    start = time.perf_counter()
    client = request.client.host if request.client else "-"
    try:
        response = await call_next(request)
    except Exception:
        elapsed = (time.perf_counter() - start) * 1000
        log.exception("%s %s %s 500 %.0fms", client, request.method, request.url.path, elapsed)
        raise
    elapsed = (time.perf_counter() - start) * 1000
    # /health is hit by Docker every 30s — keep it at DEBUG.
    level = logging.DEBUG if request.url.path == "/health" else logging.INFO
    log.log(level, "%s %s %s %d %.0fms",
            client, request.method, request.url.path, response.status_code, elapsed)
    return response


def _load_key() -> str | None:
    if KEY_FILE.exists():
        try:
            return json.loads(KEY_FILE.read_text()).get("key")
        except (json.JSONDecodeError, OSError) as e:
            log.warning("couldn't read key file %s: %s", KEY_FILE, e)
            return None
    return None


def _save_key(key: str) -> None:
    KEY_FILE.write_text(json.dumps({"key": key}))
    KEY_FILE.chmod(0o600)
    log.info("saved new pairing key to %s", KEY_FILE)


async def _resolve(host: str) -> str:
    try:
        ip = await asyncio.to_thread(socket.gethostbyname, host)
    except socket.gaierror as e:
        log.error("DNS resolve failed for %r: %s", host, e)
        raise HTTPException(status_code=502, detail=f"can't resolve {host!r}: {e}")
    log.debug("resolved %s -> %s", host, ip)
    return ip


async def _connected_client() -> WebOsClient:
    """Resolve hostname, connect (pairing if needed), persist any new key."""
    ip = await _resolve(TV_HOST)
    existing = _load_key()
    log.info("connecting to TV at %s (paired=%s)", ip, existing is not None)
    client = WebOsClient(ip, client_key=existing)
    try:
        await client.connect()  # triggers on-TV prompt if unpaired
    except Exception as e:
        log.error("connect failed: %s: %s", type(e).__name__, e)
        raise HTTPException(status_code=502, detail=f"connect failed: {e}")
    if client.client_key and client.client_key != existing:
        _save_key(client.client_key)
    return client


@app.get("/health")
async def health():
    return {"ok": True, "paired": KEY_FILE.exists()}


@app.post("/pair")
async def pair():
    """Trigger pairing. User must accept prompt on the LG with its remote."""
    log.info("pair: starting")
    client = await _connected_client()
    await client.disconnect()
    log.info("pair: complete")
    return {"ok": True, "paired": True}


@app.post("/poweroff")
async def poweroff():
    log.info("poweroff: starting")
    client = await _connected_client()
    try:
        await client.power_off()
        log.info("poweroff: TV acknowledged")
    finally:
        await client.disconnect()
    return {"ok": True}
