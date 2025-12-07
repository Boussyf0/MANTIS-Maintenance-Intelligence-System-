package com.mantis.orchestrator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RULPrediction {
    private String machine_id;
    private String timestamp;
    private int cycle;
    private double predicted_rul;
}
