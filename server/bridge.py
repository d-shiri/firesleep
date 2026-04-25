import asyncio
import json
import logging
import os
import secrets
import socket
import time
from pathlib import Path
from fastapi import FastAPI, HTTPException, Request, Response
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

import bridge_firetv
import bridge_lg

DATA_DIR = Path(os.environ.get("DATA_DIR", "/data"))
CONFIG_FILE = DATA_DIR / "config.json"
LEGACY_LG_KEY = DATA_DIR / "lg_key.json"
WEB_DIR = Path(__file__).parent / "web"

ENV_LOG_LEVEL = os.environ.get("LOG_LEVEL", "INFO").upper()
ENV_TV_HOST = (os.environ.get("TV_HOST") or "").strip() or None
ENV_TV_BRAND = (os.environ.get("TV_BRAND") or "").strip().lower() or None

VALID_LOG_LEVELS = ("DEBUG", "INFO", "WARNING", "ERROR")
VALID_BRANDS = ("lg", "firetv")
DEFAULT_BRAND = "lg"


def _new_id() -> str:
    return secrets.token_hex(4)


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


def _load_tvs() -> list[dict]:
    return _load_config().get("tvs", [])


def _save_tvs(tvs: list[dict]) -> None:
    cfg = _load_config()
    cfg["tvs"] = tvs
    _save_config(cfg)


def _get_tv(tv_id: str) -> dict:
    for tv in _load_tvs():
        if tv["id"] == tv_id:
            return tv
    raise HTTPException(status_code=404, detail=f"TV {tv_id!r} not found")


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


def _migrate_config() -> None:
    """Bring old single-TV config into the multi-TV shape. Idempotent."""
    cfg = _load_config()
    changed = False
    if "tvs" not in cfg:
        legacy_host = cfg.get("tv_host") or ENV_TV_HOST
        legacy_brand = (cfg.get("tv_brand") or ENV_TV_BRAND or DEFAULT_BRAND).lower()
        if legacy_brand not in VALID_BRANDS:
            legacy_brand = DEFAULT_BRAND
        tvs: list[dict] = []
        if legacy_host:
            tv_id = _new_id()
            tvs.append({
                "id": tv_id,
                "name": f"{legacy_brand.upper()} TV",
                "brand": legacy_brand,
                "host": legacy_host,
                "paired": False,
            })
            if legacy_brand == "lg" and LEGACY_LG_KEY.exists():
                new_path = DATA_DIR / f"lg_key_{tv_id}.json"
                if not new_path.exists():
                    LEGACY_LG_KEY.rename(new_path)
                    log.info("migrated LG pairing key %s -> %s", LEGACY_LG_KEY, new_path)
                tvs[0]["paired"] = True
            log.info("migrated single-TV config to multi-TV (id=%s, brand=%s)", tv_id, legacy_brand)
        cfg["tvs"] = tvs
        changed = True
    for k in ("tv_host", "tv_brand"):
        if k in cfg:
            del cfg[k]
            changed = True
    if changed:
        _save_config(cfg)


_migrate_config()

app = FastAPI()

log.info("startup: tvs=%d data_dir=%s web_dir=%s",
         len(_load_tvs()), DATA_DIR, WEB_DIR if WEB_DIR.exists() else "missing")


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
    p = request.url.path
    quiet = p == "/health" or p == "/" or p.endswith((".css", ".js", ".ico", ".png", ".svg"))
    level = logging.DEBUG if quiet else logging.INFO
    log.log(level, "%s %s %s %d %.0fms",
            client, request.method, p, response.status_code, elapsed)
    return response


async def _resolve(host: str) -> str:
    try:
        ip = await asyncio.to_thread(socket.gethostbyname, host)
    except socket.gaierror as e:
        log.error("DNS resolve failed for %r: %s", host, e)
        raise HTTPException(status_code=502, detail=f"can't resolve {host!r}: {e}")
    log.debug("resolved %s -> %s", host, ip)
    return ip


def _set_paired(tv_id: str, paired: bool) -> None:
    tvs = _load_tvs()
    for tv in tvs:
        if tv["id"] == tv_id:
            tv["paired"] = paired
            break
    _save_tvs(tvs)


async def _do_pair(tv: dict) -> None:
    ip = await _resolve(tv["host"])
    try:
        if tv["brand"] == "firetv":
            await bridge_firetv.pair(ip, DATA_DIR)
        else:
            await bridge_lg.pair(ip, DATA_DIR, tv["id"])
    except Exception as e:
        log.error("pair failed for %s: %s: %s", tv["id"], type(e).__name__, e)
        raise HTTPException(status_code=502, detail=f"pair failed: {e}")
    _set_paired(tv["id"], True)


async def _do_unpair(tv: dict) -> None:
    if tv["brand"] == "firetv":
        # ADB key is shared across Fire TVs; only delete it when no Fire TV
        # remains paired, so unpairing one doesn't bork the others.
        any_other_firetv_paired = any(
            t["id"] != tv["id"] and t["brand"] == "firetv" and t.get("paired")
            for t in _load_tvs()
        )
        if not any_other_firetv_paired:
            bridge_firetv.unpair(DATA_DIR)
    else:
        bridge_lg.unpair(DATA_DIR, tv["id"])
    _set_paired(tv["id"], False)


async def _do_poweroff(tv: dict) -> None:
    ip = await _resolve(tv["host"])
    if tv["brand"] == "firetv":
        await bridge_firetv.power_off(ip, DATA_DIR)
    else:
        await bridge_lg.power_off(ip, DATA_DIR, tv["id"])


@app.get("/health")
async def health():
    return {"ok": True}


@app.get("/config")
async def get_config():
    return {"log_level": _log_level()}


class ConfigUpdate(BaseModel):
    log_level: str | None = None


@app.post("/config")
async def post_config(body: ConfigUpdate):
    cfg = _load_config()
    if body.log_level is not None:
        lvl = body.log_level.upper()
        if lvl not in VALID_LOG_LEVELS:
            raise HTTPException(status_code=400, detail=f"invalid log_level: {body.log_level}")
        cfg["log_level"] = lvl
        log.setLevel(lvl)
    _save_config(cfg)
    log.info("config updated: log_level=%s", cfg.get("log_level"))
    return {"ok": True, "config": {"log_level": _log_level()}}


@app.get("/tvs")
async def list_tvs():
    return _load_tvs()


class TVCreate(BaseModel):
    name: str
    brand: str
    host: str


class TVUpdate(BaseModel):
    name: str | None = None
    brand: str | None = None
    host: str | None = None


def _validate_brand(brand: str) -> str:
    b = brand.strip().lower()
    if b not in VALID_BRANDS:
        raise HTTPException(status_code=400, detail=f"invalid brand: {brand!r}")
    return b


def _validate_name(name: str) -> str:
    n = name.strip()
    if not n:
        raise HTTPException(status_code=400, detail="name must not be empty")
    return n


def _validate_host(host: str) -> str:
    h = host.strip()
    if not h:
        raise HTTPException(status_code=400, detail="host must not be empty")
    return h


@app.post("/tvs", status_code=201)
async def create_tv(body: TVCreate):
    tv = {
        "id": _new_id(),
        "name": _validate_name(body.name),
        "brand": _validate_brand(body.brand),
        "host": _validate_host(body.host),
        "paired": False,
    }
    tvs = _load_tvs()
    tvs.append(tv)
    _save_tvs(tvs)
    log.info("added TV id=%s name=%r brand=%s host=%s", tv["id"], tv["name"], tv["brand"], tv["host"])
    return tv


@app.get("/tvs/{tv_id}")
async def get_tv(tv_id: str):
    return _get_tv(tv_id)


@app.put("/tvs/{tv_id}")
async def update_tv(tv_id: str, body: TVUpdate):
    tvs = _load_tvs()
    for tv in tvs:
        if tv["id"] != tv_id:
            continue
        brand_changed = False
        if body.name is not None:
            tv["name"] = _validate_name(body.name)
        if body.host is not None:
            tv["host"] = _validate_host(body.host)
        if body.brand is not None:
            new_brand = _validate_brand(body.brand)
            if new_brand != tv["brand"]:
                tv["brand"] = new_brand
                brand_changed = True
        if brand_changed:
            # Old key no longer applies — drop the paired flag so the user
            # re-pairs through the new backend.
            if tv.get("paired"):
                # Clean the old backend's key for this TV before flipping.
                if tv["brand"] == "firetv":
                    bridge_lg.unpair(DATA_DIR, tv_id)
                # (firetv key is shared; leave it in place)
                tv["paired"] = False
        _save_tvs(tvs)
        log.info("updated TV id=%s -> %s", tv_id, tv)
        return tv
    raise HTTPException(status_code=404, detail=f"TV {tv_id!r} not found")


@app.delete("/tvs/{tv_id}", status_code=204)
async def delete_tv(tv_id: str):
    tvs = _load_tvs()
    for i, tv in enumerate(tvs):
        if tv["id"] == tv_id:
            await _do_unpair(tv)
            tvs.pop(i)
            _save_tvs(tvs)
            log.info("deleted TV id=%s name=%r", tv_id, tv["name"])
            return Response(status_code=204)
    raise HTTPException(status_code=404, detail=f"TV {tv_id!r} not found")


@app.post("/tvs/{tv_id}/pair")
async def pair_tv(tv_id: str):
    tv = _get_tv(tv_id)
    log.info("pair: starting tv=%s brand=%s", tv_id, tv["brand"])
    await _do_pair(tv)
    log.info("pair: complete tv=%s", tv_id)
    return {"ok": True, "paired": True}


@app.post("/tvs/{tv_id}/unpair")
async def unpair_tv(tv_id: str):
    tv = _get_tv(tv_id)
    await _do_unpair(tv)
    return {"ok": True, "paired": False}


@app.post("/tvs/{tv_id}/poweroff")
async def poweroff_tv(tv_id: str):
    tv = _get_tv(tv_id)
    log.info("poweroff: starting tv=%s brand=%s", tv_id, tv["brand"])
    try:
        await _do_poweroff(tv)
    except HTTPException:
        raise
    except Exception as e:
        log.error("poweroff failed for %s: %s: %s", tv_id, type(e).__name__, e)
        detail = str(e) or type(e).__name__
        raise HTTPException(status_code=502, detail=f"poweroff failed: {detail}")
    return {"ok": True}


@app.post("/poweroff")
async def poweroff_all():
    """Best-effort fan-out across every configured TV.

    Always returns 200 so a sleep-timer client (the Fire TV APK) doesn't
    page on a single-TV failure. Per-TV results live in the response body
    and the bridge log.
    """
    tvs = _load_tvs()
    if not tvs:
        raise HTTPException(status_code=503, detail="no TVs configured")
    log.info("poweroff: fan-out to %d TV(s)", len(tvs))
    coros = [_do_poweroff(tv) for tv in tvs]
    raw = await asyncio.gather(*coros, return_exceptions=True)
    results = []
    for tv, r in zip(tvs, raw):
        if isinstance(r, Exception):
            log.error("poweroff failed for %s (%s): %s: %s",
                      tv["id"], tv["name"], type(r).__name__, r)
            err = str(r) or type(r).__name__
            results.append({"id": tv["id"], "name": tv["name"], "ok": False, "error": err})
        else:
            results.append({"id": tv["id"], "name": tv["name"], "ok": True})
    return {"ok": all(r["ok"] for r in results), "results": results}


# Static UI mounted LAST so the API routes above take precedence. html=True
# serves index.html for "/" and any directory request.
if WEB_DIR.exists():
    app.mount("/", StaticFiles(directory=str(WEB_DIR), html=True), name="web")
