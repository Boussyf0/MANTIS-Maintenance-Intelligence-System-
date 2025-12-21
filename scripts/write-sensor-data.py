#!/usr/bin/env python3
"""
Continuous sensor data writer for Grafana visualization
Writes data every 2 seconds for 60 iterations
"""
import urllib.request
import urllib.error
import time
import random
import math

INFLUXDB_URL = "http://localhost:8086"
INFLUXDB_TOKEN = "mantis-super-secret-token"
INFLUXDB_ORG = "mantis-org"
INFLUXDB_BUCKET = "sensors"

# Machine state
machines = {
    "machine_001": {"cycle": 0, "rul": 180, "temp_base": 70, "vib_base": 6},
    "machine_002": {"cycle": 0, "rul": 120, "temp_base": 68, "vib_base": 7},
    "machine_003": {"cycle": 0, "rul": 45, "temp_base": 75, "vib_base": 9},
}


def write_to_influxdb(lines):
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
            return response.status == 204
    except Exception as e:
        print(f"Error: {e}")
        return False


print("=" * 50)
print("🏭 MANTIS Continuous Data Writer")
print("📊 Writing to InfluxDB every 2 seconds")
print("=" * 50)

for i in range(60):  # Run for ~2 minutes
    timestamp_ns = int(time.time() * 1e9)
    lines = []

    for machine_id, state in machines.items():
        state["cycle"] += 1
        state["rul"] = max(0, state["rul"] - 1)

        # Generate sensor values with degradation
        degradation = 1 - (state["rul"] / 200)
        temp = state["temp_base"] + degradation * 10 + random.uniform(-2, 2)
        vib = state["vib_base"] + degradation * 5 + random.uniform(-0.5, 0.5)
        pressure = 100 + degradation * 15 + random.uniform(-3, 3)
        current = 12 + degradation * 3 + random.uniform(-1, 1)
        rpm = 3000 + math.sin(state["cycle"] * 0.1) * 100 + random.uniform(-20, 20)

        # Reset if RUL reaches 0
        if state["rul"] <= 0:
            print(f"⚠️  {machine_id} reset!")
            state["rul"] = random.randint(100, 200)
            state["cycle"] = 0

        lines.extend(
            [
                f"sensor_reading,machine_id={machine_id},sensor_type=temperature value={temp:.2f} {timestamp_ns}",
                f"sensor_reading,machine_id={machine_id},sensor_type=vibration value={vib:.2f} {timestamp_ns}",
                f"sensor_reading,machine_id={machine_id},sensor_type=pressure value={pressure:.2f} {timestamp_ns}",
                f"sensor_reading,machine_id={machine_id},sensor_type=current value={current:.2f} {timestamp_ns}",
                f"sensor_reading,machine_id={machine_id},sensor_type=rpm value={rpm:.2f} {timestamp_ns}",
                f'sensor_reading,machine_id={machine_id},sensor_type=rul value={state["rul"]} {timestamp_ns}',
                f'sensor_reading,machine_id={machine_id},sensor_type=cycle value={state["cycle"]} {timestamp_ns}',
            ]
        )

    if write_to_influxdb(lines):
        m1, m2, m3 = machines.values()
        print(f"[{i+1:02d}/60] ✅ RUL: M1={m1['rul']:03d} M2={m2['rul']:03d} M3={m3['rul']:03d}")
    else:
        print(f"[{i+1:02d}/60] ❌ Write failed")

    time.sleep(2)

print("\n✅ Done! Check Grafana at http://localhost:3001")
