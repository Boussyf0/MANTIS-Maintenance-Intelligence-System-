package com.mantis.orchestrator.model;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class Alert {
    private String alertId;
    private String machineId;
    private String timestamp;
    private String severity; // INFO, WARNING, CRITICAL, EMERGENCY
    private String message;
    private String source; // ANOMALY_DETECTION, RUL_PREDICTION
}
