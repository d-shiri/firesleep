import asyncio, json, os, sys
from aiowebostv import WebOsClient

TV_IP = "192.168.2.226"  # LG IP 
KEY_FILE = os.path.expanduser("~/lg_key.json")

async def main():
    key = json.load(open(KEY_FILE)).get("key") if os.path.exists(KEY_FILE) else None
    client = WebOsClient(TV_IP, client_key=key)
    await client.connect()
    if client.client_key != key:
        json.dump({"key": client.client_key}, open(KEY_FILE, "w"), indent=2)
        print("Paired. Key saved to", KEY_FILE)
    await client.power_off()
    await client.disconnect()
    print("Done.")

asyncio.run(main())
