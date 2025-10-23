#!/usr/bin/env python3
"""
MANTIS Database Testing Script
Tests all databases with sample data insertion and queries

This script demonstrates:
1. Database connectivity
2. Data insertion
3. Query operations
4. Foreign key relationships
5. Time-series operations
"""

import sys
import psycopg2
from psycopg2.extras import RealDictCursor
import redis
from datetime import datetime, timedelta
import json
import uuid
from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS


# Color codes for output
class Colors:
    GREEN = "\033[92m"
    RED = "\033[91m"
    YELLOW = "\033[93m"
    BLUE = "\033[94m"
    END = "\033[0m"
    BOLD = "\033[1m"


def print_header(text):
    """Print a formatted header"""
    print(f"\n{Colors.BLUE}{Colors.BOLD}{'='*60}{Colors.END}")
    print(f"{Colors.BLUE}{Colors.BOLD}{text:^60}{Colors.END}")
    print(f"{Colors.BLUE}{Colors.BOLD}{'='*60}{Colors.END}\n")


def print_success(text):
    """Print success message"""
    print(f"{Colors.GREEN}✓ {text}{Colors.END}")


def print_error(text):
    """Print error message"""
    print(f"{Colors.RED}✗ {text}{Colors.END}")


def print_info(text):
    """Print info message"""
    print(f"{Colors.YELLOW}ℹ {text}{Colors.END}")


# Database connection configurations
POSTGRES_CONFIG = {
    "host": "localhost",
    "port": 5432,
    "database": "mantis",
    "user": "mantis",
    "password": "mantis_password",
}

TIMESCALEDB_CONFIG = {
    "host": "localhost",
    "port": 5433,
    "database": "mantis_timeseries",
    "user": "mantis",
    "password": "mantis_password",
}

INFLUXDB_CONFIG = {
    "url": "http://localhost:8086",
    "token": "mantis-super-secret-token",
    "org": "mantis-org",
    "bucket": "sensors",
}

REDIS_CONFIG = {"host": "localhost", "port": 6380, "decode_responses": True}


def test_postgresql():
    """Test PostgreSQL with sample data"""
    print_header("Testing PostgreSQL (Metadata Database)")

    try:
        # Connect
        conn = psycopg2.connect(**POSTGRES_CONFIG)
        cursor = conn.cursor(cursor_factory=RealDictCursor)
        print_success("Connected to PostgreSQL")

        # Generate unique identifiers for this test run
        unique_suffix = str(uuid.uuid4())[:8]

        # 1. Insert sample asset
        print_info("Inserting sample asset...")
        cursor.execute(
            """
            INSERT INTO assets (asset_code, name, type, manufacturer, model,
                              location_line, criticality, status)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            RETURNING id, asset_code, name
        """,
            (
                f"PUMP-{unique_suffix}",
                f"Hydraulic Pump #{unique_suffix}",
                "pump",
                "Grundfos",
                "CR-150",
                "Line-A",
                "critical",
                "operational",
            ),
        )

        asset = cursor.fetchone()
        asset_id = asset["id"]
        print_success(f"Created asset: {asset['name']} (ID: {asset_id})")

        # 2. Insert sample sensor
        print_info("Inserting sample sensor...")
        cursor.execute(
            """
            INSERT INTO sensors (asset_id, sensor_code, sensor_type, unit,
                               sampling_rate_hz, threshold_warning,
                               threshold_critical)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            RETURNING id, sensor_code, sensor_type
        """,
            (
                asset_id,
                f"VIB-PUMP-{unique_suffix}-A",
                "vibration",
                "mm/s",
                1000.0,
                10.0,
                15.0,
            ),
        )

        sensor = cursor.fetchone()
        print_success(f"Created sensor: {sensor['sensor_code']} " f"(Type: {sensor['sensor_type']})")

        # 3. Insert spare part
        print_info("Inserting spare part...")
        cursor.execute(
            """
            INSERT INTO spare_parts (part_number, name, category, unit_price,
                                   stock_quantity, stock_min_threshold)
            VALUES (%s, %s, %s, %s, %s, %s)
            RETURNING id, part_number, name
        """,
            (
                f"SEAL-{unique_suffix}",
                f"Mechanical Seal {unique_suffix}",
                "seals",
                125.50,
                5,
                2,
            ),
        )

        part = cursor.fetchone()
        print_success(f"Created spare part: {part['name']}")

        # 4. Create work order
        print_info("Creating work order...")
        cursor.execute(
            """
            INSERT INTO work_orders (asset_id, work_order_number, title,
                                   priority, status, scheduled_start)
            VALUES (%s, %s, %s, %s, %s, %s)
            RETURNING id, work_order_number, title
        """,
            (
                asset_id,
                f"WO-{unique_suffix}",
                f"Preventive Maintenance - Pump #{unique_suffix}",
                "medium",
                "open",
                datetime.now() + timedelta(days=7),
            ),
        )

        work_order = cursor.fetchone()
        print_success(f"Created work order: {work_order['work_order_number']}")

        # 5. Query data back with JOIN
        print_info("\nQuerying assets with their sensors...")
        cursor.execute(
            """
            SELECT a.asset_code, a.name as asset_name, a.criticality,
                   s.sensor_code, s.sensor_type, s.unit
            FROM assets a
            LEFT JOIN sensors s ON a.id = s.asset_id
            WHERE a.asset_code = %s
        """,
            (f"PUMP-{unique_suffix}",),
        )

        results = cursor.fetchall()
        print_success(f"Found {len(results)} sensor(s) for asset PUMP-001:")
        for row in results:
            print(f"  • {row['sensor_code']} ({row['sensor_type']}) - Unit: {row['unit']}")

        # Commit changes
        conn.commit()
        print_success("\nAll PostgreSQL operations completed successfully!")

        # Close connection
        cursor.close()
        conn.close()

        return True

    except Exception as e:
        print_error(f"PostgreSQL test failed: {str(e)}")
        return False


def test_timescaledb():
    """Test TimescaleDB with time-series data"""
    print_header("Testing TimescaleDB (Time-Series Database)")

    try:
        # Connect
        conn = psycopg2.connect(**TIMESCALEDB_CONFIG)
        cursor = conn.cursor(cursor_factory=RealDictCursor)
        print_success("Connected to TimescaleDB")

        # 1. Insert sample time-series data
        print_info("Inserting sample sensor readings...")

        # Generate 100 data points over the last hour
        # Note: We need actual UUIDs for sensor_id and asset_id from PostgreSQL
        # For this test, we'll use dummy UUIDs
        base_time = datetime.now() - timedelta(hours=1)
        dummy_asset_id = "00000000-0000-0000-0000-000000000001"
        dummy_sensor_id = "00000000-0000-0000-0000-000000000002"

        for i in range(100):
            timestamp = base_time + timedelta(seconds=i * 36)  # Every 36 seconds

            cursor.execute(
                """
                INSERT INTO sensor_data_raw (time, sensor_id, asset_id,
                                            sensor_code, sensor_type, value, unit, quality)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            """,
                (
                    timestamp,
                    dummy_sensor_id,
                    dummy_asset_id,
                    "VIB-PUMP-001-A",
                    "vibration",
                    8.5 + (i % 10) * 0.1,
                    "mm/s",
                    100,
                ),
            )

        print_success("Inserted 100 time-series data points")

        # 2. Query recent data
        print_info("\nQuerying recent sensor readings...")
        cursor.execute(
            """
            SELECT time, sensor_code, value, unit
            FROM sensor_data_raw
            WHERE sensor_code = %s
            AND time > NOW() - INTERVAL '10 minutes'
            ORDER BY time DESC
            LIMIT 5
        """,
            ("VIB-PUMP-001-A",),
        )

        results = cursor.fetchall()
        print_success("Recent readings for sensor VIB-PUMP-001-A:")
        for row in results:
            print(f"  • {row['time']}: {row['value']} {row['unit']}")

        # 3. Perform time-bucket aggregation
        print_info("\nPerforming time-bucket aggregation (5-minute windows)...")
        cursor.execute(
            """
            SELECT time_bucket('5 minutes', time) AS bucket,
                   sensor_code,
                   AVG(value) as avg_value,
                   MIN(value) as min_value,
                   MAX(value) as max_value,
                   COUNT(*) as data_points
            FROM sensor_data_raw
            WHERE sensor_code = %s
            AND time > NOW() - INTERVAL '1 hour'
            GROUP BY bucket, sensor_code
            ORDER BY bucket DESC
        """,
            ("VIB-PUMP-001-A",),
        )

        results = cursor.fetchall()
        print_success("Aggregated data (5-min buckets):")
        for row in results:
            print(
                f"  • {row['bucket']}: avg={row['avg_value']:.2f}, "
                f"min={row['min_value']:.2f}, max={row['max_value']:.2f}, "
                f"points={row['data_points']}"
            )

        # Commit changes
        conn.commit()
        print_success("\nAll TimescaleDB operations completed successfully!")

        # Close connection
        cursor.close()
        conn.close()

        return True

    except Exception as e:
        print_error(f"TimescaleDB test failed: {str(e)}")
        return False


def test_influxdb():
    """Test InfluxDB with high-frequency data"""
    print_header("Testing InfluxDB (High-Frequency Metrics)")

    try:
        # Connect
        client = InfluxDBClient(
            url=INFLUXDB_CONFIG["url"],
            token=INFLUXDB_CONFIG["token"],
            org=INFLUXDB_CONFIG["org"],
        )
        print_success("Connected to InfluxDB")

        # 1. Write sample data
        print_info("Writing sample high-frequency data...")
        write_api = client.write_api(write_options=SYNCHRONOUS)

        # Write 50 points with millisecond precision
        base_time = datetime.utcnow()
        for i in range(50):
            point = (
                Point("sensor_reading")
                .tag("asset_id", "PUMP-001")
                .tag("sensor_id", "VIB-PUMP-001-A")
                .tag("sensor_type", "vibration")
                .field("value", 8.5 + (i % 20) * 0.05)
                .field("quality", 100.0)
                .time(
                    base_time + timedelta(milliseconds=i * 20),
                    WritePrecision.MS,
                )
            )

            write_api.write(bucket=INFLUXDB_CONFIG["bucket"], record=point)

        print_success("Wrote 50 high-frequency data points")

        # 2. Query data back
        print_info("\nQuerying recent data...")
        query_api = client.query_api()

        query = f"""
            from(bucket: "{INFLUXDB_CONFIG['bucket']}")
              |> range(start: -1h)
              |> filter(fn: (r) => r["_measurement"] == "sensor_reading")
              |> filter(fn: (r) => r["sensor_id"] == "VIB-PUMP-001-A")
              |> filter(fn: (r) => r["_field"] == "value")
              |> sort(columns: ["_time"], desc: true)
              |> limit(n: 5)
        """

        result = query_api.query(query=query)
        print_success("Recent readings from InfluxDB:")

        for table in result:
            for record in table.records:
                print(f"  • {record.get_time()}: {record.get_value():.2f}")

        # 3. Perform aggregation
        print_info("\nPerforming aggregation (mean over 1 minute)...")
        agg_query = f"""
            from(bucket: "{INFLUXDB_CONFIG['bucket']}")
              |> range(start: -1h)
              |> filter(fn: (r) => r["_measurement"] == "sensor_reading")
              |> filter(fn: (r) => r["sensor_id"] == "VIB-PUMP-001-A")
              |> filter(fn: (r) => r["_field"] == "value")
              |> aggregateWindow(every: 1m, fn: mean, createEmpty: false)
              |> yield(name: "mean")
        """

        agg_result = query_api.query(query=agg_query)
        print_success("Aggregated values (1-minute windows):")

        for table in agg_result:
            for record in table.records[:3]:  # Show first 3
                print(f"  • {record.get_time()}: {record.get_value():.2f}")

        print_success("\nAll InfluxDB operations completed successfully!")
        client.close()

        return True

    except Exception as e:
        print_error(f"InfluxDB test failed: {str(e)}")
        return False


def test_redis():
    """Test Redis feature store"""
    print_header("Testing Redis (Feature Store & Cache)")

    try:
        # Connect
        r = redis.Redis(**REDIS_CONFIG)
        r.ping()
        print_success("Connected to Redis")

        # 1. Store latest features
        print_info("Storing latest ML features...")
        features = {
            "vibration_rms": 8.65,
            "vibration_peak": 12.3,
            "temperature_avg": 68.2,
            "current_avg": 15.8,
            "timestamp": datetime.now().isoformat(),
        }

        r.set("feature:PUMP-001:latest", json.dumps(features))
        r.expire("feature:PUMP-001:latest", 300)  # TTL: 5 minutes
        print_success("Stored features with 5-minute TTL")

        # 2. Retrieve features
        print_info("\nRetrieving features...")
        stored_features = json.loads(r.get("feature:PUMP-001:latest"))
        print_success("Retrieved features:")
        for key, value in stored_features.items():
            print(f"  • {key}: {value}")

        # 3. Store rolling window (using List)
        print_info("\nStoring rolling window (last 10 readings)...")
        for i in range(10):
            reading = {
                "value": 8.5 + i * 0.1,
                "timestamp": (datetime.now() - timedelta(seconds=i * 10)).isoformat(),
            }
            r.lpush("window:PUMP-001:vibration", json.dumps(reading))

        # Keep only last 10
        r.ltrim("window:PUMP-001:vibration", 0, 9)
        print_success("Stored 10 readings in rolling window")

        # 4. Retrieve window
        print_info("\nRetrieving rolling window...")
        window = r.lrange("window:PUMP-001:vibration", 0, -1)
        print_success(f"Rolling window (last {len(window)} readings):")
        for item in window[:3]:  # Show first 3
            reading = json.loads(item)
            print(f"  • {reading['timestamp']}: {reading['value']}")

        # 5. Use Redis as cache
        print_info("\nCaching aggregated query result...")
        cache_key = "cache:PUMP-001:daily_avg"
        cache_value = {
            "daily_avg": 8.75,
            "computed_at": datetime.now().isoformat(),
        }
        r.setex(cache_key, 3600, json.dumps(cache_value))  # Cache for 1 hour
        print_success("Cached result with 1-hour expiration")

        print_success("\nAll Redis operations completed successfully!")

        return True

    except Exception as e:
        print_error(f"Redis test failed: {str(e)}")
        return False


def main():
    """Run all database tests"""
    print(f"\n{Colors.BOLD}{'='*60}")
    print("  MANTIS Database Testing Suite")
    print("  Testing all databases with sample data")
    print(f"{Colors.BOLD}{'='*60}{Colors.END}\n")

    results = {
        "PostgreSQL": False,
        "TimescaleDB": False,
        "InfluxDB": False,
        "Redis": False,
    }

    # Run tests
    results["PostgreSQL"] = test_postgresql()
    results["TimescaleDB"] = test_timescaledb()
    results["InfluxDB"] = test_influxdb()
    results["Redis"] = test_redis()

    # Summary
    print_header("Test Summary")

    all_passed = all(results.values())

    for db, passed in results.items():
        if passed:
            print_success(f"{db}: PASSED")
        else:
            print_error(f"{db}: FAILED")

    print(f"\n{Colors.BOLD}{'='*60}{Colors.END}")
    if all_passed:
        print(f"{Colors.GREEN}{Colors.BOLD}✓ All database tests passed!{Colors.END}")
        print(f"{Colors.GREEN}Your MANTIS data layer is fully operational!{Colors.END}")
        return 0
    else:
        failed_count = sum(1 for v in results.values() if not v)
        print(f"{Colors.RED}{Colors.BOLD}✗ {failed_count} test(s) failed{Colors.END}")
        return 1


if __name__ == "__main__":
    sys.exit(main())
