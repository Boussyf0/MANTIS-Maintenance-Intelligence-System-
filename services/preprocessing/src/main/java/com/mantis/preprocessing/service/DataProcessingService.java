package com.mantis.preprocessing.service;

import com.mantis.preprocessing.model.ProcessedData;
import com.mantis.preprocessing.model.SensorData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DataProcessingService {

    @Value("${app.window-size}")
    private int windowSize;

    // In-memory buffer: MachineID -> List of SensorData
    private final Map<String, List<SensorData>> buffer = new ConcurrentHashMap<>();

    // Simple min-max parameters (mocked)
    private final double MIN_VAL = -1.0;
    private final double MAX_VAL = 1.0;

    public Optional<ProcessedData> process(SensorData data) {
        String machineId = data.getMachine_id();
        buffer.putIfAbsent(machineId, new ArrayList<>());
        List<SensorData> machineBuffer = buffer.get(machineId);

        // Add new data
        machineBuffer.add(data);

        // Maintain window size
        if (machineBuffer.size() > windowSize) {
            machineBuffer.remove(0);
        }

        // Check if window is full
        if (machineBuffer.size() < windowSize) {
            return Optional.empty();
        }

        return Optional.of(extractFeatures(machineBuffer, data));
    }

    private ProcessedData extractFeatures(List<SensorData> window, SensorData latest) {
        int sensorCount = latest.getSensors().size();
        List<Double> means = new ArrayList<>();
        List<Double> stds = new ArrayList<>();
        List<Double> lasts = new ArrayList<>();

        for (int i = 0; i < sensorCount; i++) {
            final int sensorIndex = i;
            List<Double> sensorValues = window.stream()
                    .map(d -> d.getSensors().get(sensorIndex))
                    .collect(Collectors.toList());

            // Normalize
            List<Double> normalized = sensorValues.stream()
                    .map(v -> (v - MIN_VAL) / (MAX_VAL - MIN_VAL))
                    .collect(Collectors.toList());

            // Calculate stats
            DoubleSummaryStatistics stats = normalized.stream()
                    .mapToDouble(Double::doubleValue)
                    .summaryStatistics();

            means.add(stats.getAverage());
            stds.add(calculateStd(normalized, stats.getAverage()));
            lasts.add(normalized.get(normalized.size() - 1));
        }

        Map<String, List<Double>> features = new HashMap<>();
        features.put("mean", means);
        features.put("std", stds);
        features.put("last", lasts);

        return ProcessedData.builder()
                .machine_id(latest.getMachine_id())
                .timestamp(latest.getTimestamp())
                .cycle(latest.getCycle())
                .features(features)
                .build();
    }

    private double calculateStd(List<Double> values, double mean) {
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }
}
