package com.mantis.orchestrator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyEvent {
    private String machine_id;
    private String timestamp;
    private int cycle;
    private double anomaly_score;
    private boolean is_anomaly;
    private String details;
}
