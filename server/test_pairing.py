import asyncio
import json
import os
import socket
from aiowebostv import WebOsClient

TV_HOST = os.environ.get("TV_HOST", "LGwebOSTV")  # hostname or IP
KEY_FILE = os.path.expanduser("~/lg_key.json")


async def main():
    ip = socket.gethostbyname(TV_HOST)
    print(f"{TV_HOST} -> {ip}")
    key = json.load(open(KEY_FILE)).get("key") if os.path.exists(KEY_FILE) else None
    client = WebOsClient(ip, client_key=key)
    await client.connect()
    if client.client_key != key:
        json.dump({"key": client.client_key}, open(KEY_FILE, "w"), indent=2)
        print("Paired. Key saved to", KEY_FILE)
    await client.power_off()
    await client.disconnect()
    print("Done.")


asyncio.run(main())
