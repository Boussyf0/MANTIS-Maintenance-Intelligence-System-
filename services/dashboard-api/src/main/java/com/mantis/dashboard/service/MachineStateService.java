package com.mantis.dashboard.service;

import com.mantis.dashboard.model.AlertEntity;
import com.mantis.dashboard.model.MachineState;
import com.mantis.dashboard.repository.AlertRepository;
import com.mantis.dashboard.repository.MachineStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MachineStateService {

    private final MachineStateRepository machineStateRepository;
    private final AlertRepository alertRepository;

    public List<MachineState> getAllMachines() {
        return machineStateRepository.findAll();
    }

    public Optional<MachineState> getMachine(String id) {
        return machineStateRepository.findById(id);
    }

    public List<AlertEntity> getRecentAlerts() {
        return alertRepository.findTop10ByOrderByTimestampDesc();
    }

    public synchronized void updateAnomalyStatus(String machineId, String timestamp, double score, boolean isAnomaly) {
        MachineState state = machineStateRepository.findById(machineId)
                .orElse(new MachineState(machineId, timestamp, null, score, isAnomaly, 0, "NORMAL", null));

        state.setLastUpdated(timestamp);
        state.setLastAnomalyScore(score);
        state.setIsAnomaly(isAnomaly);
        if (isAnomaly) {
            state.setStatus("WARNING");
        }
        machineStateRepository.save(state);
    }

    public synchronized void updateRUL(String machineId, String timestamp, double rul, int cycle) {
        MachineState state = machineStateRepository.findById(machineId)
                .orElse(new MachineState(machineId, timestamp, rul, null, false, cycle, "NORMAL", null));

        state.setLastUpdated(timestamp);
        state.setLastRul(rul);
        state.setCycle(cycle);

        // Simple status update based on RUL if not already warning
        if (!"WARNING".equals(state.getStatus())) {
            if (rul < 20)
                state.setStatus("CRITICAL");
            else if (rul < 50)
                state.setStatus("WARNING");
            else
                state.setStatus("NORMAL");
        }

        machineStateRepository.save(state);
    }

    public synchronized void updateSensors(String machineId, String timestamp, java.util.Map<String, Double> sensors) {
        MachineState state = machineStateRepository.findById(machineId)
                .orElse(new MachineState(machineId, timestamp, null, null, false, 0, "NORMAL", sensors));

        state.setLastUpdated(timestamp);
        state.setSensors(sensors);
        machineStateRepository.save(state);
    }

    public void saveAlert(AlertEntity alert) {
        alertRepository.save(alert);
        // Also update machine status based on alert severity
        machineStateRepository.findById(alert.getMachineId()).ifPresent(state -> {
            if ("EMERGENCY".equals(alert.getSeverity())) {
                state.setStatus("CRITICAL");
            } else if ("CRITICAL".equals(alert.getSeverity())) {
                state.setStatus("WARNING");
            }
            machineStateRepository.save(state);
        });
    }
}
