import asyncio
import json
import os
import socket
from pathlib import Path
from fastapi import FastAPI, HTTPException
from aiowebostv import WebOsClient

TV_HOST = os.environ.get("TV_HOST")
KEY_FILE = Path(os.environ.get("LG_KEY_FILE", "~/lg_key.json")).expanduser()

if not TV_HOST:
    raise SystemExit("Set TV_HOST env var (TV hostname or IP)")

app = FastAPI()


def _load_key() -> str | None:
    if KEY_FILE.exists():
        try:
            return json.loads(KEY_FILE.read_text()).get("key")
        except (json.JSONDecodeError, OSError):
            return None
    return None


def _save_key(key: str) -> None:
    KEY_FILE.write_text(json.dumps({"key": key}))
    KEY_FILE.chmod(0o600)


async def _resolve(host: str) -> str:
    try:
        return await asyncio.to_thread(socket.gethostbyname, host)
    except socket.gaierror as e:
        raise HTTPException(status_code=502, detail=f"can't resolve {host!r}: {e}")


async def _connected_client() -> WebOsClient:
    """Resolve hostname, connect (pairing if needed), persist any new key."""
    ip = await _resolve(TV_HOST)
    existing = _load_key()
    client = WebOsClient(ip, client_key=existing)
    await client.connect()  # triggers on-TV prompt if unpaired
    if client.client_key and client.client_key != existing:
        _save_key(client.client_key)
    return client


@app.get("/health")
async def health():
    return {"ok": True, "paired": KEY_FILE.exists()}


@app.post("/pair")
async def pair():
    """Trigger pairing. User must accept prompt on the LG with its remote."""
    client = await _connected_client()
    await client.disconnect()
    return {"ok": True, "paired": True}


@app.post("/poweroff")
async def poweroff():
    client = await _connected_client()
    try:
        await client.power_off()
    finally:
        await client.disconnect()
    return {"ok": True}
