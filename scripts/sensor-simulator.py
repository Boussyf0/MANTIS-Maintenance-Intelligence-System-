#!/usr/bin/env python3
"""
MANTIS Sensor Data Simulator
Generates realistic sensor data and publishes to Kafka for testing the full pipeline.

This script simulates:
1. Multiple machines with degradation patterns
2. Various sensor types (vibration, temperature, current, pressure)
3. C-MAPSS style data for RUL prediction testing
"""

import json
import time
import random
import math
import signal
import os
from datetime import datetime
from kafka import KafkaProducer

with open("simulator_debug.log", "a") as f:
    f.write("DEBUG: Script started\n")
print("DEBUG: Script started")

# Kafka configuration
KAFKA_BROKER = os.getenv("KAFKA_BROKER", "localhost:9093")  # Host port mapping
INPUT_TOPIC = "raw-sensor-data"

# Simulation configuration
NUM_MACHINES = 3
SENSORS_PER_MACHINE = 21  # C-MAPSS has 21 sensors
SIMULATION_INTERVAL = 1.0  # seconds between messages

# Stop flag for graceful shutdown
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
        self.max_rul = random.randint(150, 300)  # Random RUL at start
        self.degradation_rate = random.uniform(0.001, 0.005)

        # Sensor baselines (21 sensors like C-MAPSS)
        self.sensor_baselines = {f"sensor_{i:02d}": random.uniform(100, 500) for i in range(1, 22)}

        # Operational settings (3 settings like C-MAPSS)
        self.settings = {
            "op_setting_1": random.uniform(0, 100),
            "op_setting_2": random.uniform(0, 100),
            "op_setting_3": random.uniform(0, 100),
        }

    def get_degradation_factor(self) -> float:
        """Calculate degradation factor based on cycle"""
        remaining_rul = max(0, self.max_rul - self.cycle)
        degradation = 1 - (remaining_rul / self.max_rul)
        return max(0, min(1, degradation))

    def generate_sensor_value(self, sensor_name: str, baseline: float) -> float:
        """Generate sensor value with noise and degradation"""
        degradation = self.get_degradation_factor()

        # Add noise
        noise = random.gauss(0, baseline * 0.02)

        # Add degradation effect (increases over time)
        degradation_offset = baseline * degradation * 0.3

        # Add periodic variation
        periodic = math.sin(self.cycle * 0.1) * baseline * 0.02

        return baseline + noise + degradation_offset + periodic

    def generate_data(self) -> dict:
        """Generate a full sensor data point"""
        self.cycle += 1

        timestamp = datetime.now().isoformat()

        # Generate sensor values
        sensor_values = {}
        for sensor_name, baseline in self.sensor_baselines.items():
            sensor_values[sensor_name] = round(self.generate_sensor_value(sensor_name, baseline), 4)

        # Calculate actual RUL
        actual_rul = max(0, self.max_rul - self.cycle)

        # Convert sensors to list of values (sorted by sensor name)
        # Java SensorData expects List<Double>
        sensor_values_list = [sensor_values[k] for k in sorted(sensor_values.keys())]

        data = {
            "machine_id": self.machine_id,
            "timestamp": timestamp,
            "cycle": self.cycle,
            "sensors": sensor_values_list,
            "operational_settings": self.settings,
            "unit_number": int(self.machine_id.split("_")[1]),
            # Hidden RUL (for validation, not used in prediction)
            "_actual_rul": actual_rul,
        }

        # Reset machine if it "fails"
        if actual_rul <= 0:
            print(f"⚠️  Machine {self.machine_id} reached end of life at cycle {self.cycle}. Resetting...")
            self.cycle = 0
            self.max_rul = random.randint(150, 300)

        return data


def create_producer() -> KafkaProducer:
    """Create Kafka producer with retry logic"""
    max_retries = 30
    for attempt in range(max_retries):
        try:
            producer = KafkaProducer(
                bootstrap_servers=KAFKA_BROKER,
                value_serializer=lambda v: json.dumps(v).encode("utf-8"),
                acks="all",
            )
            print(f"✅ Connected to Kafka at {KAFKA_BROKER}")
            return producer
        except Exception:
            print(f"⏳ Waiting for Kafka... ({attempt + 1}/{max_retries})")
            time.sleep(2)

    raise Exception(f"Failed to connect to Kafka after {max_retries} attempts")


def main():
    print("=" * 60)
    print("🏭 MANTIS Sensor Data Simulator")
    print("=" * 60)
    print(f"📊 Simulating {NUM_MACHINES} machines with {SENSORS_PER_MACHINE} sensors each")
    print(f"📤 Publishing to topic: {INPUT_TOPIC}")
    print(f"⏱️  Interval: {SIMULATION_INTERVAL}s")
    print("=" * 60)
    print("\nPress Ctrl+C to stop\n")

    # Create producer
    producer = create_producer()

    # Create machine simulators
    machines = [MachineSimulator(i) for i in range(1, NUM_MACHINES + 1)]

    message_count = 0

    while running:
        for machine in machines:
            if not running:
                break

            data = machine.generate_data()

            # Send to Kafka
            try:
                # DEBUG: Print data structure
                print(f"DEBUG: Sending data type of sensors: {type(data['sensors'])}")
                producer.send(INPUT_TOPIC, data)
                producer.flush()
                message_count += 1

                rul = data["_actual_rul"]
                status = "🔴" if rul < 30 else "🟡" if rul < 100 else "🟢"

                print(
                    f"{status} [{message_count:05d}] {data['machine_id']} | "
                    f"Cycle: {data['cycle']:04d} | RUL: {rul:03d} | "
                    f"Sensor 1: {data['sensors'][0]:.2f}"
                )

            except Exception as e:
                print(f"❌ Error sending message: {e}")

        time.sleep(SIMULATION_INTERVAL)

    producer.close()
    print(f"\n✅ Simulator stopped. Total messages sent: {message_count}")


if __name__ == "__main__":
    main()
