"""LG webOS backend.

Per-TV pairing key on disk: `<data_dir>/lg_key_<tv_id>.json`.
"""
from __future__ import annotations

import json
import logging
from pathlib import Path

from aiowebostv import WebOsClient

log = logging.getLogger("firesleep")


def _key_path(data_dir: Path, tv_id: str) -> Path:
    return data_dir / f"lg_key_{tv_id}.json"


def _load_key(path: Path) -> str | None:
    if not path.exists():
        return None
    try:
        return json.loads(path.read_text()).get("key")
    except (json.JSONDecodeError, OSError) as e:
        log.warning("couldn't read LG key %s: %s", path, e)
        return None


def _save_key(path: Path, key: str) -> None:
    path.write_text(json.dumps({"key": key}))
    path.chmod(0o600)


def paired(data_dir: Path, tv_id: str) -> bool:
    return _key_path(data_dir, tv_id).exists()


def unpair(data_dir: Path, tv_id: str) -> None:
    p = _key_path(data_dir, tv_id)
    if p.exists():
        p.unlink()
        log.info("forgot LG key %s", p)


async def _connected(ip: str, key_path: Path) -> WebOsClient:
    existing = _load_key(key_path)
    log.info("LG: connecting to %s (paired=%s)", ip, existing is not None)
    client = WebOsClient(ip, client_key=existing)
    await client.connect()  # may trigger on-TV prompt if unpaired
    if client.client_key and client.client_key != existing:
        _save_key(key_path, client.client_key)
        log.info("LG: saved pairing key to %s", key_path)
    return client


async def pair(ip: str, data_dir: Path, tv_id: str) -> None:
    client = await _connected(ip, _key_path(data_dir, tv_id))
    await client.disconnect()


async def power_off(ip: str, data_dir: Path, tv_id: str) -> None:
    client = await _connected(ip, _key_path(data_dir, tv_id))
    try:
        await client.power_off()
        log.info("LG: power_off acknowledged")
    finally:
        await client.disconnect()
