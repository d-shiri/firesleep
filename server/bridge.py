import asyncio
import json
import logging
import os
import socket
import time
from pathlib import Path
from fastapi import FastAPI, HTTPException, Request
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
from aiowebostv import WebOsClient

import bridge_firetv

DATA_DIR = Path(os.environ.get("DATA_DIR", "/data"))
KEY_FILE = Path(os.environ.get("LG_KEY_FILE", str(DATA_DIR / "lg_key.json"))).expanduser()
CONFIG_FILE = DATA_DIR / "config.json"
WEB_DIR = Path(__file__).parent / "web"

ENV_LOG_LEVEL = os.environ.get("LOG_LEVEL", "INFO").upper()
ENV_TV_HOST = (os.environ.get("TV_HOST") or "").strip() or None
ENV_TV_BRAND = (os.environ.get("TV_BRAND") or "").strip().lower() or None

VALID_LOG_LEVELS = ("DEBUG", "INFO", "WARNING", "ERROR")
VALID_BRANDS = ("lg", "firetv")
DEFAULT_BRAND = "lg"


def _load_config() -> dict:
    if CONFIG_FILE.exists():
        try:
            return json.loads(CONFIG_FILE.read_text())
        except (json.JSONDecodeError, OSError) as e:
            log.warning("couldn't read config %s: %s", CONFIG_FILE, e)
    return {}


def _save_config(cfg: dict) -> None:
    CONFIG_FILE.write_text(json.dumps(cfg, indent=2))
    CONFIG_FILE.chmod(0o600)


def _tv_host() -> str | None:
    return _load_config().get("tv_host") or ENV_TV_HOST


def _tv_brand() -> str:
    brand = (_load_config().get("tv_brand") or ENV_TV_BRAND or DEFAULT_BRAND).lower()
    return brand if brand in VALID_BRANDS else DEFAULT_BRAND


def _paired() -> bool:
    if _tv_brand() == "firetv":
        return bridge_firetv.paired(DATA_DIR)
    return KEY_FILE.exists()


def _log_level() -> str:
    lvl = (_load_config().get("log_level") or ENV_LOG_LEVEL).upper()
    return lvl if lvl in VALID_LOG_LEVELS else "INFO"


# uvicorn configures the root logger before importing this module, so a plain
# logging.basicConfig() here is a no-op. Attach our own handler + level to the
# `firesleep` logger so the format we want is the format that ships.
_handler = logging.StreamHandler()
_handler.setFormatter(logging.Formatter(
    fmt="%(asctime)s %(levelname)-5s %(name)s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
))
log = logging.getLogger("firesleep")
log.addHandler(_handler)
log.propagate = False
log.setLevel(_log_level())

app = FastAPI()

log.info("startup: brand=%s tv_host=%s paired=%s web_dir=%s",
         _tv_brand(), _tv_host(), _paired(), WEB_DIR if WEB_DIR.exists() else "missing")


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
    # Health and static asset hits are noisy — keep at DEBUG.
    p = request.url.path
    quiet = p == "/health" or p == "/" or p.endswith((".css", ".js", ".ico", ".png", ".svg"))
    level = logging.DEBUG if quiet else logging.INFO
    log.log(level, "%s %s %s %d %.0fms",
            client, request.method, p, response.status_code, elapsed)
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
    host = _tv_host()
    if not host:
        raise HTTPException(status_code=503, detail="TV host not configured. Open the web UI to set it.")
    ip = await _resolve(host)
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
    return {
        "ok": True,
        "paired": _paired(),
        "configured": _tv_host() is not None,
    }


@app.get("/config")
async def get_config():
    return {
        "tv_host": _tv_host() or "",
        "tv_brand": _tv_brand(),
        "log_level": _log_level(),
        "paired": _paired(),
    }


class ConfigUpdate(BaseModel):
    tv_host: str | None = None
    tv_brand: str | None = None
    log_level: str | None = None


@app.post("/config")
async def post_config(body: ConfigUpdate):
    cfg = _load_config()
    if body.tv_host is not None:
        cfg["tv_host"] = body.tv_host.strip()
    if body.tv_brand is not None:
        brand = body.tv_brand.strip().lower()
        if brand not in VALID_BRANDS:
            raise HTTPException(status_code=400, detail=f"invalid tv_brand: {body.tv_brand}")
        cfg["tv_brand"] = brand
    if body.log_level is not None:
        lvl = body.log_level.upper()
        if lvl not in VALID_LOG_LEVELS:
            raise HTTPException(status_code=400, detail=f"invalid log_level: {body.log_level}")
        cfg["log_level"] = lvl
        log.setLevel(lvl)
    _save_config(cfg)
    log.info("config updated: %s", cfg)
    return {"ok": True, "config": cfg}


async def _resolved_host() -> str:
    host = _tv_host()
    if not host:
        raise HTTPException(status_code=503, detail="TV host not configured. Open the web UI to set it.")
    return await _resolve(host)


@app.post("/pair")
async def pair():
    """Trigger first-time pairing.

    LG: user accepts the on-screen prompt with the LG remote.
    Fire TV: user accepts "Allow USB debugging" on the Fire TV.
    """
    brand = _tv_brand()
    log.info("pair: starting brand=%s", brand)
    if brand == "firetv":
        ip = await _resolved_host()
        try:
            await bridge_firetv.pair(ip, DATA_DIR)
        except Exception as e:
            log.error("pair failed: %s: %s", type(e).__name__, e)
            raise HTTPException(status_code=502, detail=f"pair failed: {e}")
        log.info("pair: complete")
        return {"ok": True, "paired": True}
    client = await _connected_client()
    await client.disconnect()
    log.info("pair: complete")
    return {"ok": True, "paired": True}


@app.post("/unpair")
async def unpair():
    if _tv_brand() == "firetv":
        bridge_firetv.unpair(DATA_DIR)
        return {"ok": True, "paired": False}
    if KEY_FILE.exists():
        KEY_FILE.unlink()
        log.info("unpaired: removed %s", KEY_FILE)
    return {"ok": True, "paired": False}


@app.post("/poweroff")
async def poweroff():
    brand = _tv_brand()
    log.info("poweroff: starting brand=%s", brand)
    if brand == "firetv":
        ip = await _resolved_host()
        try:
            await bridge_firetv.power_off(ip, DATA_DIR)
        except Exception as e:
            log.error("poweroff failed: %s: %s", type(e).__name__, e)
            raise HTTPException(status_code=502, detail=f"poweroff failed: {e}")
        return {"ok": True}
    client = await _connected_client()
    try:
        await client.power_off()
        log.info("poweroff: TV acknowledged")
    finally:
        await client.disconnect()
    return {"ok": True}


# Static UI mounted LAST so the API routes above take precedence. html=True
# serves index.html for "/" and any directory request.
if WEB_DIR.exists():
    app.mount("/", StaticFiles(directory=str(WEB_DIR), html=True), name="web")
