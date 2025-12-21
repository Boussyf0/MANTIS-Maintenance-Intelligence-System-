package com.mantis.preprocessing.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.mantis.preprocessing.model.RULPrediction;
import com.mantis.preprocessing.model.SensorData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class InfluxDBService {

    @Value("${influxdb.url}")
    private String url;

    @Value("${influxdb.token}")
    private String token;

    @Value("${influxdb.org}")
    private String influxOrg;

    @Value("${influxdb.bucket}")
    private String bucket;

    private InfluxDBClient influxDBClient;

    @PostConstruct
    public void init() {
        try {
            influxDBClient = InfluxDBClientFactory.create(url, token.toCharArray(), influxOrg, bucket);
            log.info("Connected to InfluxDB at {}", url);
        } catch (Exception e) {
            log.error("Failed to connect to InfluxDB", e);
        }
    }

    @PreDestroy
    public void close() {
        if (influxDBClient != null) {
            influxDBClient.close();
        }
    }

    public void writeSensorData(SensorData data) {
        if (influxDBClient == null) {
            log.warn("InfluxDB client is not initialized. Skipping write.");
            return;
        }

        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            List<Double> sensors = data.getSensors();

            if (sensors == null || sensors.size() < 4) {
                log.warn("Insufficient sensor data for machine {}. Expected at least 4 values.", data.getMachine_id());
                return;
            }

            // Mapping based on simulator: 0=Temperature, 1=Vibration, 2=Pressure, 3=Current
            writePoint(writeApi, data, "temperature", sensors.get(0));
            writePoint(writeApi, data, "vibration", sensors.get(1));
            writePoint(writeApi, data, "pressure", sensors.get(2));
            writePoint(writeApi, data, "current", sensors.get(3));

            log.debug("Wrote sensor data to InfluxDB for machine {}", data.getMachine_id());

        } catch (Exception e) {
            log.error("Error writing to InfluxDB", e);
        }
    }

    public void writeRULPrediction(RULPrediction prediction) {
        if (influxDBClient == null) {
            log.warn("InfluxDB client is not initialized. Skipping write.");
            return;
        }

        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();

            Point point = Point.measurement("sensor_reading")
                    .addTag("machine_id", prediction.getMachine_id())
                    .addTag("sensor_type", "rul")
                    .addField("value", prediction.getPredicted_rul())
                    .time(Instant.now(), WritePrecision.MS);

            writeApi.writePoint(point);
            log.debug("Wrote RUL prediction to InfluxDB for machine {}", prediction.getMachine_id());

        } catch (Exception e) {
            log.error("Error writing RUL prediction to InfluxDB", e);
        }
    }

    private void writePoint(WriteApiBlocking writeApi, SensorData data, String sensorType, Double value) {
        Point point = Point.measurement("sensor_reading")
                .addTag("machine_id", data.getMachine_id())
                .addTag("sensor_type", sensorType)
                .addField("value", value)
                .time(Instant.now(), WritePrecision.MS);

        writeApi.writePoint(point);
    }
}
