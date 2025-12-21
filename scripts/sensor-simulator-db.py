#!/usr/bin/env python3
"""
MANTIS Sensor Data Simulator with Database Writing
Generates sensor data and writes directly to InfluxDB for Grafana visualization.

Run with: python3 scripts/sensor-simulator-db.py
"""

import time
import random
import math
import signal
from datetime import datetime, timezone

# Configuration
INFLUXDB_URL = "http://localhost:8086"
INFLUXDB_TOKEN = "mantis-super-secret-token"
INFLUXDB_ORG = "mantis-org"
INFLUXDB_BUCKET = "sensors"

NUM_MACHINES = 3
SIMULATION_INTERVAL = 2.0  # seconds between batches

running = True


def signal_handler(sig, frame):
    global running
    print("\n🛑 Stopping simulator...")
    running = False


signal.signal(signal.SIGINT, signal_handler)


class MachineSimulator:
    """Simulates a machine with degradation"""

    def __init__(self, machine_id: int):
        self.machine_id = f"machine_{machine_id:03d}"
        self.cycle = 0
        self.max_rul = random.randint(150, 300)
        self.degradation_rate = random.uniform(0.001, 0.005)

        # Sensor baselines
        self.sensors = {
            "temperature": random.uniform(65, 75),
            "vibration": random.uniform(5, 10),
            "pressure": random.uniform(90, 110),
            "current": random.uniform(10, 15),
            "rpm": random.uniform(2800, 3200),
        }

    def get_degradation_factor(self) -> float:
        remaining_rul = max(0, self.max_rul - self.cycle)
        return 1 - (remaining_rul / self.max_rul)

    def generate_sensor_value(self, sensor_name: str, baseline: float) -> float:
        degradation = self.get_degradation_factor()
        noise = random.gauss(0, baseline * 0.02)
        degradation_offset = baseline * degradation * 0.2
        periodic = math.sin(self.cycle * 0.1) * baseline * 0.02
        return baseline + noise + degradation_offset + periodic

    def generate_data(self) -> dict:
        self.cycle += 1

        values = {}
        for sensor_name, baseline in self.sensors.items():
            values[sensor_name] = round(self.generate_sensor_value(sensor_name, baseline), 2)

        actual_rul = max(0, self.max_rul - self.cycle)
        values["rul"] = actual_rul
        values["cycle"] = self.cycle

        # Reset if RUL reaches 0
        if actual_rul <= 0:
            print(f"⚠️  {self.machine_id} reached EOL at cycle {self.cycle}. Resetting...")
            self.cycle = 0
            self.max_rul = random.randint(150, 300)

        return values


def write_to_influxdb(machines_data: list):
    """Write data to InfluxDB using line protocol via HTTP"""
    import urllib.request
    import urllib.error

    lines = []
    timestamp_ns = int(datetime.now(timezone.utc).timestamp() * 1e9)

    for machine_id, data in machines_data:
        # Create line protocol entries for each sensor
        for sensor_name in [
            "temperature",
            "vibration",
            "pressure",
            "current",
            "rpm",
            "rul",
            "cycle",
        ]:
            if sensor_name in data:
                line = (
                    f"sensor_reading,machine_id={machine_id},sensor_type={sensor_name} "
                    f"value={data[sensor_name]} {timestamp_ns}"
                )
                lines.append(line)

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
    except urllib.error.URLError as e:
        print(f"❌ InfluxDB write error: {e}")
        return False
    except Exception as e:
        print(f"❌ Error: {e}")
        return False


def main():
    print("=" * 60)
    print("🏭 MANTIS Sensor Simulator (Database Mode)")
    print("=" * 60)
    print(f"📊 Simulating {NUM_MACHINES} machines")
    print(f"💾 Writing to InfluxDB: {INFLUXDB_URL}")
    print(f"🪣 Bucket: {INFLUXDB_BUCKET}")
    print(f"⏱️  Interval: {SIMULATION_INTERVAL}s")
    print("=" * 60)
    print("\nPress Ctrl+C to stop\n")

    # Create machine simulators
    machines = [MachineSimulator(i) for i in range(1, NUM_MACHINES + 1)]

    message_count = 0

    # Wait for InfluxDB
    print("⏳ Connecting to InfluxDB...")
    for attempt in range(10):
        try:
            import urllib.request

            req = urllib.request.Request(f"{INFLUXDB_URL}/health")
            with urllib.request.urlopen(req, timeout=2) as response:
                if response.status == 200:
                    print("✅ InfluxDB connected!")
                    break
        except Exception:
            print(f"   Retry {attempt + 1}/10...")
            time.sleep(2)
    else:
        print("⚠️  Could not verify InfluxDB connection, continuing anyway...")

    while running:
        machines_data = []

        for machine in machines:
            if not running:
                break

            data = machine.generate_data()
            machines_data.append((machine.machine_id, data))

            rul = data["rul"]
            status = "🔴" if rul < 30 else "🟡" if rul < 100 else "🟢"

            print(
                f"{status} {machine.machine_id} | "
                f"Cycle: {data['cycle']:04d} | RUL: {rul:03d} | "
                f"Temp: {data['temperature']:.1f}°C | "
                f"Vib: {data['vibration']:.2f}mm/s"
            )

        # Write batch to InfluxDB
        if machines_data and write_to_influxdb(machines_data):
            message_count += len(machines_data)

        time.sleep(SIMULATION_INTERVAL)

    print(f"\n✅ Simulator stopped. Total points written: {message_count}")


if __name__ == "__main__":
    main()
