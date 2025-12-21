package com.mantis.dashboard.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MachineState {
    @Id
    private String machineId;
    private String lastUpdated;
    private Double lastRul;
    private Double lastAnomalyScore;
    private Boolean isAnomaly;
    private Integer cycle;
    private String status; // NORMAL, WARNING, CRITICAL
}
