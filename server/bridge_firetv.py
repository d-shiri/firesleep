"""Fire TV (or any Android TV) backend.

Sends `input keyevent 223` (KEYCODE_SLEEP) over ADB-on-TCP. The Fire TV
goes to standby and propagates power-off over HDMI-CEC, so this single
backend covers any modern CEC-compliant TV (Samsung, Sony, Vizio, Hisense,
Roku TV, ...). Doesn't help LG UR78 — that's the broken-CEC case the LG
backend handles directly.

One-time setup on the Fire TV:
1. Settings -> My Fire TV -> Developer options -> ADB debugging: ON
2. Trigger /pair from the bridge. The Fire TV shows
   "Allow USB debugging from this computer?" — accept and tick
   "Always allow from this computer" so the key sticks across reboots.
"""
from __future__ import annotations

import asyncio
import logging
from pathlib import Path

from adb_shell.adb_device import AdbDeviceTcp
from adb_shell.auth.keygen import keygen
from adb_shell.auth.sign_pythonrsa import PythonRSASigner

log = logging.getLogger("firesleep")

ADB_PORT = 5555
KEYCODE_SLEEP = 223
TRANSPORT_TIMEOUT_S = 9.0
POWEROFF_AUTH_TIMEOUT_S = 5.0
PAIR_AUTH_TIMEOUT_S = 30.0


def _key_paths(data_dir: Path) -> tuple[Path, Path]:
    return data_dir / "adb_key", data_dir / "adb_key.pub"


def _ensure_signer(data_dir: Path) -> PythonRSASigner:
    priv, pub = _key_paths(data_dir)
    if not priv.exists() or not pub.exists():
        log.info("generating ADB key pair at %s", priv)
        keygen(str(priv))
        priv.chmod(0o600)
    return PythonRSASigner(pub.read_text(), priv.read_text())


def paired(data_dir: Path) -> bool:
    # Local key existence only — we can't tell from disk whether the Fire TV
    # still trusts it. Same caveat as the LG path: the actual handshake
    # either works or fails on /poweroff.
    priv, pub = _key_paths(data_dir)
    return priv.exists() and pub.exists()


def unpair(data_dir: Path) -> None:
    for p in _key_paths(data_dir):
        if p.exists():
            p.unlink()
    log.info("forgot ADB key in %s", data_dir)


def _connect_and_run(host: str, signer: PythonRSASigner, auth_timeout_s: float, cmd: str) -> str:
    device = AdbDeviceTcp(host, ADB_PORT, default_transport_timeout_s=TRANSPORT_TIMEOUT_S)
    device.connect(rsa_keys=[signer], auth_timeout_s=auth_timeout_s)
    try:
        return device.shell(cmd)
    finally:
        device.close()


async def pair(host: str, data_dir: Path) -> None:
    """Run a benign shell to force the auth handshake without affecting the TV."""
    signer = _ensure_signer(data_dir)
    log.info("ADB pair: connecting to %s:%d (accept the prompt on the Fire TV)", host, ADB_PORT)
    await asyncio.to_thread(_connect_and_run, host, signer, PAIR_AUTH_TIMEOUT_S, "echo ok")
    log.info("ADB pair: handshake accepted")


async def power_off(host: str, data_dir: Path) -> None:
    signer = _ensure_signer(data_dir)
    log.info("ADB poweroff: keyevent SLEEP -> %s:%d", host, ADB_PORT)
    await asyncio.to_thread(
        _connect_and_run, host, signer, POWEROFF_AUTH_TIMEOUT_S,
        f"input keyevent {KEYCODE_SLEEP}",
    )
    log.info("ADB poweroff: Fire TV acknowledged")
