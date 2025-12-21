#!/usr/bin/env python3
"""Quick test to write data to InfluxDB"""
import urllib.request
import time

INFLUXDB_URL = "http://localhost:8086"
INFLUXDB_TOKEN = "mantis-super-secret-token"
INFLUXDB_ORG = "mantis-org"
INFLUXDB_BUCKET = "sensors"

timestamp_ns = int(time.time() * 1e9)
lines = [
    f"sensor_reading,machine_id=machine_001,sensor_type=temperature value=72.5 {timestamp_ns}",
    f"sensor_reading,machine_id=machine_001,sensor_type=vibration value=7.2 {timestamp_ns}",
    f"sensor_reading,machine_id=machine_001,sensor_type=rul value=150 {timestamp_ns}",
    f"sensor_reading,machine_id=machine_002,sensor_type=rul value=85 {timestamp_ns}",
    f"sensor_reading,machine_id=machine_003,sensor_type=rul value=25 {timestamp_ns}",
]

line_protocol = "\n".join(lines)
url = f"{INFLUXDB_URL}/api/v2/write?org={INFLUXDB_ORG}&bucket={INFLUXDB_BUCKET}&precision=ns"

try:
    req = urllib.request.Request(
        url,
        data=line_protocol.encode("utf-8"),
        headers={
            "Authorization": f"Token {INFLUXDB_TOKEN}",
            "Content-Type": "text/plain; charset=utf-8",
        },
        method="POST",
    )

    with urllib.request.urlopen(req, timeout=5) as response:
        print(f"Write successful! Status: {response.status}")
except urllib.error.HTTPError as e:
    print(f"HTTP Error: {e.code} - {e.reason}")
    print(e.read().decode())
except Exception as e:
    print(f"Error: {e}")
